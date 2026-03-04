# RVSwaps — Development Roadmap

---

## Overview

Six phases with explicit gates. Each gate must be passed before the next phase begins. No phase is started unless its predecessor has demonstrated results.

**Critical addition: Phase 0 (Inventory Seeding)** runs in parallel with Phase 1. The marketplace must have goods to buy before it launches.

```
Phase 0 ──────────────────────────────────────┐ (parallel)
Phase 1 ──→ Phase 2 ──→ Phase 3 ──→ Phase 4 ──→ Phase 5 ──→ Phase 6
Contracts   Wallets &   Marketplace  Staking &   Governance  Scale &
+ Audit     Fiat Rails  MVP          Mobile      & DAO       Expansion
(2 months)  (2 months)  (3 months)   (2 months)  (3 months)  (ongoing)

Inventory seeding (Phase 0) starts Month 1, runs continuously.
```

**MVP ships at end of Phase 3: Month 7.** This is when real users transact.

---

## Phase 0 — Inventory Seeding & Community Building (Parallel Track)

**Duration**: Starts Month 1, runs through Phase 3 launch and beyond
**Team**: 1 BD/Community lead + 1 content person + founders

### Deliverables

- [ ] Define launch category (recommend: Trading Cards & Collectibles)
- [ ] Source 1,000–5,000 listings (liquidation lots, wholesale, team personal items)
- [ ] Recruit 20–50 Anchor Sellers with signed agreements
- [ ] Build Discord community to 5,000 members
- [ ] Create social media presence (Twitter, TikTok, Instagram)
- [ ] Develop "Import from eBay" CSV tool
- [ ] Write seller onboarding guide and FAQ
- [ ] Create Daily Deals curation workflow
- [ ] Establish shipping partnership (Pirate Ship or ShipStation integration)
- [ ] Produce launch announcement content (blog post, video, press kit)

### Budget

```
Inventory sourcing:      $10,000–$25,000 (recoverable through sales)
Anchor seller incentives: $5,000 (RV allocation + featured placement)
Marketing pre-launch:    $10,000 (ads, content, Discord tools)
BD lead salary:          $8,000–$12,000/month
Total Phase 0 cost:      ~$40,000–$60,000
```

### Gate: Ready for Phase 3 Launch

- 1,000+ real listings ready to go live
- 20+ committed Anchor Sellers
- 5,000+ Discord members
- Shipping logistics tested end-to-end

---

## Phase 1 — Token & Smart Contracts

**Duration**: 2 months
**Team**: 2 Solidity engineers + 1 security researcher

### Deliverables

- [ ] RVToken contract (ERC-20 + burn mechanics + permit + pause)
- [ ] RVEscrow contract (deposit, release, refund, dispute, auto-release)
- [ ] RVStaking contract (tiered staking, fee-funded rewards)
- [ ] RVBuybackBurn contract (automated treasury buyback)
- [ ] Comprehensive test suite:
  - [ ] Unit tests (Hardhat + Chai, 100% coverage)
  - [ ] Fuzz tests (Foundry)
  - [ ] Invariant tests (Foundry)
  - [ ] Gas profiling
- [ ] Testnet deployment on Polygon Amoy
- [ ] 2-week testnet soak (stability, edge cases)
- [ ] External audit #1 (RVToken + RVEscrow — highest priority)
- [ ] Multi-sig wallet setup (Gnosis Safe, 3-of-5)
- [ ] Token distribution plan finalized
- [ ] Deployment scripts + runbooks

### Key Decisions

- Finalize burn rates and fee basis points
- Select audit firm (get quotes from Trail of Bits, OpenZeppelin, Cyfrin)
- Finalize vesting schedules
- Set up CI/CD for contracts (Hardhat + Foundry in GitHub Actions)

### Gate to Phase 2

- All contracts pass external audit with zero critical/high findings
- Testnet deployment stable for 2+ weeks with no redeployments
- Multi-sig operational with all 5 signers verified
- Gas costs validated within acceptable range

---

## Phase 2 — Wallets, KYC & Fiat Rails

**Duration**: 2 months
**Team**: 2 backend engineers + 1 frontend engineer + 1 DevOps

### Deliverables

- [ ] User registration + authentication (email/password, Google/Apple OAuth)
- [ ] JWT auth with refresh token rotation
- [ ] 2FA (TOTP) setup flow
- [ ] Custodial wallet infrastructure:
  - [ ] AWS CloudHSM integration
  - [ ] HD wallet derivation
  - [ ] Transaction signing service
