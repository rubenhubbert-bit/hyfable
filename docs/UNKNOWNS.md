# RVSwaps — Unknowns & Assumptions to Validate

---

## Purpose

This document lists every assumption the RVSwaps design makes that has NOT been validated. Before committing significant resources, each assumption should be tested. Items are ranked by risk (how badly we're wrong if the assumption fails).

---

## 1. Critical Assumptions (Validate BEFORE Building)

### A1: Resellers Will Switch Platforms for Lower Fees

**Assumption**: eBay/Mercari sellers will list on RVSwaps because we charge 0% seller fees vs. 10–20% on incumbents.

**Risk if wrong**: No supply. Empty marketplace. Everything fails.

**How to validate**:
- Interview 20+ active resellers. Ask: "What would make you list on a new marketplace?"
- Run a landing page with the value prop. Measure signup rate.
- Ask: "Would you list here if you could import your eBay listings in one click?"
- Critical follow-up: "Would you list EXCLUSIVELY here, or just cross-list?" (Cross-listing is fine for start.)

**Status**: NOT VALIDATED

---

### A2: Buyers Will Transact on a New, Unknown Marketplace

**Assumption**: Buyers will trust RVSwaps enough to purchase items, especially without the brand recognition of eBay or Amazon.

**Risk if wrong**: No demand. Sellers leave. Everything fails.

**How to validate**:
- Run a deal-focused social media campaign. Drive traffic to listings. Measure conversion.
- Emphasize escrow/buyer protection in all messaging.
- Study competitors who launched recently (Whatnot, Mercari's early days, GOAT) for tactics.
- Can we get press coverage? TechCrunch / crypto media? Brand legitimacy matters.

**Status**: NOT VALIDATED

---

### A3: The "Crypto Invisible" UX Actually Works

**Assumption**: We can make the checkout experience feel like a normal marketplace while executing on-chain escrow behind the scenes.

**Risk if wrong**: Checkout is slow (5–10 seconds for on-chain confirmation), confusing (error states from blockchain), or unreliable (RPC failures, oracle stale).

**How to validate**:
- Build a prototype of the checkout flow end-to-end on testnet.
- Measure latency: time from "Place Order" click to "Order Confirmed" screen.
- Test failure modes: what happens when RPC is down? Oracle stale? Conversion Engine error?
- User test with 10 non-crypto people. Can they complete a purchase without confusion?

**Status**: NOT VALIDATED — this is the highest-risk technical assumption.

---

### A4: TWAP Oracle Provides Accurate, Timely Pricing

**Assumption**: A 30-minute TWAP on a QuickSwap pool (or Chainlink feed) gives reliable USD→RV conversion rates that don't deviate significantly from fair market value.

**Risk if wrong**: Users get bad conversion rates. Platform loses money on conversions. Arbitrage opportunities.

**How to validate**:
- Simulate TWAP calculations against historical DEX data for similar-size token pairs.
- Compare TWAP vs spot price deviation across different volatility regimes.
- Stress test: what's the maximum TWAP deviation during a 30% daily move?
- Is 30 minutes the right window? Too long = stale. Too short = manipulable.

**Status**: NOT VALIDATED

---

### A5: Token Won't Be Classified as a Security

**Assumption**: RV can be legally sold and used as a utility token in the US and EU.

**Risk if wrong**: SEC enforcement. Token sale is illegal. Project shuts down.

**How to validate**:
- Engage securities counsel (not just any lawyer — a crypto-specialized firm).
- Get a formal legal opinion letter.
- Review Howey test analysis: Is there an investment of money? In a common enterprise? With expectation of profits? Based on the efforts of others?
- The staking APY and buyback-burn mechanic could be problematic. Counsel must opine on these specifically.

**Status**: NOT VALIDATED — engage legal counsel before writing any marketing materials.

---

## 2. High-Risk Assumptions (Validate During Phase 1–2)

### A6: Auto-Cashout Economics Work at Low Volume

**Assumption**: Converting seller payments from RV→USD via DEX or treasury at low volume is affordable and doesn't result in excessive slippage.

**Risk if wrong**: Platform loses money on every seller cashout. Treasury drains.

**How to validate**:
- Model the economics: at 100 transactions/day, what's the daily cashout volume?
- At that volume, what's the price impact on QuickSwap?
- How much treasury USDC do we need to absorb cashouts without DEX routing?
- What if 90% of sellers choose USD cashout (likely initially)?

**Open question**: Should we batch cashouts (every 4 hours) instead of instant-per-order?

---

### A7: Meta-Transactions Scale Reliably

**Assumption**: OpenZeppelin Defender Relayer (or similar) can handle all our gas sponsorship needs without becoming a bottleneck or single point of failure.

**Risk if wrong**: Transactions fail or are slow. User experience degrades.

**How to validate**:
- Load test the relayer at target transaction volume
- Test failover: what happens when the relayer is down?
- Benchmark: how many transactions/second can a single relayer handle?
- Cost analysis: at 10,000 transactions/day, what's the daily gas bill?

---

### A8: KYC Conversion Rate Is Acceptable

**Assumption**: Users will complete KYC (ID + selfie) when prompted at the $50 purchase threshold without abandoning.

**Risk if wrong**: Most users bounce at KYC. Conversion funnel broken.

**How to validate**:
- Research: what's the industry KYC completion rate? (Typically 60–80% for well-designed flows.)
- Should the threshold be higher ($100? $200?) to reduce friction for casual buyers?
- Can we do a "light KYC" tier (phone number only) before full ID verification?
- A/B test: KYC at $50 vs $100 vs $200 threshold.

---

### A9: Customer Support Load Is Manageable

**Assumption**: Dispute rate stays below 5%, and support tickets can be handled by a small team.

**Risk if wrong**: Support overwhelmed. Disputes pile up. Users leave.

**How to validate**:
- Study eBay/Mercari dispute rates (typically 1–3% for established platforms)
- For a new platform with unknown sellers, expect 5–10% initially
- Plan for: 1 support person per 5,000 monthly transactions
- Automated tools: canned responses, FAQ bot, self-service dispute filing

---

## 3. Medium-Risk Assumptions (Validate During Phase 3)

### A10: Trading Cards Is the Right Launch Category

**Assumption**: Trading cards + collectibles is the best vertical to start with.

**Risk if wrong**: Category is too niche, not enough volume, or wrong demographic.

**How to validate**:
- Survey potential users: "What would you buy/sell on a new marketplace?"
- Analyze search volume for category-specific terms
- Compare: trading cards vs electronics vs sneakers on existing platforms
- Which category has the most active reseller community willing to try new platforms?

---

### A11: 2.5% Buyer Fee Is Sustainable

**Assumption**: 2.5% is enough revenue per transaction to cover costs and fund staking + buyback.

**Risk if wrong**: Not enough revenue. Can't fund operations or staking rewards.

**How to validate**:
- Per-transaction unit economics model (done in MONETIZATION doc)
- Sensitivity analysis: what if average order value is $25 instead of $50?
- What if we need to raise fees? How price-sensitive are users?
- Compare: Whatnot charges 0% seller fee but takes buyer-side fees too

---

### A12: Protocol-Owned Liquidity Is Sufficient

**Assumption**: 100M RV in DEX pools provides enough liquidity for all conversion needs.

**Risk if wrong**: Large orders cause excessive slippage. Conversions fail.

**How to validate**:
- Model: what pool depth is needed for a $500 order with <1% slippage?
- At launch USDC-equivalent value of POL: depends on launch price of RV
- Is 100M RV enough, or do we need 200M? Trade-off with other allocations.

---

### A13: Staking APY Is Attractive Enough

**Assumption**: 3–18% APY (depending on tier) is competitive enough to attract stakers.

**Risk if wrong**: Nobody stakes. No TVL. Staking mechanic is irrelevant.

**How to validate**:
- Compare to DeFi yields: Aave (2–5%), Compound (3–7%), Lido (3–4%)
- Our 14–18% for locked tiers is above DeFi averages — competitive
- But: users must trust a new protocol. Higher APY = more trust needed.
- Risk perception: users may see 18% as "too good to be true" → scam signal

---

## 4. Low-Risk Assumptions (Monitor Post-Launch)

### A14: Polygon Remains the Best Chain

**Assumption**: Polygon PoS continues to be the best choice for fees, speed, and ecosystem.

**Monitor**: Track Polygon fees, reliability, and compare with Base, Arbitrum, zkSync.
If Polygon degrades, multi-chain deployment (Phase 6) provides an escape valve.

### A15: Burn Rate Is Calibrated Correctly

**Assumption**: 1% burn per transaction is the right rate — not too aggressive, not too passive.

**Monitor**: Track circulating supply trajectory. If burning too fast, governance can reduce rate. If too slow, governance can increase. The burn floor provides a safety net.

### A16: The Buyback Mechanism Matters at Low Volume

**Assumption**: Even small buybacks ($1K/week) provide meaningful buy pressure.

**Reality check**: At low volume, buybacks are negligible. A $1K weekly buy on a $1M liquidity pool moves price by <0.1%. The buyback matters at scale but is mostly symbolic early on. That's fine — it builds the mechanism that scales later.

---

## 5. Open Design Questions

### Q1: Custody Partner vs. Build Own

Should we partner with a licensed custodian (Circle, Zero Hash, Fireblocks) or build our own custodial wallet infrastructure?

**Trade-offs**:
- Partner: faster to market, handles licensing, costs more per-tx (10–30 bps)
- Build own: full control, lower marginal cost, requires MTL, higher upfront investment

**Recommendation**: Partner first (Phase 2), evaluate build-own at Phase 4 when economics justify it.

### Q2: Stripe vs. Dedicated Crypto Ramp

For fiat→RV conversion, should we use Stripe (charge card → platform buys RV) or a dedicated crypto on-ramp (MoonPay, Transak)?

**Trade-offs**:
- Stripe: familiar to users, better conversion rates, but we handle the RV purchase ourselves
- MoonPay: user buys RV directly, simpler for us, but user sees a "crypto purchase" flow (less invisible)

**Recommendation**: Stripe for the invisible flow (user pays card, we handle conversion). MoonPay as an option for users who explicitly want to "Buy RV" from the wallet tab.

### Q3: What If RV Price Goes to Near-Zero?

If RV trades at $0.001, a $50 item costs 50,000 RV. The system still works (USD pricing doesn't change), but it looks ugly and gas costs per RV token become proportionally higher.

**Mitigation**: This scenario likely means the marketplace failed, not that the token design is wrong. If the marketplace has no users, RV has no utility, and price approaches zero. The solution is to build a great marketplace, not to engineer around a token failure.

### Q4: Multi-Sig Key Ceremony

Who holds the 5 multi-sig keys? If it's 2 founders + 1 advisor + 1 legal + 1 security auditor, what happens if a founder is incapacitated?

**Action item**: Document key ceremony procedure. Include: backup key holders, incapacitation protocol, quarterly rotation check, hardware wallet requirements.

### Q5: What Jurisdictions Do We Launch In?

US-first? EU? Global? Each has different regulatory requirements.

**Recommendation**: US-only at launch. Canada + UK in Phase D. EU requires MiCA compliance (evaluate at Phase 5). Restrict all sanctioned countries from day one.

---

## 6. Validation Priority Matrix

| # | Assumption | Risk | Effort to Validate | When |
|---|---|---|---|---|
| A1 | Resellers will switch | Fatal | Low (interviews) | Before any code |
| A2 | Buyers will trust us | Fatal | Low (landing page) | Before any code |
| A5 | Not a security | Fatal | Medium (legal opinion) | Before any code |
| A3 | Crypto-invisible UX works | Critical | Medium (prototype) | Phase 2 |
| A4 | TWAP oracle is reliable | Critical | Medium (simulation) | Phase 2 |
| A6 | Auto-cashout economics | High | Low (spreadsheet) | Phase 2 |
| A7 | Meta-tx scale | High | Medium (load test) | Phase 2 |
| A8 | KYC conversion rate | High | Low (A/B test) | Phase 3 |
| A9 | Support load manageable | High | Low (benchmark) | Phase 3 |
| A10 | Right launch category | Medium | Low (survey) | Phase 0 |
| A11 | 2.5% fee sustainable | Medium | Low (model) | Phase 3 |
| A12 | POL is sufficient | Medium | Low (model) | Phase 1 |
| A13 | Staking APY competitive | Low | Low (comp analysis) | Phase 4 |

**Action: Validate A1, A2, and A5 before writing a single line of production code.** If any of these fail, the project needs fundamental redesign.
