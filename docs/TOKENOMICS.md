# RVSwaps — Tokenomics Model

---

## 1. Token Overview

| Property | Value |
|---|---|
| Name | ReValue Coin |
| Symbol | RV |
| Standard | ERC-20 (Polygon PoS) |
| Max Supply | 1,000,000,000 (1 billion) |
| Decimals | 18 |
| Type | Deflationary utility token |
| Pricing | USD-denominated marketplace; RV is the payment rail |

---

## 2. Token Distribution

| Allocation | % | Amount | Vesting |
|---|---|---|---|
| Public Sale (IDO/Launchpad) | 15% | 150,000,000 | 25% at TGE, 75% linear over 6 months |
| Team & Founders | 15% | 150,000,000 | 1-year cliff, then 3-year linear vest |
| Ecosystem & Rewards | 25% | 250,000,000 | Emitted via staking rewards over 5 years |
| Platform Treasury | 15% | 150,000,000 | DAO-controlled post-governance; multi-sig before |
| Staking Rewards Pool | 15% | 150,000,000 | Released per emission schedule (see below) |
| Liquidity Provision (POL) | 10% | 100,000,000 | Permanently locked in DEX pools |
| Advisors & Partners | 5% | 50,000,000 | 6-month cliff, 2-year linear vest |

### Initial Circulating Supply at TGE

```
Public sale TGE unlock:     37,500,000  (25% of 150M)
Liquidity provision (POL): 100,000,000  (locked in DEX, tradeable)
                           ───────────
Initial circulating:       137,500,000  (~13.75% of max supply)
```

Low initial float creates scarcity at launch. The 1-year cliff on team tokens means insiders cannot sell for 12 months. POL tokens are in the DEX pool — they provide liquidity but the LP position itself is permanently locked (protocol-owned, never withdrawable).

---

## 3. Burn Mechanics

Tokens burned are sent to `0x000...dead` and permanently removed from supply. Burns are the primary deflationary mechanism.

### Burn Schedule

| Action | Burn Rate | Source | Example |
|---|---|---|---|
| Item purchase | 1.0% of item price | Deducted from escrow at release | $45 item → 4.46 RV burned |
| Fiat → RV conversion | 0.5% of converted amount | Deducted from conversion | Buy 1000 RV → 5 RV burned |
| RV withdrawal to external wallet | 2.0% of amount | Deducted from withdrawal | Withdraw 500 RV → 10 RV burned |
| Listing boost (premium) | 100% of boost fee | Entire boost fee burned | 5 RV boost → 5 RV burned |
| Dispute resolution (losing party) | 1.5% of order value | Penalty on loser | $200 order → ~3 RV burned |
| **Buyback burn** | **100% of buyback** | **Weekly treasury buyback** | **$500 buyback → all RV burned** |

### What Is NOT Burned

- Staking / unstaking — no burn (to avoid discouraging staking)
- Internal RV transfers between users — no burn (encourage tipping/sending)
- Offer negotiation — no burn until actual purchase
- Claiming staking rewards — no burn

### Burn Floor (Anti-Death-Spiral)

To prevent excessive deflation that would make RV too scarce for marketplace use:

```
Supply > 500M (50% of max):   Full burn rates apply
Supply 250M–500M:             All burn rates halved
Supply < 250M (25% of max):   Burns stop entirely
```

This is enforced on-chain in the `RVToken.effectiveBurnAmount()` function. No admin can override it. The burn floor ensures the token remains liquid enough to function as a medium of exchange even at very high adoption.

---

## 4. Buyback & Burn Engine

This is the **new mechanism** that didn't exist in the prior design. It creates constant buy pressure proportional to marketplace activity.

### How It Works

```
1. Every completed transaction generates a 2.5% platform fee
2. 10% of all fees are routed to the Buyback Pool
3. Weekly, the Buyback Engine executes:
   a. Read buyback pool balance (in USDC)
   b. Execute TWAP-spread market buys on QuickSwap RV/USDC pool
      - Split into small orders over 4 hours
      - Max 0.5% of pool per individual trade
      - Slippage limit: 2%
   c. All purchased RV is burned via RVBuybackBurn contract
   d. Burn event published to analytics dashboard

4. Transparency: every buyback is logged on-chain
   - tx hashes published to public dashboard
   - total bought, average price, total burned
```

### Buyback Projections

