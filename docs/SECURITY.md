# RVSwaps — Security & Fraud Architecture

---

## 1. Threat Model Overview

RVSwaps combines three high-value targets: a marketplace (user data, goods), a financial system (token payments, escrow, fiat rails), and a blockchain layer (smart contracts, wallets). Each requires distinct security strategies.

```
┌───────────────────────────────────────────────────────────────────┐
│                      THREAT CATEGORIES                             │
│                                                                   │
│  Smart Contract     │  Platform           │  Marketplace          │
│  ─────────────      │  ────────           │  ───────────          │
│  Reentrancy         │  Account takeover   │  Fake listings        │
│  Access control     │  Credential stuff   │  Non-shipment         │
│  Integer overflow   │  SQL injection      │  Chargebacks          │
│  Flash loans        │  XSS / CSRF         │  Counterfeit goods    │
│  Oracle manipulate  │  API abuse / IDOR   │  Collusion            │
│  Proxy exploits     │  Session hijack     │  Feedback fraud       │
│  Front-running/MEV  │  Rate limit bypass  │                       │
│  Griefing           │  Insider threat     │  Economic             │
│  Key compromise     │  Supply chain       │  ────────             │
│                     │  Phishing           │  Wash trading         │
│                     │                     │  Sybil farming        │
│                     │                     │  Volume spoofing      │
│                     │                     │  Price manipulation   │
│                     │                     │  Liquidity drain      │
└───────────────────────────────────────────────────────────────────┘
```

---

## 2. Smart Contract Security

### 2.1 Reentrancy

**Risk**: Attacker re-enters escrow release/refund to drain funds.

**Mitigations**:
- All state-changing functions use OpenZeppelin `ReentrancyGuard` (`nonReentrant` modifier)
- State updates (status change) happen BEFORE external calls (checks-effects-interactions)
- RV is ERC-20, reducing callback risk vs ETH — but we still guard because token upgrades could introduce hooks (ERC-777 style)

### 2.2 Access Control Exploits

**Risk**: Compromised admin key mints tokens, upgrades to malicious contract, or drains escrow.

**Mitigations**:
- Admin operations require **3-of-5 Gnosis Safe multi-sig**
- **48-hour timelock** on upgrade and role-change operations
- **No timelock on pause** (must be fast for emergencies) — requires 2-of-5
- Role separation: MINTER, BURNER, PAUSER, OPERATOR, ARBITER, EXECUTOR, UPGRADER — no single address holds all
- No EOA (externally owned account) holds admin roles post-deployment
- Admin key rotation procedure: quarterly, documented, tested
- Signer hardware: all multi-sig signers use hardware wallets (Ledger/Trezor)

### 2.3 Proxy Upgrade Attacks

**Risk**: Malicious implementation deployed via proxy upgrade.

**Mitigations**:
- UUPS pattern: upgrade function is in the implementation, controlled by UPGRADER_ROLE
- UPGRADER_ROLE is on the multi-sig with 48-hour timelock
- Every upgrade requires independent audit sign-off
- Storage layout tests: Hardhat plugin `@openzeppelin/hardhat-upgrades` validates no storage collisions
- `_disableInitializers()` in constructor prevents initialization of implementation contracts directly

### 2.4 Flash Loan Attacks

**Risk**: Attacker borrows large RV via flash loan, stakes, claims rewards, repays in same block.

**Mitigations**:
- Staking rewards use time-based accrual: `reward = (annualRate * elapsedTime) / 365 days`
- If `elapsedTime == 0` (same block), reward is 0
- Minimum lock periods before rewards are meaningful
- No governance snapshots (Phase 5+) — when added, will use checkpoint-based voting with delay

### 2.5 Oracle Manipulation

**Risk**: Attacker manipulates RV price on DEX, gets favorable conversion rate at checkout.

**Mitigations**:
- **30-minute TWAP** — not spot price. Attacker would need to sustain manipulation for 30 minutes, which is prohibitively expensive against a liquid pool
- Chainlink price feed as primary oracle (aggregated, resistant to single-DEX manipulation)
- Circuit breaker: >20% price move in 1 hour → halt all conversions
- Large order detection: trades > 0.5% of pool depth are flagged and split
- Conversion Engine validates execution price against TWAP: max 1% deviation

### 2.6 Front-Running / MEV

**Risk**: MEV bots front-run escrow deposits or large DEX swaps.