- [ ] External wallet connection (MetaMask, WalletConnect via wagmi)
- [ ] KYC integration (Sumsub or Onfido) with Tier 0/1/2 flows
- [ ] Fiat on-ramp integration (MoonPay or Transak)
- [ ] Fiat off-ramp / auto-cashout pipeline (Stripe Connect or similar)
- [ ] Conversion Engine v1:
  - [ ] TWAP oracle integration (Chainlink or DEX-based)
  - [ ] USD→RV conversion at checkout
  - [ ] RV→USD conversion for auto-cashout
  - [ ] Circuit breakers (price volatility, oracle staleness)
- [ ] RV balance display + transaction history
- [ ] Basic web UI for account management
- [ ] Cloud infrastructure:
  - [ ] AWS EKS cluster
  - [ ] RDS PostgreSQL (Multi-AZ)
  - [ ] ElastiCache Redis
  - [ ] Message queue (RabbitMQ via Amazon MQ)
  - [ ] S3 + CloudFront for static assets
- [ ] CI/CD pipeline (GitHub Actions → EKS deploy)
- [ ] Monitoring: Prometheus + Grafana + PagerDuty
- [ ] Logging: CloudWatch or ELK stack
- [ ] API security: rate limiting, input validation, CORS, WAF

### Gate to Phase 3

- 100 internal testers can: create account → complete KYC → buy RV with card → see balance → withdraw
- Fiat on-ramp works end-to-end on mainnet
- Conversion Engine: TWAP rate updating, circuit breakers tested
- Custodial wallet: deposit + withdrawal works on Polygon mainnet
- Penetration test completed with no critical findings
- Infrastructure handles 100 concurrent users without degradation

---

## Phase 3 — Marketplace MVP

**Duration**: 3 months
**Team**: 3 backend + 2 frontend + 1 designer + 1 QA

### Deliverables

- [ ] Listing creation (title, description, USD price, category, condition, images)
- [ ] Image upload + processing (resize, compress, CDN)
- [ ] Elasticsearch integration:
  - [ ] Full-text search
  - [ ] Category faceting
  - [ ] Price range filters
  - [ ] Condition filters
  - [ ] Sort by: newest, price low-high, price high-low, relevance
- [ ] Listing detail page (USD price prominently, "Buy Now", "Make Offer")
- [ ] Browse pages: category grid, Daily Deals, Just Listed, Rare Finds
- [ ] Offer/counter-offer negotiation (max 3 rounds)
- [ ] Purchase flow (crypto-invisible):
  - [ ] Checkout page with USD total
  - [ ] Payment: Credit Card, ACH, Apple Pay, or RV Balance
  - [ ] Behind the scenes: Stripe charge → Conversion Engine → on-chain escrow
- [ ] Order management (buyer + seller dashboards):
  - [ ] Order status tracking
  - [ ] Shipping: tracking number entry, carrier detection
  - [ ] Delivery confirmation (manual + auto after 14 days)
- [ ] Seller payout:
  - [ ] Auto-cashout to USD (default)
  - [ ] Keep as RV (opt-in)
  - [ ] Payout preference settings
- [ ] Ratings (1–5 stars + text, only after completed order)
- [ ] Dispute system:
  - [ ] Open dispute with reason + evidence
  - [ ] Admin resolution dashboard
  - [ ] On-chain dispute resolution execution
- [ ] Moderation:
  - [ ] Image screening (AWS Rekognition)
  - [ ] Text filtering (prohibited content)
  - [ ] Manual review queue
- [ ] Notifications:
  - [ ] Email (transactional)
  - [ ] Push notifications (FCM)
  - [ ] In-app notification feed
- [ ] Risk & Fraud Service v1:
  - [ ] Rules-based risk scoring
  - [ ] Device fingerprinting
  - [ ] Velocity checks
- [ ] Mobile-responsive web design
- [ ] Anchor Seller onboarding (white-glove setup)
- [ ] Platform-sourced inventory goes live

### Gate to Phase 4

- 1,000+ real listings from external users (not platform-sourced)
- 500+ completed transactions with real shipping
- Dispute rate below 5%
- Average time-to-first-purchase under 10 minutes for new users
- Seller auto-cashout working reliably (>99% success rate)
- Conversion Engine: 0 reconciliation discrepancies over 2 weeks
- NPS score > 30 from early user surveys

---

## Phase 4 — Staking UI, Burns Dashboard & Mobile App

**Duration**: 2 months
**Team**: 2 backend + 2 frontend + 1 mobile engineer