```
Monthly GMV:         $500,000
Platform fees (2.5%): $12,500
Buyback portion (10%): $1,250/month

At RV price $0.10:
  Monthly buyback:    12,500 RV
  Annual buyback:    150,000 RV  (0.015% of max supply)
  Combined with transaction burns: ~1.8M RV/year

At scale ($5M monthly GMV):
  Monthly buyback:   125,000 RV
  Annual buyback:  1,500,000 RV  (0.15% of max supply)
  Combined with transaction burns: ~18M RV/year (1.8% of max supply)

At $50M monthly GMV:
  Annual buyback + burns: ~180M RV/year (18% of max supply)
  → At this scale, burn floor becomes relevant
```

### Why Buyback + Burn > Just Burns

Transaction burns reduce supply passively. Buyback + burn adds **active buy pressure**:
- Creates visible, predictable demand for RV on the open market
- Increases DEX trading volume (good for liquidity depth)
- Demonstrates protocol revenue flowing back to token holders
- Can be tuned independently of transaction burn rates

---

## 5. Protocol-Controlled Liquidity (POL)

### What Is POL

Protocol-Controlled Liquidity means the platform permanently owns DEX liquidity positions. Unlike third-party LPs who can withdraw at any time (especially during crashes), POL is always there.

### POL Structure

```
Initial deployment:
  50,000,000 RV + equivalent USDC → QuickSwap RV/USDC pool
  50,000,000 RV + equivalent MATIC → QuickSwap RV/MATIC pool

LP tokens: owned by protocol multi-sig, NEVER withdrawable
  - LP tokens are sent to a TimelockController with no withdrawal function
  - Even with admin access, liquidity cannot be pulled
  - This is verifiable on-chain

POL provides:
  1. Guaranteed minimum liquidity (even if all third-party LPs leave)
  2. Trading fee revenue for the protocol (~0.3% of all DEX volume)
  3. Price stability floor (large POL = harder to crash the price)
```

### POL Growth Over Time

```
Phase 1 (launch):     100M RV in DEX pools (10% of supply)
Phase 2 (6 months):   Trading fees from POL are compounded back into LP positions
Phase 3 (12 months):  Additional RV from treasury allocated to POL if liquidity
                      depth targets aren't met
Target:               POL should represent ≥50% of total DEX liquidity
```

---

## 6. Staking Model

### Tiers

| Tier | Lock Period | Base APY | Marketplace Benefits | Effective APY |
|---|---|---|---|---|
| Flex | No lock | 3% | None | 3% |
| Bronze | 30 days | 6% | None | 6% |
| Silver | 90 days | 10% | 5% fee discount | 10% |
| Gold | 180 days | 14% | 10% fee discount + early listing access | 14% |
| Diamond | 365 days | 18% | 15% fee discount + governance vote + priority support | 18% |

### Reward Sources (Sustainability Model)

Staking rewards come from **two sources** — this is critical for long-term sustainability:

**Source 1: Ecosystem Emission (Years 1–5)**

```
Year 1:  80,000,000 RV  (32% of 250M ecosystem pool)
Year 2:  60,000,000 RV  (24%)
Year 3:  45,000,000 RV  (18%)
Year 4:  35,000,000 RV  (14%)
Year 5:  30,000,000 RV  (12%)
Total:  250,000,000 RV
```

Emission tapers — this is intentional. Early stakers get higher rewards to bootstrap TVL. By Year 5, emissions drop to zero.

**Source 2: Platform Fee Redistribution (Perpetual)**

```
50% of all platform fees → staking reward pool

At $500K monthly GMV:   $6,250/month to staking pool
At $5M monthly GMV:    $62,500/month to staking pool
At $50M monthly GMV:  $625,000/month to staking pool
```

After Year 5, staking rewards are funded **entirely by platform fees**. If the marketplace is healthy, fee revenue sustains attractive yields. If not, rewards naturally decrease — this is correct incentive alignment. No Ponzi mechanics.

### Sustainability Check: Can Fees Support APY?