**Mitigations**:
- Polygon has ~2s block times, reducing the MEV window
- Escrow deposits use pre-committed order IDs — can't be front-run because the orderId is known
- Meta-transactions through a relayer bypass the public mempool
- Buyback trades use slippage protection (`amountOutMinimum`)
- Future: consider Flashbots Protect or private mempool for large treasury operations

### 2.7 Griefing / DoS

**Risk**: Attacker creates dust escrows or positions to bloat storage or exhaust gas.

**Mitigations**:
- Minimum escrow amount enforced (calculated from `sellerAmount > 0`)
- Minimum stake: 100 RV
- Rate limiting on escrow creation (off-chain, in Order Service)
- Gas costs on Polygon are low but not zero — making pure griefing unprofitable

---

## 3. Platform Security

### 3.1 Authentication & Session Management

**Threats**: Account takeover (ATO), credential stuffing, session hijacking.

**Mitigations**:
- **Password hashing**: Argon2id (preferred) or bcrypt with cost factor 12+
- **JWT access tokens**: 15-minute expiry, signed with RS256
- **Refresh tokens**: httpOnly, secure, sameSite=strict cookies. 7-day expiry, rotated on use. Old refresh tokens invalidated on rotation.
- **Rate limiting**: 5 login attempts / 15 min / IP → CAPTCHA. 20 attempts → IP temp-ban (1 hour).
- **2FA**: TOTP required for all withdrawals and high-value transactions. Optional for login (strongly encouraged for sellers).
- **Session binding**: JWT includes device fingerprint hash + IP range. Significant change triggers re-authentication.
- **Account lockout**: 10 failed attempts → locked. Unlock via email verification link + CAPTCHA.
- **Login anomaly detection**: new device, new country, impossible travel → block + notify user

### 3.2 API Security

**Threats**: SQL injection, XSS, CSRF, IDOR, rate limit bypass, mass data scraping.

**Mitigations**:
- **SQL injection**: Parameterized queries everywhere. ORM-enforced (Prisma or Drizzle). Zero raw SQL in application code.
- **XSS**: CSP headers (Content-Security-Policy). All user-generated content sanitized with DOMPurify. React auto-escapes by default.
- **CSRF**: SameSite=strict cookies for auth. CSRF tokens on all state-changing endpoints. Origin header validation.
- **IDOR**: All resource access checks ownership. `GET /api/orders/:id` verifies the authenticated user is buyer or seller on that order.
- **Rate limiting**: Per-user AND per-IP. Redis sliding window. Different limits per endpoint tier:
  - Read endpoints: 100 req/min
  - Write endpoints: 20 req/min
  - Auth endpoints: 5 req/min
  - Payment endpoints: 10 req/min
- **Input validation**: Zod schemas on all endpoints. Request size limit: 1MB. File upload limit: 10MB per image, 10 images per listing.
- **API versioning**: All endpoints prefixed with version. Breaking changes require new version.
- **CORS**: Restricted to known origins (app domain only)

### 3.3 Wallet Security (Custodial)

**Threats**: Key theft, insider theft, unauthorized withdrawals, hot wallet compromise.

**Mitigations**:
- **Master seed**: Stored in AWS CloudHSM (FIPS 140-2 Level 3). Never leaves HSM boundary.
- **HD derivation**: Each user gets a unique derived key path. No user ever touches the master seed.
- **Transaction signing**: Happens inside HSM. Private keys never in application memory.
- **Hot/cold split**: 5% of total deposits in hot wallet (for instant operations). 95% in cold wallet (multi-sig, manual moves).
- **Withdrawal limits**:
  - Tier 0: $100/day
  - Tier 1: $5,000/day
  - Tier 2: $50,000/day
  - Any single withdrawal > $10,000: 24-hour manual review hold
- **Automated monitoring**: Alert if hot wallet balance deviates >10% from expected. Alert if withdrawal volume spikes >3x daily average.
- **Insider threat**: No single employee has full key access. Wallet operations require code review + deployment pipeline. Audit trail on all HSM operations.

### 3.4 Data Protection