### Deliverables

- [ ] Staking UI:
  - [ ] Tier selection with APY display
  - [ ] Stake, unstake, claim rewards
  - [ ] Position overview with unlock countdowns
  - [ ] Fee discount display for stakers
- [ ] Burns dashboard:
  - [ ] Total burned (cumulative + chart over time)
  - [ ] Burn rate (daily/weekly/monthly)
  - [ ] Buyback history (amount, price, tx hashes)
  - [ ] Circulating supply chart
- [ ] Buyback Engine automation:
  - [ ] Weekly automated buyback execution
  - [ ] TWAP-spread over 4 hours
  - [ ] Dashboard reporting
- [ ] The Graph subgraph deployment (all contract events)
- [ ] Promoted listings feature (pay to boost visibility)
- [ ] Featured Seller program
- [ ] Seller analytics (premium):
  - [ ] Sales trends, top items, pricing suggestions
- [ ] React Native mobile app:
  - [ ] iOS + Android
  - [ ] Camera listing creation
  - [ ] Biometric auth
  - [ ] Push notifications
  - [ ] Deep linking
- [ ] Referral program:
  - [ ] Unique referral codes
  - [ ] Both parties get 25 RV on first completed transaction
  - [ ] Anti-Sybil: shipping to different address required
- [ ] Buyer cashback (0.5% in RV)
- [ ] Public API documentation

### Gate to Phase 5

- Staking TVL > $100,000 USD equivalent
- Monthly transaction volume > 15,000
- Mobile app: >4.0 star rating on App Store / Play Store
- Buyback Engine: 4+ successful weekly executions
- Platform trending toward cash-flow positive (or clear path)

---

## Phase 5 — Governance & Advanced Features

**Duration**: 3 months
**Team**: 2 backend + 2 frontend + 1 Solidity + 1 security

### Deliverables

- [ ] RVGovernance contract:
  - [ ] Proposal creation (Diamond stakers only)
  - [ ] Voting (all stakers, weight = staked amount)
  - [ ] Quorum: 5% of staked supply
  - [ ] 48-hour timelock on execution
- [ ] Governance UI:
  - [ ] Proposal list, voting, results
  - [ ] Discussion threads per proposal
- [ ] Governance audit (external)
- [ ] Advanced search:
  - [ ] AI-powered recommendations ("similar items", "you might like")
  - [ ] Image search (photo → matching listings)
- [ ] In-app messaging (encrypted, between buyer/seller)
- [ ] Seller verification badges (identity, high volume, trusted)
- [ ] Automated pricing suggestions (ML model based on sold items)
- [ ] Shipping label generation (partnership integration)
- [ ] Multi-language support (Spanish, Portuguese, French)
- [ ] Advanced fraud detection (ML model replacing rules-based)

### Gate to Phase 6

- 50,000+ monthly active users
- Governance: >5% participation rate
- Revenue consistently covers operating costs
- Demand for additional chains validated by user feedback

---

## Phase 6 — Scale & Expansion

**Duration**: Ongoing
**Team**: Full org (15–25 engineers)

### Deliverables

- [ ] Multi-chain deployment (Arbitrum or Base)
- [ ] Cross-chain bridge for RV
- [ ] International expansion (Canada, UK, EU first)
- [ ] Enterprise seller tools (bulk API, inventory sync, ERP integration)
- [ ] Auction-format listings
- [ ] Local pickup / meetup feature
- [ ] Authentication services for high-value items
- [ ] White-label marketplace platform
- [ ] Advanced fraud ML
- [ ] Real-time customer support (chat)
- [ ] Strategic brand partnerships

---

## Lean MVP Approach

If funding is limited, here's what to cut and what to keep:

### Must Have (MVP Core)

```
✓ RVToken + RVEscrow contracts (audited)
✓ Custodial wallets (can be simplified — single HSM key, not full HD)
✓ KYC (Tier 0 + Tier 1 only)
✓ Fiat on/off ramp (one provider: Stripe for fiat, MoonPay for crypto)
✓ Listings + search (Postgres full-text search instead of Elasticsearch)
✓ Purchase flow with escrow
✓ Seller auto-cashout to USD
✓ Basic notifications (email only)
✓ Mobile-responsive web (skip native app initially)
```

### Nice to Have (Defer)

