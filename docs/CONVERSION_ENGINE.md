# RVSwaps — Conversion Engine Design

---

## 1. Overview

The Conversion Engine is the hidden bridge between the USD-denominated marketplace and the RV-denominated blockchain layer. It handles:

- **USD → RV** at checkout (buyer pays dollars, escrow receives RV)
- **RV → USD** at settlement (seller receives dollars if auto-cashout is enabled)
- **Rate determination** via TWAP oracle
- **Slippage protection** and circuit breakers
- **Treasury reserve management** for instant liquidity
- **Reconciliation** between off-chain and on-chain state

The user never interacts with the Conversion Engine directly. It operates silently behind the Payment Service.

---

## 2. Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    CONVERSION ENGINE                          │
│                                                              │
│  ┌──────────────┐   ┌───────────────┐   ┌───────────────┐  │
│  │ Rate Oracle  │   │ Conversion    │   │ Treasury      │  │
│  │              │   │ Router        │   │ Manager       │  │
│  │ - TWAP calc  │──▶│               │◀──│               │  │
│  │ - Chainlink  │   │ - Fill from   │   │ - Reserve     │  │
│  │ - DEX feeds  │   │   treasury    │   │   tracking    │  │
│  │ - Circuit    │   │ - Route to    │   │ - Rebalance   │  │
│  │   breakers   │   │   DEX         │   │   triggers    │  │
│  │              │   │ - Split large │   │ - Hot/cold    │  │
│  │              │   │   orders      │   │   targets     │  │
│  └──────────────┘   └───────────────┘   └───────────────┘  │
│                             │                                │
│                    ┌────────▼────────┐                       │
│                    │ Slippage       │                       │
│                    │ Controller     │                       │
│                    │                │                       │
│                    │ - Max slippage │                       │
│                    │ - Order split  │                       │
│                    │ - Retry logic  │                       │
│                    └────────────────┘                       │
└──────────────────────────────────────────────────────────────┘
         │                    │                     │
    ┌────▼────┐        ┌──────▼──────┐       ┌─────▼──────┐
    │Chainlink│        │ QuickSwap   │       │ Platform   │
    │ Oracle  │        │ RV/USDC     │       │ Treasury   │
    │ (on-    │        │ Pool        │       │ Wallet     │
    │ chain)  │        │ (on-chain)  │       │ (on-chain) │
    └─────────┘        └─────────────┘       └────────────┘
```

---

## 3. Rate Determination (TWAP Oracle)

### Why TWAP, Not Spot Price

Spot prices are manipulable. A single large trade on QuickSwap could move the RV/USDC price 10% for a few seconds — long enough to exploit checkout if we used spot pricing. TWAP (Time-Weighted Average Price) smooths this out.

### TWAP Calculation

```
TWAP Period: 30 minutes
Update Frequency: every block (~2 seconds on Polygon)
Data Sources (priority order):
  1. Chainlink RV/USD price feed (if available)
  2. QuickSwap RV/USDC pool (on-chain TWAP)
  3. Fallback: last known good TWAP (stale protection)

TWAP Formula:
  TWAP = Σ(price_i × duration_i) / Σ(duration_i)

  Where:
  - price_i = spot price at observation i
  - duration_i = time between observation i and i+1
  - Observations span the last 30 minutes
```

### Oracle Architecture

```
Primary: Chainlink Price Feed
  - If Chainlink has an RV/USD pair on Polygon, use it.
  - Chainlink aggregates from multiple sources, resistant to single-DEX manipulation.
  - Heartbeat: update every 1% price change or every 1 hour minimum.

Secondary: On-Chain TWAP from DEX
  - Read cumulative price from QuickSwap V3 pool (built-in oracle).
  - Calculate 30-min TWAP from cumulative price difference.
  - This is the fallback if Chainlink is unavailable or stale.

Tertiary: Cached Rate
  - If both primary and secondary are unavailable (RPC issues, oracle downtime),
    use the last known good TWAP cached in Redis.
  - Stale window: max 15 minutes. After 15 minutes of stale data,
    all conversions are halted (circuit breaker triggers).
```

### Rate Caching

```
Redis key: conversion:twap:rv_usd
Value: { rate: 0.1010, timestamp: 1709510400, source: "chainlink" }
TTL: 60 seconds (refreshed every 30 seconds by background job)

Rate is cached for UI display (GET /api/conversion/rate) and for
checkout quoting (GET /api/conversion/quote).

Actual conversion uses freshly fetched rate at execution time,
with max deviation of 0.5% from the quoted rate.
```

---

## 4. Conversion Flows

### 4.1 Checkout: USD → RV (Buyer Pays Card)

```
Step-by-step:

1. Buyer clicks "Buy Now" on a $45 listing
   → Order Service creates order, calls Conversion Engine for quote

2. Conversion Engine returns quote:
   {
     price_usd: 45.00,
     buyer_fee_usd: 1.13,          // 2.5%
     total_usd: 46.13,
     rv_rate: 0.1010,              // 1 RV = $0.1010
     amount_rv: 456.73,            // 46.13 / 0.1010
     burn_rv: 4.46,                // 1% of (45.00 / 0.1010)
     fee_rv: 11.19,                // buyer fee in RV
     seller_rv: 441.08,            // net to seller
     quote_expires_at: "2026-03-04T12:05:00Z",  // 5-min validity
     quote_id: "q_8f3x2a"
   }

3. Buyer sees: "Total: $46.13" and pays with credit card

4. Payment Service charges Stripe for $46.13
   → Stripe confirms charge

5. Conversion Engine executes USD → RV:

   Path A — Treasury Fill (preferred, instant):
     If treasury reserve has ≥ 456.73 RV:
       - Debit 456.73 RV from platform treasury wallet
       - Credit $46.13 USDC to treasury rebalance pool
       - Instant — no DEX interaction needed
       → Latency: <1 second

   Path B — DEX Purchase (fallback):
     If treasury reserve is insufficient:
       - Swap 46.13 USDC → RV on QuickSwap via router
       - Use actual received RV amount
       - Slippage: max 1.0% from quoted rate
       → Latency: ~5 seconds (1-2 blocks)

   Path C — Split Fill:
     If treasury has partial amount:
       - Fill what's available from treasury
       - Buy remainder from DEX
       → Minimizes price impact

6. Wallet Service deposits 456.73 RV to RVEscrow contract
   → Meta-transaction (platform pays gas)

7. Order status → PAID
```

### 4.2 Settlement: RV → USD (Seller Auto-Cashout)

```
Triggered when: escrow is released AND seller payout_preference = 'USD'

1. RVEscrow.release() executes on-chain:
   - 4.46 RV burned
   - 11.19 RV to fee recipient
   - 441.08 RV to seller's custodial wallet

2. Conversion Engine handles seller cashout:

   Path A — Treasury Absorption (preferred):
     If treasury USDC reserve has ≥ $44.55:
       - Credit seller $44.55 USDC
       - Absorb 441.08 RV into treasury RV reserve
       - Instant settlement
       → Good for treasury: acquires RV at market rate

   Path B — DEX Sale:
     If treasury USDC is insufficient:
       - Sell 441.08 RV → USDC on QuickSwap
       - Slippage: max 1.5% (wider than buy-side, for seller protection)
       - Actual USDC received = seller payout

   Path C — Batched Sale:
     For efficiency, batch multiple seller cashouts:
       - Accumulate RV cashout requests over 10-minute windows
       - Execute single DEX sale for the batch
       - Reduces gas costs and price impact
       - Max batch delay: 10 minutes

3. Payment Service initiates fiat payout:
   - Stripe Connect transfer to seller's bank account
   - or ACH via connected bank
   - Settlement time: 2-3 business days
```

### 4.3 Direct RV Payment (Crypto-Savvy Buyer)

```
If buyer has sufficient RV balance and chooses "Pay with RV Balance":

1. Skip Stripe entirely
2. Conversion Engine still computes USD→RV at TWAP for fee calculation
3. Wallet Service signs RVEscrow.deposit() directly from buyer's custodial wallet
4. No on/off-ramp needed — pure on-chain flow
5. Buyer saves ~2.9% card processing fee (not charged)
   → Incentive for crypto adoption

→ This is the most efficient path. Eventually, as users
   accumulate RV (from sales, staking, referrals), more
   transactions should flow through this path.
```

---

## 5. Slippage Controls

### Maximum Slippage Parameters

| Operation | Max Slippage | Action if Exceeded |
|---|---|---|
| Checkout (USD→RV) | 1.0% | Abort purchase, notify buyer "price changed, retry" |
| Auto-cashout (RV→USD) | 1.5% | Retry after 5 min, queue for batch if still high |
| Buyback (treasury→DEX) | 2.0% | Split into smaller tranches, spread over hours |
| User direct buy RV | 1.5% | Show warning, require confirmation |

### Slippage Calculation

```
quoted_rate = TWAP at time of quote
execution_rate = actual rate achieved

slippage = |execution_rate - quoted_rate| / quoted_rate × 100%

If slippage > max_allowed:
  - Abort the conversion
  - Refund buyer's card charge (if applicable)
  - Log the event for analysis
  - Alert if slippage > 3% (possible manipulation attempt)