- All data encrypted at rest: AES-256 via AWS RDS encryption
- All data encrypted in transit: TLS 1.3 minimum
- PII stored separately from transaction data (separate database schema)
- GDPR-compliant: data deletion capabilities, export, consent management
- Regular automated backups with point-in-time recovery (35-day retention)
- Database access: IAM roles only, no shared credentials, no direct access outside VPC
- KYC data: stored with third-party provider (Sumsub/Onfido), NOT on our servers. We store only: verification status, tier, hashed reference ID.

### 3.5 Infrastructure Security

- **WAF**: AWS WAF with OWASP Core Rule Set — blocks known attack patterns
- **DDoS**: AWS Shield Standard (included) + Shield Advanced for critical endpoints
- **Network**: VPC with private subnets for all data stores. Public subnets only for load balancers. No direct internet access for service pods.
- **Kubernetes**: RBAC with least-privilege service accounts. Network policies restrict pod-to-pod communication. No privileged containers.
- **Container security**: Image scanning (Trivy) in CI pipeline. Base images pinned to specific digests. No root users in containers.
- **Dependency scanning**: Dependabot (GitHub) + Snyk for vulnerability alerts. Critical vulns addressed within 24 hours.
- **Secrets**: AWS Secrets Manager. No environment variables, no config files, no hardcoded secrets. Secrets rotated quarterly.

---

## 4. Marketplace Fraud

### 4.1 Fake Listings / Non-Shipment

**Attack**: Seller lists items they don't have, collects payment, never ships.

**Mitigations**:
- **Escrow**: Seller NEVER receives payment until buyer confirms delivery or auto-release timer expires
- **Tracking validation**: Seller must enter valid tracking number. Validated against carrier API (USPS, UPS, FedEx, DHL). Invalid tracking → order flagged.
- **New seller limits**: First 5 active listings until 3 successful completed sales
- **Seller deposit (optional)**: Sellers can deposit RV as collateral → "Verified Seller" badge + lower fees. Collateral slashed if fraud confirmed.
- **Repeat offenders**: Permanent ban. Wallet address + device fingerprint + IP blocklisted. KYC ID flagged across identity providers.

### 4.2 Counterfeit Goods

**Attack**: Seller lists counterfeit items (fake sneakers, knockoff electronics).

**Mitigations**:
- Image-based detection: ML model trained on known counterfeit patterns (Phase 4+)
- Category-specific authentication services for high-value items (sneakers, luxury goods)
- Buyer dispute category "Not as described" with dedicated review workflow
- Community reporting with flagging threshold (3 independent flags → manual review)
- Zero tolerance policy: confirmed counterfeit = permanent ban + KYC ID flag

### 4.3 Chargeback Fraud

**Attack**: Buyer pays with credit card, receives item, disputes charge with bank → double-dip (keeps item + gets money back).

**Mitigations**:
- Stripe Radar for fraud scoring on all card transactions
- High-risk transactions (new account + high value + new card): require additional verification
- Shipping confirmation + tracking evidence submitted to Stripe for chargeback disputes
- Repeat chargeback offenders: account suspended, future purchases require ACH or RV balance only
- Chargeback costs passed to buyer if dispute found fraudulent

### 4.4 Shipping Fraud

**Attack**: Buyer claims "not received" when item was delivered. Or seller ships empty box.

**Mitigations**:
- Carrier tracking API integration — system automatically checks delivery status
- Signature confirmation required for orders > $100
- Photo-on-delivery encouraged (seller can require)
- Weight verification: if declared weight is 2 lbs but carrier scanned weight is 0.1 lbs → flag
- Geographic anomaly: if tracking shows delivery to different state than buyer's address → flag

---

## 5. Economic Attack Vectors

### 5.1 Wash Trading

**Attack**: Create multiple accounts, list fake items, buy from yourself to farm rewards, inflate volume, or manipulate token metrics.

**Mitigations**:
- **KYC required for selling** (Tier 1 minimum) — expensive to create multiple verified identities
- **Device fingerprinting**: Browser hash + device ID. Shared device across accounts → flag.
- **IP correlation**: Multiple accounts from same IP / residential proxy → flag.
- **Shipping address verification**: Must ship to a real address. Can't ship to yourself under a different name (address matched against KYC).
- **Transaction pattern analysis**: Flag accounts that exclusively trade with each other. Graph analysis: detect circular fund flows.
- **Minimum account age**: 7 days before first sale allowed
- **No volume-based rewards**: Remove incentive. The 0.5% buyer cashback is too small to profit from wash trading after considering the 2.5% fee + 1% burn.
- **Wash trade detection ML**: Monitor for patterns — same device, rapid buy-sell cycles, same payment source, no real shipping