```
○ RVStaking contract (add after marketplace proves itself)
○ RVBuybackBurn (add when fee volume justifies it)
○ Elasticsearch (Postgres full-text search is sufficient to start)
○ React Native mobile app (responsive web is fine)
○ Promoted listings (add when sellers ask for it)
○ ML fraud detection (rules-based is fine to start)
○ In-app messaging (email between parties is fine)
○ Multi-language
```

### Skip Until Scale

```
✗ Governance / DAO
✗ Multi-chain
✗ White-label
✗ AI recommendations
✗ Auction format
```

### Lean MVP Timeline

```
With 3 engineers + 1 designer + 1 BD person:

Month 1-2:  Smart contracts + basic tests (1 Solidity dev)
            Backend foundation: auth, wallet, DB (2 backend devs)
Month 3:    KYC + fiat rails + conversion engine
Month 4-5:  Marketplace features (listings, search, orders)
Month 6:    Purchase flow + escrow integration + notifications
Month 7:    Testing, bug fixes, Anchor Seller onboarding
Month 8:    LAUNCH (with seeded inventory)

Total lean budget: ~$600K (8 months × $75K/month)
```

---

## Risk List

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Smart contract bug post-launch | Critical | Medium | 2 audits, bug bounty, pause capability |
| Oracle failure / manipulation | High | Low | Multi-source oracle, circuit breakers, TWAP |
| Fiat rail provider drops us | High | Low | Integrate 2 providers (primary + backup) |
| Scaling bottleneck (DB, search) | Medium | Medium | Start simple (Postgres FTS), add ES when needed |
| CloudHSM key management incident | Critical | Low | Key ceremony procedure, backup seeds, insurance |

### Business Risks

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| No product-market fit | Fatal | Medium | Phase 0 seeding tests demand before full build |
| Token classified as security | Fatal | Medium | Legal counsel, utility-first positioning |
| Money transmitter license issues | High | Medium | Partner with licensed custodian first |
| Anchor sellers don't commit | High | Medium | 0% fees + founder badge + featured placement |
| Insufficient funding | High | Medium | Lean MVP approach, Phase 0 validates before heavy spend |
| Competitor launches similar | Medium | Low | First-mover in "crypto-invisible marketplace" niche |

### Operational Risks

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Fraud overwhelms moderation | High | Medium | Automated screening + KYC gates + risk scoring |
| Chargebacks drain treasury | High | Low | Risk scoring, Stripe Radar, ACH preference |
| Key team member leaves | High | Medium | Document everything, no single-person dependencies |
| Customer support overwhelmed | Medium | High | Self-service tools, FAQ, chatbot before hiring |

---

## Team Requirements by Phase

| Phase | Engineers | Design | Ops/BD | Other |
|---|---|---|---|---|
| 0 | 0 | 0 | 1 BD + 1 Content | Founders |
| 1 | 2 Solidity + 1 Security | 0 | 0 | Legal counsel |
| 2 | 2 Backend + 1 Frontend | 0 | 1 DevOps | KYC provider |
| 3 | 3 Backend + 2 Frontend | 1 | 1 DevOps | 1 QA, 1 Community |
| 4 | 2 Backend + 2 Frontend + 1 Mobile | 1 | 1 DevOps | 1 Marketing |
| 5 | 2 Backend + 2 Frontend + 1 Solidity | 1 | 1 DevOps | 1 Security |
| 6 | 10–15 across functions | 2–3 | 2–3 | Full org |

**Minimum team to launch MVP (Phase 3)**: 5 engineers + 1 designer + 1 BD/community = **7 people**

---

## Budget Summary (Through Phase 4 — 10 months)

| Category | Monthly Avg | 10-Month Total |
|---|---|---|
| Engineering (7 people avg) | $105,000 | $1,050,000 |
| BD/Community/Marketing | $20,000 | $200,000 |
| Cloud infrastructure | $5,000 → $15,000 | $80,000 |
| Smart contract audits (2×) | — | $150,000 |
| Legal & compliance | $10,000 | $100,000 |
| KYC/AML provider | $3,000 | $30,000 |
| Inventory seeding | — | $25,000 |
| Bug bounty seed | — | $50,000 |
| Tools & services | $3,000 | $30,000 |
| Designer | $10,000 | $100,000 |
| Contingency (15%) | — | $272,000 |
| **Total** | | **~$2,087,000** |

**Funding strategy**:
1. Seed round: $2.5M–$4M (covers through Phase 4 + runway)
2. Token public sale: 15% allocation (supplementary if needed)
3. Polygon ecosystem grant (they fund marketplace projects)
4. Revenue (post-Phase 3, growing toward break-even)
