# RVSwaps — Tokenomics Model

---

## 1. Token Overview

| Property | Value |
|---|---|
| Name | ReValue Coin |
| Symbol | RV |
| Standard | ERC-20 (Polygon) |
| Max Supply | 1,000,000,000 (1 billion) |
| Decimals | 18 |
| Type | Deflationary utility token |

---

## 2. Token Distribution

| Allocation | Percentage | Amount | Vesting |
|---|---|---|---|
| Public Sale (IDO/Launchpad) | 15% | 150,000,000 | 25% at TGE, 75% linear over 6 months |
| Team & Founders | 15% | 150,000,000 | 1-year cliff, then 3-year linear vest |
| Ecosystem & Rewards | 25% | 250,000,000 | Emitted via staking and marketplace rewards |
| Platform Treasury | 15% | 150,000,000 | DAO-controlled (post-governance launch) |
| Staking Rewards Pool | 15% | 150,000,000 | Released per staking schedule over 5 years |
| Liquidity Provision | 10% | 100,000,000 | Locked in DEX pools (QuickSwap, Uniswap) |
| Advisors & Partners | 5% | 50,000,000 | 6-month cliff, 2-year linear vest |

### Initial Circulating Supply at TGE

```
Public sale TGE unlock:     37,500,000  (25% of 150M)
Liquidity provision:       100,000,000
                           ───────────
Initial circulating:       137,500,000  (~13.75% of max supply)
```

This low initial float creates scarcity at launch while preventing team dumping. The 1-year cliff on team tokens means insiders cannot sell for 12 months.

---

## 3. Burn Mechanics

Burns are the core deflationary mechanism. Tokens burned are sent to the zero address (`0x000...dead`) and are permanently removed from supply.

### Burn Schedule

| Action | Burn Rate | Burned From | Example |
|---|---|---|---|
| Item purchase | 1.0% of sale price | Buyer's payment | 100 RV item → 1 RV burned |
| Fiat → RV conversion | 0.5% of amount | Converted amount | Buy 1000 RV → 5 RV burned |
| RV withdrawal to external wallet | 2.0% of amount | Withdrawal amount | Withdraw 500 RV → 10 RV burned |
| Listing boost (premium feature) | 100% of fee | Listing fee | 5 RV boost fee → 5 RV burned |
| Dispute resolution (loser pays) | 1.5% of order value | Dispute penalty | 200 RV order → 3 RV burned |

### What Is NOT Burned

- Staking/unstaking — no burn (to avoid discouraging staking)
- Internal RV-to-RV transfers between users — no burn (to encourage tipping/sending)
- Offer negotiation — no burn until actual purchase

### Projected Burn Rate

Assumptions for Year 1 at moderate activity:
```
Monthly transactions:        50,000
Average transaction size:    200 RV
Monthly transaction volume:  10,000,000 RV

Purchase burns (1%):         100,000 RV/month
Fiat conversion burns (0.5%): 25,000 RV/month  (est. 50% of volume from fiat)
Withdrawal burns (2%):        20,000 RV/month  (est. 10% withdrawn)
Boost burns:                   5,000 RV/month
                             ─────────
Monthly burn:               ~150,000 RV
Annual burn:              ~1,800,000 RV  (~0.18% of max supply)
```

At scale (Year 3, 500K monthly transactions):
```
Annual burn:             ~18,000,000 RV  (~1.8% of max supply)
```

The burn rate scales linearly with platform activity. This is the key insight — **token deflation is a direct function of marketplace health**.

### Burn Floor

To prevent excessive deflation at very high scale, implement a **burn floor**: once total supply drops below 500,000,000 (50% of max), burn rates are halved. At 250,000,000 (25%), burns stop entirely. This prevents a death spiral where the token becomes too scarce to be useful as a medium of exchange.

---

## 4. Staking Model

### Tiers

| Tier | Lock Period | Base APY | Bonus | Effective APY |
|---|---|---|---|---|
| Flex | No lock | 3% | None | 3% |
| Bronze | 30 days | 6% | None | 6% |
| Silver | 90 days | 10% | 5% marketplace fee discount | 10% |
| Gold | 180 days | 14% | 10% fee discount + early listing access | 14% |
| Diamond | 365 days | 18% | 15% fee discount + governance vote + priority support | 18% |

### Reward Source

Staking rewards come from **two sources** (this is critical — rewards must be sustainable, not printed from nothing):

1. **Platform fee redistribution** (50% of collected platform fees → staking pool)
2. **Ecosystem Rewards allocation** (250M tokens emitted over 5 years on a halving schedule)

Emission schedule:
```
Year 1:  80,000,000 RV  (32% of ecosystem pool)
Year 2:  60,000,000 RV  (24%)
Year 3:  45,000,000 RV  (18%)
Year 4:  35,000,000 RV  (14%)
Year 5:  30,000,000 RV  (12%)
Total:  250,000,000 RV
```