### 5.2 Sybil Attacks

**Attack**: Create thousands of accounts to exploit new-user bonuses, referral programs, or governance.

**Mitigations**:
- **Progressive KYC**: Free to browse. KYC required to transact. Expensive to Sybil.
- **Phone verification**: One account per phone number (SMS verification)
- **Referral rewards**: Paid only after referee completes first real purchase WITH shipping confirmation to a different address
- **Governance**: Requires Diamond-tier staking (365-day lock of 100+ RV). Sybil cost at $0.10/RV: $10/vote minimum + 1-year lock. At any meaningful governance threshold, prohibitively expensive.
- **Airdrop limits**: KYC-verified accounts only. Device fingerprint + IP dedup.

### 5.3 Token Price Manipulation

**Attack**: Whale accumulates RV, pumps price on DEX, sells at inflated price. Or: crash price to buy cheap, accumulate, wait for recovery.

**Mitigations**:
- **Protocol-Owned Liquidity**: 100M RV permanently locked in DEX pools. Provides liquidity floor that can't be removed.
- **TWAP oracle**: 30-minute time-weighted average. Attacker would need to sustain price manipulation for 30 minutes — costly against a liquid pool.
- **Circuit breaker**: >20% move in 1 hour → halt marketplace conversions. Prevents exploitation of extreme volatility.
- **Large trade alerts**: Any single trade > 1% of pool triggers risk team notification.
- **No leverage**: No margin trading, no synthetic exposure. Limits manipulation vectors.
- **Vesting schedules**: Team + advisor tokens locked. Can't dump.

### 5.4 Liquidity Drain

**Attack**: Large holder dumps all RV on DEX, crashing price and draining LP pool.