```
Scenario: Year 6 (no more emissions)
  TVL (total staked): 200,000,000 RV ($20M at $0.10)
  Weighted average APY target: 10%
  Annual rewards needed: 20,000,000 RV ($2M)

  Fee revenue needed (at 50% to staking):
  $4M annual fees → $333K/month → $13.3M monthly GMV

  At 2.5% fee rate: monthly GMV of $13.3M
  → ~266,000 transactions/month at $50 avg
  → ~8,900 transactions/day

  Is this achievable? Mercari does millions/month. At 1% of Mercari's
  volume, this works. At <1%, APY would naturally decrease to what
  fee revenue supports — which is the correct behavior.
```

### Anti-Gaming Measures

- **Early unstake penalty**: Forfeit 50% of accumulated rewards (principal always safe)
- **Minimum stake**: 100 RV (prevents dust attacks on staking contract)
- **Flash loan protection**: Rewards accrue per-second based on time staked, not balance snapshots. Staking and unstaking in the same block yields zero rewards.
- **Compound cap**: Rewards accrue continuously but are claimable weekly (prevents compound manipulation)
- **No staking multipliers for marketplace activity**: Decouples staking rewards from transaction volume to prevent wash trading incentives

---

## 7. The Velocity Paradox — And How We Solve It

### The Problem

If users expect RV to appreciate, they hoard instead of spending → marketplace dies → no burns → no appreciation → flywheel stalls.

### The Solution: Separated Incentives

```
┌─────────────────────────────────────────────────┐
│  STAKING (hold incentive)                        │
│  "Want appreciation? Stake. Earn yield."         │
│  → Locked tokens reduce circulating supply       │
│  → Stakers earn 3-18% APY                        │
│  → Clean separation: staked RV can't be spent    │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│  MARKETPLACE (spend incentive)                   │
│  "Want to buy stuff? Prices are in USD."         │
│  → USD-denominated listings                      │
│  → Buyer sees "$45" not "446 RV"                 │
│  → Spending doesn't feel like losing investments │
│  → 0.5% cashback in RV rewards spending          │
└─────────────────────────────────────────────────┘
```

**Key insight**: Because listings are priced in USD, spending RV doesn't feel like selling an investment. It feels like using a payment method. The USD price is fixed; the RV amount adjusts. Users mentally account in USD, not RV.

### Token Flow Equilibrium

```
                    ┌──────────┐
    Fiat on-ramp──→ │  Active  │ ──→ Spend on items (USD checkout)
    (buy RV)        │  Balance │         │
                    └────┬─────┘         │ (1% purchase burn)
                         │               ▼
                    ┌────▼─────┐    ┌──────────┐
                    │  Staking │    │  Burned   │
                    │  Pool    │    │  (gone)   │
                    └────┬─────┘    └───────────┘
                         │               ▲
                    Yield earned          │ (buyback burn)
                         │               │
                    ┌────▼─────┐    ┌────┴───────┐
                    │  Active  │    │  Buyback   │
                    │  Balance │    │  Pool      │
                    └──────────┘    └────────────┘
                                        ▲
                                        │ (10% of fees)
                                   ┌────┴───────┐
                                   │  Platform  │
                                   │  Fees      │
                                   └────────────┘
```

---

## 8. Quantitative Model: Low-Volume Sensitivity Analysis

### Scenario 1: Struggling (1,000 monthly transactions)

```
Monthly GMV:              $50,000 (1000 tx × $50 avg)
Platform fees:            $1,250
Burns (transaction):       500 RV/month
Burns (buyback):           125 RV/month (10% of fees)
Total monthly burn:        625 RV

Staking pool funding:     $625/month (50% of fees)
If 50M RV staked:         Effective APY ≈ 0.15%

Assessment: Token economics are negligible. Burns don't matter.
            Staking rewards are near zero.
            → At this volume, token economics don't save you.
            → The marketplace must work on its own merits.
            → This is honest and by design.
```

### Scenario 2: Moderate (50,000 monthly transactions)

```
Monthly GMV:              $2,500,000
Platform fees:            $62,500
Burns (transaction):      25,000 RV/month
Burns (buyback):          6,250 RV/month
Burns (other):            5,000 RV/month
Total monthly burn:       36,250 RV (435K/year = 0.04% of supply)

Staking pool funding:     $31,250/month
If 100M RV staked:        Effective APY ≈ 3.75% (fees only)
                          + emissions = ~7-12% (Year 1)

Assessment: Burns are noticeable but not dramatic.
            Staking is attractive with emission boost.
            Flywheel is warming up.
```

### Scenario 3: Scale (500,000 monthly transactions)