After Year 5, staking rewards are funded **entirely by platform fees**. If the platform is healthy, fee revenue sustains rewards. If not, rewards naturally decrease — this is the correct incentive alignment.

### Anti-Gaming Measures

- Early unstake penalty: forfeit 50% of accumulated rewards
- Compound frequency cap: rewards compound daily, claimable weekly
- Minimum stake: 100 RV (prevents dust attack on staking contract)

---

## 5. The Velocity Paradox — And How We Solve It

### The Problem

Every crypto-as-currency project faces this: if users expect the token to appreciate, they hoard it instead of spending it. Marketplace activity drops. With no activity, there are no burns. With no burns, the token doesn't appreciate. The flywheel stalls.

### The Solution: Separated Incentives

RVSwaps separates the "hold" incentive from the "spend" incentive:

1. **Staking satisfies the "hold" urge** — Users who want appreciation can stake and earn yield. Their tokens are locked and not in circulation, reducing sell pressure.

2. **USD-denominated pricing satisfies the "spend" urge** — Listings are priced in USD. Buyers see "$25" and pay the RV equivalent. They don't feel like they're "losing" appreciating tokens — they're buying a $25 item.

3. **Spend-to-earn rewards** — Active buyers and sellers earn small RV bonuses (0.5% cashback on purchases, seller volume bonuses). This makes spending feel like earning.

4. **Staked tokens cannot be spent** — Clean separation. Your staking position and your spending balance are different. You never feel conflicted about whether to spend or stake a particular token.

### Token Flow Equilibrium

```
                    ┌──────────┐
         Buy RV ──→ │  Active  │ ──→ Spend on items
                    │  Balance │         │
                    └────┬─────┘         │ (purchase burn)
                         │               ▼
                    ┌────▼─────┐    ┌──────────┐
                    │  Staking │    │  Burned   │
                    │  Pool    │    │  (gone)   │
                    └────┬─────┘    └───────────┘
                         │
                    Yield earned
                         │
                    ┌────▼─────┐
                    │  Active  │ ──→ Spend or re-stake
                    │  Balance │
                    └──────────┘
```

---

## 6. Liquidity Strategy

### DEX Liquidity

Initial liquidity deployed on QuickSwap (Polygon's primary DEX):

- **RV/USDC pool**: Primary trading pair — users can buy/sell RV for stablecoins
- **RV/MATIC pool**: Secondary pair for native Polygon users
- **Protocol-owned liquidity (POL)**: 50M RV + equivalent USDC from treasury, locked permanently. This guarantees minimum liquidity regardless of market conditions.

### Fiat On-Ramp

Integrated via third-party providers (MoonPay, Transak):
```
User wants to buy RV with credit card
  → MoonPay widget opens
  → User pays $100 USD
  → MoonPay buys RV from DEX or OTC
  → RV deposited to user's platform wallet
  → 0.5% burn applied
```

### Internal Liquidity (Platform Reserve)

The platform maintains a working reserve of RV to:
- Instant-fill small purchases (user doesn't need to wait for DEX execution)
- Smooth price impact on large orders
- Provide guaranteed off-ramp liquidity

Reserve target: 5% of circulating supply, funded from treasury.

---

## 7. Economic Flywheel Analysis

### The Positive Case

```
More users → more listings → more buyers → more transactions
  → more burns → lower supply → price appreciation
  → more media attention → more users (cycle repeats)
  → more staking → higher TVL → more confidence
  → more fees → higher staking rewards → more staking (cycle repeats)
```

**This is viable IF**:
1. The marketplace has real product-market fit (people actually want to buy/sell goods on it)
2. Burn rates are calibrated correctly (not too aggressive, not too passive)
3. Fiat on-ramps are smooth (mainstream users won't jump through hoops)
4. Token price has natural demand floor (real utility for buying goods)

### The Risk Case

```
Token price drops → users lose confidence → sellers leave
  → fewer listings → fewer buyers → less activity
  → fewer burns → no deflationary pressure → further price drop
  → negative spiral
```

**Mitigation**: The burn floor + protocol-owned liquidity + USD-denominated pricing all reduce this risk. Because users price items in USD, a falling RV price doesn't directly affect marketplace usability — it just means more RV tokens change hands per transaction.

### Honest Assessment

The flywheel concept is **conditionally viable**. It works if the marketplace achieves critical mass (~10,000 monthly active users with real transactions). Below that threshold, the economic mechanics are too weak to matter. The project should focus on marketplace product-market fit first, token mechanics second.

---

## 8. Governance (Phase 5+)

Once the ecosystem matures, transition to DAO governance:

- **Voting power**: 1 staked RV = 1 vote (only staked tokens can vote)
- **Proposal types**: Burn rate adjustments, fee changes, treasury spending, feature prioritization
- **Quorum**: 5% of staked supply must participate
- **Timelock**: 48-hour delay between vote passage and execution

This is deliberately deferred to Phase 5. Premature governance creates attack vectors and voter apathy. Build the product first, decentralize later.