**Mitigations**:
- POL is locked permanently — baseline liquidity always exists
- 2% withdrawal burn makes large exits expensive (dumping 10M RV costs 200K RV)
- Team tokens vested (can't dump in bulk)
- Internal exchange uses treasury reserve, separate from DEX pools
- Slippage on large DEX sales is significant (natural market protection)

### 5.5 Front-Running Buybacks

**Attack**: Attacker monitors the buyback pool balance, buys RV just before scheduled buyback, sells after buyback drives up price.

**Mitigations**:
- Buyback execution is randomized within a 48-hour window (not predictable)
- Buyback is split into small tranches over 4 hours (not a single large order)
- TWAP-based execution reduces impact per tranche
- Buyback amounts are small relative to pool depth (by design: max 0.5% of pool per trade)

---

## 6. Risk Scoring Engine

### Real-Time Transaction Risk Score

Every transaction is scored 0–100 by the Risk & Fraud Service before it's approved.

```
Inputs:
  account_age_days              (older = safer)
  kyc_tier                      (higher = safer)
  transaction_count_lifetime    (more history = safer)
  device_trust_score            (known device = safer)
  ip_risk_score                 (residential = safer, VPN/proxy = riskier)
  order_value_usd               (higher = riskier)
  seller_rating                 (higher = safer)
  is_first_transaction          (true = riskier)
  velocity_last_hour            (more = riskier)
  device_shared_accounts        (any = riskier)

Outputs:
  0–30:   AUTO_APPROVE — proceed normally
  31–60:  ENHANCED_REVIEW — additional verification step (CAPTCHA, SMS OTP)
  61–80:  MANUAL_REVIEW — queued for human review before processing
  81–100: AUTO_BLOCK — transaction blocked, account flagged

Scoring model: initially rules-based, migrated to ML model after sufficient
training data (Phase 4+).
```

### Rules-Based Fraud Rules (Pre-ML)

```
RULE: New account + first transaction + order > $200 → score += 40
RULE: VPN/proxy IP detected → score += 20
RULE: Multiple failed payment attempts in 1 hour → score += 30
RULE: Device fingerprint seen on another account → score += 50
RULE: Shipping address different from KYC address on first transaction → score += 15
RULE: Buyer and seller share IP/device/payment method → score += 80 (wash trade)
RULE: Account created < 24 hours ago + transaction → score += 25
RULE: Order value > $500 with Tier 0 KYC → AUTO_BLOCK
```

---

## 7. Operational Security & Incident Response

### 7.1 Severity Levels

```
P0 — CRITICAL (Smart contract exploit, funds at risk, key compromise)
  Response time: 15 minutes
  Actions:
    1. PAUSE all contracts immediately (2-of-5 multi-sig)
    2. War room assembled within 15 minutes
    3. Assess scope: which contracts affected, how much at risk
    4. Public disclosure within 4 hours (Twitter, Discord, status page)
    5. Remediation within 24 hours
    6. Post-mortem within 1 week (public)
    7. If funds lost: treasury used to make users whole

P1 — HIGH (Platform outage, payment processing failure, data breach, hot wallet anomaly)
  Response time: 30 minutes
  Actions:
    1. On-call engineer responds
    2. Status page updated within 1 hour
    3. Root cause identified within 4 hours
    4. Resolution within 8 hours
    5. Post-mortem within 3 days (internal)

P2 — MEDIUM (Feature degradation, slow performance, elevated fraud rate)
  Response time: 4 hours (business hours)
  Actions:
    1. Ticket created and assigned
    2. Fix deployed within 24 hours

P3 — LOW (UI bugs, non-critical issues)
  Response time: Next sprint
```

### 7.2 Smart Contract Emergency Procedures

```
Step 1: DETECT
  - Forta Network agents detect anomalous contract behavior
  - OR: community/bounty hunter reports vulnerability
  - OR: monitoring alerts on unexpected token transfers, balance changes

Step 2: PAUSE
  - Multi-sig executes pause() on affected contracts (no timelock)
  - Requires 2-of-5 signers
  - Estimated time: 5–15 minutes (signers have mobile app alerts)

Step 3: ASSESS
  - Security team analyzes the vulnerability
  - Determine: exploited or pre-exploitation?
  - Scope: which funds are affected, how much?
  - Can it be mitigated without an upgrade?

Step 4: COMMUNICATE
  - Public announcement within 4 hours
  - Clear: what happened, what's affected, what we're doing
  - Twitter, Discord, status page, email to affected users

Step 5: FIX
  - For P0: emergency upgrade (48-hour timelock WAIVED with 4-of-5 multi-sig)
  - Deploy patched implementation via UUPS upgrade
  - Run full test suite + targeted audit of the fix

Step 6: COMPENSATE
  - If funds were lost: treasury funds used to make affected users whole
  - Documented and transparent

Step 7: POST-MORTEM
  - Public post-mortem within 1 week
  - Root cause, timeline, remediation, prevention measures
  - Update audit scope and testing strategy
```

### 7.3 On-Call Rotation

```
Coverage: 24/7/365
Team: 4-person rotation (1 week on, 3 weeks off)
Escalation path:
  Alert → On-call engineer (5 min)
    → Engineering lead (15 min)
    → CTO (30 min)
    → Full team (1 hour)

Tools:
  PagerDuty for alerting
  Slack #incidents channel for coordination
  Notion runbooks for common scenarios
  Status page: status.rvswaps.com
```

---

## 8. Bug Bounty Program

Hosted on **Immunefi** with clear scope and rules.

| Severity | Payout | Examples |
|---|---|---|
| Critical | $100,000–$250,000 | Fund theft, unlimited minting, escrow bypass, key extraction |
| High | $25,000–$100,000 | Access control bypass, significant loss risk, oracle manipulation |
| Medium | $5,000–$25,000 | DoS on critical functions, information disclosure, minor fund loss |
| Low | $1,000–$5,000 | Gas optimization, minor issues |

**Scope**:
- In scope: All deployed smart contracts, API endpoints, web application
- Out of scope: Third-party services (Stripe, MoonPay, Sumsub), testnet, documentation

**Rules**:
- No DoS attacks against production systems
- No social engineering of team members
- Report first, disclose later (90-day disclosure window)
- Duplicate reports: first reporter gets the bounty

---

## 9. Compliance & Legal Security

### 9.1 KYC/AML Tiers

```
Tier 0 — Browse Only
  Requirement: Email verification
  Limits: Browse free, buy up to $50/tx, $200/month
  No selling allowed

Tier 1 — Standard
  Requirement: Government ID + selfie (via Sumsub/Onfido)
  Limits: Buy/sell up to $5,000/tx, $20,000/month
  Unlocks: selling, all payment methods

Tier 2 — Unlimited
  Requirement: Tier 1 + proof of address + source of funds declaration
  Limits: Unlimited
  Unlocks: high-value transactions, increased withdrawal limits
```

KYC data stored with the third-party provider. We store ONLY:
- Verification status (NONE, PENDING, VERIFIED, REJECTED)
- Tier level (0, 1, 2)
- Hashed reference ID (to query provider if needed)

### 9.2 Token Classification Risk

**Risk**: RV classified as a security under the Howey test.

**Mitigation strategy**:
- Position RV as a **utility token** — genuine utility for buying goods on the marketplace
- **Never** promise returns or price appreciation in marketing materials
- Burns are a platform mechanic, not a profit distribution mechanism
- Staking rewards come from platform fees (payment for providing liquidity to the ecosystem), not from team efforts
- Engage securities counsel (Anderson Kill, Debevoise, or equivalent) before launch
- Legal opinion letter for each target jurisdiction
- Geographic restrictions: block jurisdictions where classification is unclear (start US + EU only)

### 9.3 Money Transmitter Licensing

Custodial wallets + fiat on/off ramps = money transmitter in most jurisdictions.

**Strategy**:
- **Phase 1**: Partner with a licensed provider (e.g., Circle, Zero Hash) who holds the MTL. They handle custody + compliance. We build the UX.
- **Phase 2**: As revenue grows, evaluate obtaining own licenses (FinCEN MSB registration + state-by-state MTLs)
- **Fallback**: If regulatory pressure increases, fall back to non-custodial only (users bring their own wallets). This hurts onboarding but eliminates MTL requirement.

### 9.4 Sanctions & Geographic Restrictions

- OFAC SDN list screening on all users at KYC (and periodic re-screening)
- IP-based geo-blocking for sanctioned countries
- Transaction monitoring for suspicious patterns (AML)
- Suspicious Activity Reports (SARs) filed when required

---

## 10. Monitoring & Alerting Strategy

### Dashboards (Grafana)

```
Dashboard 1: Platform Health
  - API latency (p50, p95, p99) per service
  - Error rates per service
  - Active users (real-time)
  - Queue depths (RabbitMQ)

Dashboard 2: Financial Health
  - Escrow deposits / releases / refunds (real-time)
  - Conversion Engine: rate, latency, failure rate
  - Treasury reserves: RV + USDC balances
  - Hot wallet balance vs expected

Dashboard 3: Blockchain Health
  - Token supply / burn rate (real-time from The Graph)
  - Staking TVL
  - DEX pool depth
  - Contract pause status
  - Gas prices

Dashboard 4: Fraud & Risk
  - Risk score distribution
  - Blocked transactions (count, reasons)
  - Dispute rate (should be <5%)
  - Chargeback rate (should be <1%)
  - Flagged accounts

Dashboard 5: Reconciliation
  - Off-chain ledger vs on-chain state
  - Discrepancy count (should be 0)
  - Last reconciliation timestamp
```

### Automated Alerts

| Alert | Condition | Severity | Channel |
|---|---|---|---|
| Contract paused | Any contract pause event | P0 | PagerDuty + Slack |
| Hot wallet anomaly | Balance deviates >10% from expected | P1 | PagerDuty |
| Oracle stale | >5 min since last update | P1 | PagerDuty |
| Circuit breaker triggered | Any trigger | P1 | PagerDuty + Slack |
| Reconciliation mismatch | Any discrepancy | P1 | PagerDuty |
| API error rate | >5% for >5 min | P1 | PagerDuty |
| Conversion failure rate | >2% | P1 | Slack |
| Dispute rate | >5% of transactions | P2 | Slack |
| Chargeback rate | >1% | P2 | Slack |
| Fraud score spike | Avg score >50 for >1 hour | P2 | Slack |
| Treasury low | RV reserve <4% of circ. | P2 | Slack |
| DEX liquidity low | Pool depth <$250K | P2 | Slack |

---

## 11. Security Audit Schedule

| Timing | Scope | Type |
|---|---|---|
| Pre-launch (Month -1) | RVToken, RVEscrow, RVStaking, RVBuybackBurn | Smart contract audit (2 firms) |
| Month 3 | Full platform | Penetration test |
| Ongoing | Contract events | Forta monitoring agents |
| Quarterly | Dependencies + infrastructure | Automated scan + manual review |
| Annually | Full system | Comprehensive security assessment |
| On upgrade | Modified contracts | Targeted audit of changes |