```
Monthly GMV:              $25,000,000
Platform fees:            $625,000
Burns (transaction):      250,000 RV/month
Burns (buyback):          62,500 RV/month
Burns (other):            50,000 RV/month
Total monthly burn:       362,500 RV (4.35M/year = 0.44% of supply)

Staking pool funding:     $312,500/month
If 200M RV staked:        Effective APY ≈ 18.75% (fees only!)
                          → Emissions not even needed

Assessment: Burns meaningful. Fee-funded staking is self-sustaining.
            Flywheel is running. Demand from marketplace usage
            creates genuine utility floor for RV price.
```

### Scenario 4: Hypergrowth (2,000,000 monthly transactions)

```
Monthly GMV:              $100,000,000
Platform fees:            $2,500,000
Total monthly burn:       1,450,000 RV (17.4M/year = 1.74% of supply)
Burn floor hit in:        ~25 years at this rate (healthy)

Staking pool funding:     $1,250,000/month
Staking is extremely profitable → increases demand to hold RV

Assessment: All mechanisms working at full power.
            Burns are significant but burn floor prevents spiral.
            Token has genuine utility value as marketplace payment rail.
```

---

## 9. Death Spiral Analysis & Mitigations

### What Causes Death Spirals

```
Token price drops → users lose confidence → sellers leave
  → fewer listings → fewer buyers → less activity
  → fewer burns → no deflationary pressure → further price drop
  → death spiral
```

### Why RVSwaps Is More Resistant

**Mitigation 1: USD-Denominated Pricing**
A falling RV price doesn't affect marketplace usability. A $45 item is still $45. The buyer pays $45. The seller receives ~$44. More RV tokens change hands, but the user experience is identical. This breaks the link between token price and marketplace utility.

**Mitigation 2: Auto-Cashout Default**
Sellers receive USD by default. They don't hold RV price risk. A crashing RV price doesn't cause seller flight because sellers never touch RV. Only opt-in crypto sellers are exposed, and they're sophisticated enough to understand volatility.

**Mitigation 3: Protocol-Controlled Liquidity**
POL can't be withdrawn. Even if every third-party LP pulls out, the protocol's 100M RV in DEX pools remains. This provides a liquidity floor — you can always sell RV, just with more slippage.

**Mitigation 4: Burn Floor**
Burns slow down as supply decreases. This prevents the "too few tokens to transact" scenario. Even in a crash, the burn rate halves below 500M supply and stops at 250M.

**Mitigation 5: Circuit Breakers**
If RV drops >20% in 1 hour, conversions halt. This prevents panic-driven cascading liquidations. The marketplace continues operating with pending checkouts queued until the circuit breaker clears.

**Mitigation 6: Buyback Pool Acts as Counter-Cyclical Force**
During price drops, the same buyback dollar amount buys MORE RV. The buyback pool provides continuous buy pressure that gets stronger as price decreases. This is automatic and algorithmic.

### Honest Assessment: When Does RVSwaps Fail?

RVSwaps fails if:
1. **The marketplace has no product-market fit** — people don't want to buy/sell goods on it, regardless of tokenomics. No amount of burns or staking saves a marketplace nobody uses.
2. **Regulatory shutdown** — token classified as security, or money transmitter license denied.
3. **Smart contract exploit** — catastrophic loss of escrowed funds.
4. **Sustained low volume (<1,000 tx/month)** — token economics are irrelevant at this scale; the project is just a bad marketplace.

The token economics are a **growth accelerator**, not a **product substitute**. They amplify success but cannot create it.

---

## 10. Governance (Phase 5+)

Deferred until the ecosystem has proven product-market fit.

- **Voting power**: 1 staked RV = 1 vote (only Diamond-tier stakers initially)
- **Proposal types**: Burn rate adjustments, fee changes, treasury spending, feature prioritization, new chain deployments
- **Quorum**: 5% of staked supply must participate
- **Timelock**: 48-hour delay between vote passage and execution
- **Emergency proposals**: 24-hour fast-track with 10% quorum + 4-of-5 multi-sig

Governance is deliberately deferred. Premature governance creates:
- Voter apathy (too few users to care)
- Attack vectors (cheap to buy governance at low market cap)
- Analysis paralysis (committee decision-making kills velocity)

Build the product first. Decentralize later when there are enough stakeholders to meaningfully participate.