```

### Large Order Splitting

Orders that would impact more than 0.5% of DEX pool depth are automatically split:

```
Pool depth: 1,000,000 USDC in RV/USDC pool
Order size: $10,000 (1% of pool)
Split: 5 tranches of $2,000, executed over 5 blocks (~10 seconds)

Each tranche checks slippage independently.
If any tranche exceeds slippage, remaining tranches pause and retry.
```

---

## 6. Circuit Breakers

### Price Volatility Circuit Breaker

```
Trigger: RV price moves >20% in 1 hour (measured by TWAP)

Actions:
  Level 1 (15% move in 1 hour):
    - Widen slippage tolerance to 2%
    - Alert operations team
    - Log all conversions with enhanced detail

  Level 2 (20% move in 1 hour):
    - HALT all new conversions
    - Existing escrows continue (already locked rate)
    - Marketplace shows "Checkout temporarily unavailable"
    - Auto-resume after 30 minutes if price stabilizes (<5% move in 30 min)

  Level 3 (30% move in 1 hour OR oracle failure):
    - HALT all conversions
    - Pause new listings
    - Require manual review to resume
    - Notify all admins
    - Public status page update

Recovery:
  - Auto-recovery if price stabilizes for 30 minutes
  - Manual override by 2-of-5 admin multi-sig for forced resume
  - Post-incident review required within 24 hours
```

### Oracle Failure Circuit Breaker

```
Trigger: Oracle data is stale (>15 minutes since last update)

Actions:
  - Fall back to secondary oracle source
  - If all sources stale: halt conversions
  - Cache last known good rate for display (marked "estimated")
  - Resume automatically when oracle recovers

Monitoring:
  - Prometheus metric: conversion_oracle_staleness_seconds
  - Alert at 5 minutes stale (warning)
  - Alert at 10 minutes stale (critical)
  - Halt at 15 minutes stale (automatic)
```

### Liquidity Circuit Breaker

```
Trigger: DEX pool liquidity drops below $100,000

Actions:
  - Switch to treasury-only fills (no DEX routing)
  - If treasury is also low: halt conversions
  - Alert treasury manager to rebalance

Monitoring:
  - Check pool depth every 5 minutes
  - Alert at $200,000 (warning)
  - Halt DEX routing at $100,000 (critical)
```

---

## 7. Treasury Reserve Management

### Reserve Structure

```
┌───────────────────────────────────────────────┐
│               PLATFORM TREASURY                │
│                                               │
│   ┌──────────────┐    ┌──────────────┐       │
│   │ RV Reserve   │    │ USDC Reserve │       │
│   │              │    │              │       │
│   │ Target: 5%   │    │ Target: 3    │       │
│   │ of circ.     │    │ months of    │       │
│   │ supply       │    │ cashout vol  │       │
│   │              │    │              │       │
│   │ Used for:    │    │ Used for:    │       │
│   │ instant      │    │ instant      │       │
│   │ checkout     │    │ seller       │       │
│   │ fills        │    │ cashouts     │       │
│   └──────────────┘    └──────────────┘       │
│                                               │
│   ┌──────────────┐    ┌──────────────┐       │
│   │ Buyback Pool │    │ Operating    │       │
│   │              │    │ Fund         │       │
│   │ 10% of all   │    │              │       │
│   │ fees routed  │    │ 30% of fees  │       │
│   │ here         │    │ for hosting, │       │
│   │              │    │ team, legal  │       │
│   │ Weekly buy-  │    │              │       │
│   │ back + burn  │    │              │       │
│   └──────────────┘    └──────────────┘       │
└───────────────────────────────────────────────┘
```

### Reserve Targets

| Reserve | Target | Rebalance Trigger |
|---|---|---|
| RV Reserve | 5% of circulating supply | Below 3% or above 8% |
| USDC Reserve | 3 months avg cashout volume | Below 2 months |
| Buyback Pool | Accumulated, spent weekly | Execute when pool > $1,000 |
| Operating Fund | 6 months runway | Below 3 months triggers fundraise alert |

### Rebalancing Logic

```
Every hour, the Treasury Manager runs:

1. Check RV Reserve level
   if rv_reserve < 3% of circulating:
     → Buy RV from DEX using USDC reserve
     → Use TWAP-spread execution (small buys over 2 hours)
   if rv_reserve > 8% of circulating:
     → Sell excess RV on DEX
     → Move USDC to USDC reserve

2. Check USDC Reserve level
   if usdc_reserve < 2_months_avg_cashout:
     → Sell RV from RV reserve to DEX
     → Alert treasury manager

3. Check Buyback Pool
   if buyback_pool > $1,000:
     → Schedule weekly buyback execution
     (see Buyback Engine in TOKENOMICS.md)
```

---

## 8. Fee Routing

Every completed transaction generates fees that are routed automatically:

```
Buyer pays: $46.13 (item $45 + fee $1.13)

On-chain at escrow release:
  Burned:           4.46 RV   (1% of item price)       → gone forever
  Fee collected:   11.19 RV   (2.5% buyer fee)          → fee recipient wallet

Fee recipient wallet distributes (off-chain batch, daily):
  50% → Staking reward pool    =  5.60 RV  ($0.57)
  30% → Operating fund         =  3.36 RV  ($0.34)     → auto-cashout to USDC
  10% → Treasury reserve       =  1.12 RV  ($0.11)
  10% → Buyback pool           =  1.12 RV  ($0.11)     → held until weekly buyback

The fee distribution is handled by a daily cron job that:
1. Reads fee recipient wallet balance
2. Transfers appropriate amounts to each pool wallet
3. Logs all distributions to ledger_entries table
4. Auto-cashouts the operating fund portion to USDC
```

---

## 9. Conversion Engine API (Internal)

These endpoints are internal-only (not exposed to public API gateway).

```
POST /internal/conversion/quote
  Request: { amount_usd: 46.13, direction: "USD_TO_RV" }
  Response: {
    quote_id: "q_8f3x2a",
    rv_rate: 0.1010,
    amount_rv: 456.73,
    source: "chainlink",
    expires_at: "2026-03-04T12:05:00Z"
  }

POST /internal/conversion/execute
  Request: { quote_id: "q_8f3x2a", order_id: "ord_xxx" }
  Response: {
    execution_id: "ex_9g4y3b",
    amount_rv: 456.73,
    actual_rate: 0.1008,
    slippage_bps: 20,  // 0.20%
    fill_source: "treasury",
    tx_hash: null  // null for treasury fills, hash for DEX
  }

POST /internal/conversion/cashout
  Request: { user_id: "u_xxx", amount_rv: 441.08, order_id: "ord_xxx" }
  Response: {
    cashout_id: "co_7h5z4c",
    amount_usd: 44.55,
    actual_rate: 0.1010,
    payout_method: "stripe_connect",
    estimated_arrival: "2026-03-07"
  }

GET /internal/conversion/health
  Response: {
    oracle_status: "healthy",
    oracle_staleness_seconds: 12,
    twap_rate: 0.1010,
    circuit_breaker_status: "normal",
    treasury_rv_reserve: 42500000,
    treasury_usdc_reserve: 1250000,
    dex_pool_depth_usdc: 850000
  }
```

---

## 10. Failure Modes & Recovery

| Failure | Impact | Recovery |
|---|---|---|
| Stripe charge succeeds but conversion fails | Buyer charged, no escrow | Auto-refund Stripe charge within 5 min. Retry conversion once. |
| Conversion succeeds but escrow deposit fails | RV purchased but not escrowed | RV held in transit wallet. Retry escrow deposit 3x. If all fail, reverse conversion. |
| Oracle returns stale data | Wrong conversion rate | Fall back to secondary oracle. If both stale, halt conversions. |
| DEX pool drained (low liquidity) | Can't fill order | Switch to treasury-only. If treasury low, queue order. |
| Treasury depleted | Can't instant-fill | Route all orders to DEX. Alert treasury manager. |
| Seller cashout fails | Seller not paid | Retry 3x over 30 min. If persistent, manual review queue. RV stays in seller wallet. |
| Circuit breaker triggered | No new conversions | Existing escrows unaffected. New checkouts show "temporarily unavailable". Auto-resume when conditions clear. |

### Idempotency

Every conversion operation has a unique idempotency key (quote_id or order_id). Re-submitting the same request returns the same result without executing twice. This prevents double-charges, double-conversions, and double-escrow deposits.

```
Redis key: conversion:idempotency:{quote_id}
Value: { status: "completed", result: {...} }
TTL: 24 hours
```

---

## 11. Monitoring & Alerting

| Metric | Warning | Critical |
|---|---|---|
| Oracle staleness | > 5 min | > 10 min |
| TWAP deviation from spot | > 5% | > 10% |
| Conversion failure rate | > 2% | > 5% |
| Avg conversion latency | > 5s | > 15s |
| Treasury RV reserve | < 4% circ. | < 3% circ. |
| Treasury USDC reserve | < 2.5 months | < 2 months |
| DEX pool depth | < $250K | < $100K |
| Slippage (avg) | > 0.5% | > 1% |
| Circuit breaker triggered | — | Any trigger |
| Reconciliation discrepancy | — | Any discrepancy |

All metrics exported to Prometheus, dashboarded in Grafana, alerted via PagerDuty.
