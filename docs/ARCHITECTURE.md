# RVSwaps — Technical Architecture

---

## 1. Architecture Overview

RVSwaps uses a **hybrid architecture** — off-chain services for performance-critical marketplace operations (search, messaging, moderation), on-chain for financial settlement (escrow, burns, staking). A **Conversion Engine** sits between them, translating USD-denominated marketplace activity into on-chain RV flows without users ever seeing crypto.

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENT LAYER                                │
│                                                                     │
│   ┌──────────────┐    ┌───────────────┐    ┌──────────────────┐    │
│   │  Next.js Web │    │ React Native  │    │  Admin Dashboard │    │
│   │   (PWA/SSR)  │    │  iOS/Android  │    │   (Internal)     │    │
│   └──────┬───────┘    └───────┬───────┘    └────────┬─────────┘    │
│          └────────────────────┼─────────────────────┘              │
│                               │                                     │
│                    ┌──────────▼───────────┐                        │
│                    │   API Gateway        │                        │
│                    │   (Kong / AWS ALB)   │                        │
│                    │   Rate limit, auth,  │                        │
│                    │   WAF, routing       │                        │
│                    └──────────┬───────────┘                        │
└───────────────────────────────┼─────────────────────────────────────┘
                                │
┌───────────────────────────────┼─────────────────────────────────────┐
│                         SERVICE LAYER                                │
│                                                                      │
│  ┌────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │ Auth       │  │ Marketplace     │  │ Order Service           │  │
│  │ Service    │  │ Service         │  │                         │  │
│  │ (JWT, KYC, │  │ (listings,      │  │ (lifecycle management,  │  │
│  │  2FA,      │  │  search, offers,│  │  state machine,         │  │
│  │  OAuth)    │  │  categories)    │  │  shipping tracking)     │  │
│  └────────────┘  └─────────────────┘  └─────────────────────────┘  │
│                                                                      │
│  ┌────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │ Conversion │  │ Wallet          │  │ Payment                 │  │
│  │ Engine     │  │ Service         │  │ Service                 │  │
│  │            │  │                 │  │                         │  │
│  │ (USD↔RV,   │  │ (custodial HSM, │  │ (Stripe, escrow init,  │  │
│  │  TWAP,     │  │  external conn, │  │  settlement, auto-     │  │
│  │  slippage, │  │  balance cache, │  │  cashout, fiat rails)  │  │
│  │  circuit   │  │  signing)       │  │                         │  │
│  │  breakers) │  │                 │  │                         │  │
│  └────────────┘  └─────────────────┘  └─────────────────────────┘  │
│                                                                      │
│  ┌────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │ Risk &     │  │ Notification    │  │ Buyback                 │  │
│  │ Fraud      │  │ Service         │  │ Engine                  │  │
│  │ Service    │  │                 │  │                         │  │
│  │ (ML scoring│  │ (push, email,   │  │ (periodic buyback from  │  │
│  │  device FP,│  │  in-app, SMS)   │  │  DEX, burn execution,  │  │
│  │  velocity) │  │                 │  │  treasury ops)          │  │
│  └────────────┘  └─────────────────┘  └─────────────────────────┘  │
│                                                                      │
│  ┌────────────┐  ┌─────────────────┐                               │
│  │ Moderation │  │ Analytics       │                               │
│  │ Service    │  │ Service         │                               │
│  │ (image ML, │  │ (metrics, burn  │                               │
│  │  text scan,│  │  tracking,      │                               │
│  │  manual Q) │  │  dashboards)    │                               │
│  └────────────┘  └─────────────────┘                               │
│                                                                      │
│             ┌──────────────────────────────┐                        │
│             │  Message Bus (RabbitMQ/SQS)  │                        │
│             │  Event-driven state machine  │                        │
│             └──────────────────────────────┘                        │
└───────────────────────────────┼─────────────────────────────────────┘
                                │
┌───────────────────────────────┼─────────────────────────────────────┐
│                          DATA LAYER                                  │
│                                                                      │
│   ┌──────────┐  ┌─────────────────┐  ┌────────────────────────┐    │
│   │PostgreSQL│  │ Redis           │  │ Elasticsearch          │    │
│   │          │  │                 │  │                        │    │
│   │ users,   │  │ sessions, cache,│  │ listing search,        │    │
│   │ listings,│  │ rate limits,    │  │ full-text, facets,     │    │
│   │ orders,  │  │ TWAP cache,     │  │ geo, price range       │    │
│   │ wallets, │  │ pub/sub,        │  │                        │    │
│   │ ledger   │  │ leaderboards    │  │                        │    │
│   └──────────┘  └─────────────────┘  └────────────────────────┘    │
│                                                                      │
│   ┌──────────┐  ┌─────────────────┐                                │
│   │ S3 + CDN │  │ Event Store     │                                │
│   │ (images, │  │ (chain indexer, │                                │
│   │  uploads)│  │  The Graph,     │                                │
│   │          │  │  reconciliation)│                                │
│   └──────────┘  └─────────────────┘                                │
└───────────────────────────────┼─────────────────────────────────────┘
                                │
┌───────────────────────────────┼─────────────────────────────────────┐
│                     BLOCKCHAIN LAYER (Polygon PoS)                   │
│                                                                      │
│   ┌────────────────────────────────────────────────────────────┐    │
│   │                                                            │    │
│   │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │    │
│   │  │ RVToken     │ │ RVEscrow    │ │ RVStaking           │ │    │
│   │  │ (ERC-20     │ │ (payment    │ │ (tiered lockups,    │ │    │
│   │  │  + burn     │ │  escrow,    │ │  reward distrib.,   │ │    │
│   │  │  + permit)  │ │  disputes)  │ │  fee routing)       │ │    │
│   │  └─────────────┘ └─────────────┘ └─────────────────────┘ │    │
│   │                                                            │    │
│   │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │    │
│   │  │ RVBuyback   │ │ Chainlink   │ │ Meta-tx Relayer     │ │    │
│   │  │ Burn        │ │ TWAP Oracle │ │ (EIP-2771,          │ │    │
│   │  │ (treasury   │ │ (RV/USD     │ │  gas sponsorship)   │ │    │
│   │  │  buyback)   │ │  price feed)│ │                     │ │    │
│   │  └─────────────┘ └─────────────┘ └─────────────────────┘ │    │
│   │                                                            │    │
│   │  ┌───────────────────────────────────────────────────────┐│    │
│   │  │  The Graph Subgraph (indexer for all contract events) ││    │
│   │  └───────────────────────────────────────────────────────┘│    │
│   └────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Blockchain Selection: Why Polygon PoS

| Criteria | Ethereum | Polygon PoS | Solana | Arbitrum | Base |
|---|---|---|---|---|---|
| Transaction fee | $2–50 | $0.001–0.01 | $0.0005 | $0.01–0.10 | $0.001–0.05 |
| Block time | 12s | 2s | 0.4s | 0.25s | 2s |
| EVM compatible | Yes | Yes | No (Rust) | Yes | Yes |
| Ecosystem maturity | Highest | High | Medium | Growing | Growing |
| Fiat on-ramp support | Excellent | Excellent | Good | Limited | Good |
| Developer tooling | Best | Best (EVM) | Growing | Best (EVM) | Best (EVM) |

**Decision: Polygon PoS as primary chain.**

- Sub-cent fees make micro-transactions viable (listing boosts, small purchases)
- 2-second finality — fast enough for marketplace UX
- Full EVM = Solidity, Hardhat, Foundry, ethers.js — largest developer talent pool
- Mature fiat on-ramp ecosystem (MoonPay, Transak all support Polygon)
- The Graph supports Polygon natively
- Future expansion to Arbitrum or Base in Phase 6

---

## 3. Frontend Architecture

### Web — Next.js 14+ (App Router)

```
next-app/
├── app/
│   ├── (auth)/
│   │   ├── login/           ← email/password, OAuth, wallet connect
│   │   ├── register/
│   │   └── verify/
│   ├── (marketplace)/
│   │   ├── browse/          ← category grid, daily deals, trending
│   │   ├── listing/[id]/    ← listing detail, buy now, make offer
│   │   ├── sell/            ← create listing (USD price input)
│   │   ├── search/          ← full-text search with facets
│   │   └── deals/           ← curated "Daily Deals", "Rare Finds"
│   ├── (orders)/
│   │   ├── [id]/            ← order detail, tracking, dispute
│   │   └── history/         ← past orders
│   ├── (wallet)/
│   │   ├── dashboard/       ← RV balance (shown as USD equiv + RV)
│   │   ├── stake/           ← staking tiers, lock, unlock
│   │   └── settings/        ← payout preference (USD default vs keep RV)
│   ├── (profile)/
│   │   ├── [username]/      ← public seller profile, ratings
│   │   └── settings/        ← KYC, 2FA, notification prefs
│   └── api/                 ← BFF routes (server components)
├── components/
│   ├── marketplace/         ← listing card, price display ($USD), filters
│   ├── checkout/            ← payment form (card/ACH or RV), fee breakdown
│   ├── wallet/              ← balance widget, staking UI
│   └── common/              ← nav, modals, buttons, error boundaries
├── hooks/
│   ├── useCheckout.ts       ← handles USD→RV conversion silently
│   ├── useListings.ts
│   └── useWallet.ts
└── lib/
    ├── api.ts               ← API client
    ├── conversion.ts        ← client-side price display helpers
    └── auth.ts
```

**Key decisions:**
- **SSR for marketplace pages** — SEO for listings, fast initial load
- **Client-side for wallet/staking** — requires authenticated state
- **All prices displayed in USD** — RV amount shown as secondary info only
- **PWA** — mobile web users get near-native experience
- Images via CloudFront CDN with on-the-fly resizing (imgproxy)

### Mobile — React Native

- Shared TypeScript business logic with web
- Camera integration for listing photos
- Biometric auth for high-value actions
- Push notifications (Firebase)
- Deep linking for sharing listings

---

## 4. Backend Service Architecture

### Service Decomposition

Each service owns its data. Communication is REST (synchronous, request-response) or message queue (asynchronous, events). Services are stateless — all state is in Postgres/Redis.

#### Auth Service

- Registration: email+password, Google/Apple OAuth, wallet-based (Sign-In with Ethereum)
- JWT access tokens (15-min expiry) + httpOnly refresh tokens (7-day, rotated)
- KYC integration (Sumsub or Onfido): Tier 0 (browse) → Tier 1 (transact) → Tier 2 (unlimited)
- 2FA: TOTP (Google Authenticator) or SMS fallback
- Rate limiting: 5 login attempts / 15 min / IP, then CAPTCHA
- Device fingerprinting stored per session for risk scoring

#### Marketplace Service

- CRUD for listings — title, description, **price_usd** (primary), category, condition, images
- Offer/counter-offer negotiation
- Category taxonomy with faceted search
- Listing moderation queue: ML image screening (nudity, counterfeit, prohibited items) + manual review
- Seller verification status and listing limits
- "Daily Deals" and "Rare Finds" curation algorithms

#### Order Service

- Order lifecycle state machine:
  ```
  CREATED → PAYMENT_PENDING → PAID → SHIPPED → DELIVERED → COMPLETED
                                                    ↓
                                               DISPUTED → RESOLVED
  ```
- Shipping tracking: carrier detection, tracking number validation via carrier APIs
- Auto-confirm delivery after 14 days if buyer doesn't act
- Dispute management: evidence submission, admin review, resolution

#### Conversion Engine

*(Full details in [CONVERSION_ENGINE.md](CONVERSION_ENGINE.md))*

- USD→RV conversion at checkout using TWAP oracle
- RV→USD conversion for auto-cashout using TWAP + slippage protection
- Circuit breakers: halt conversions if RV price moves >20% in 1 hour
- Treasury reserve management for instant fills
- Reconciliation between off-chain ledger and on-chain state

#### Payment Service

- Fiat processing: Stripe for cards/ACH, Apple Pay, Google Pay
- On-ramp: MoonPay/Transak for direct fiat→RV
- Escrow initiation: calls Conversion Engine for USD→RV, then initiates on-chain escrow
- Settlement: on escrow release, triggers auto-cashout if seller preference is USD
- Idempotent payment flows with unique payment intent IDs

#### Wallet Service

- **Custodial wallets** (default): HD wallet derivation from master seed in AWS CloudHSM
- **External wallets** (optional): MetaMask, WalletConnect via wagmi/viem
- Balance queries: on-chain + cached in Redis (30s TTL)
- Transaction signing: custodial via HSM; external via client-side
- Withdrawal processing with rate limits and security holds
- Hot/cold wallet split: 5% hot, 95% cold

#### Risk & Fraud Service

- Real-time risk scoring on every transaction
- Device fingerprinting (browser hash, device ID, IP geolocation)
- Velocity checks: max transactions/hour, max new listings/day
- Multi-account detection: shared device/IP/payment method correlation
- Wash trade detection: circular fund flows, buyer-seller collusion patterns
- Chargeback risk scoring (for fiat payments)
- ML model inputs: account age, KYC tier, transaction history, device trust score
- Actions: allow / flag for review / block / freeze account

#### Notification Service

- Email (SendGrid/SES): transactional + marketing
- Push (Firebase Cloud Messaging): order updates, offers, price alerts
- In-app notification feed
- Event-driven: listens to message bus

#### Moderation Service

- Image scanning: AWS Rekognition or Google Cloud Vision
- Text filtering: profanity, scam keywords, contact info extraction
- Prohibited item detection (weapons, drugs, counterfeit)
- Manual review queue with admin tooling
- Seller trust scoring affects review priority

#### Buyback Engine

- Accumulates buyback pool from fee routing (10% of all platform fees)
- Executes periodic market buys of RV from DEX (QuickSwap RV/USDC pair)
- Uses TWAP-based execution to minimize price impact (spread over hours, not single large order)
- Burns purchased RV via `RVBuybackBurn` contract
- Publishes burn events to analytics dashboard

#### Analytics Service

- Transaction volume, GMV, burn metrics, staking TVL
- User engagement: DAU/MAU, retention cohorts, funnel analysis
- Marketplace health: time-to-sale, listing-to-purchase ratio
- Token metrics: circulating supply, burn rate, velocity
- Real-time dashboards (Grafana) + weekly automated reports

---

## 5. Message Bus Events

All inter-service communication for state changes flows through RabbitMQ (or SQS). Each event has a unique ID for idempotency.

```
Event                       → Consumers
────────────────────────────────────────────────────────────────

# User lifecycle
user.registered             → Notification (welcome email)
                            → Wallet (create custodial wallet)
                            → Analytics (track signup source)

user.kyc.completed          → Marketplace (unlock selling)
                            → Risk (update trust score)

# Listing lifecycle
listing.created             → Search (index in Elasticsearch)
                            → Moderation (content review queue)
                            → Analytics (listing metrics)

listing.updated             → Search (re-index)
listing.removed             → Search (de-index)

listing.flagged             → Moderation (priority review)
                            → Risk (flag seller)

listing.approved            → Notification (seller: "listing live")
listing.rejected            → Notification (seller: "listing rejected, reason: X")

# Order lifecycle
order.created               → Conversion Engine (lock USD→RV rate)
                            → Payment (initiate payment intent)

order.payment.pending       → Notification (buyer: "complete payment")

order.paid                  → Wallet (initiate on-chain escrow deposit)
                            → Notification (seller: "you have a sale!")
                            → Analytics (record transaction)
                            → Marketplace (mark listing as sold)

order.shipped               → Notification (buyer: "item shipped, tracking: X")
                            → Order (start auto-confirm countdown)

order.delivered             → Notification (buyer: "confirm receipt")
                            → Order (start 14-day auto-release timer)

order.completed             → Payment (release escrow → auto-cashout if USD pref)
                            → Notification (both: "leave a rating")
                            → Analytics (completed transaction)

order.disputed              → Notification (both: "dispute opened")
                            → Risk (update both users' scores)

order.dispute.resolved      → Payment (execute resolution split)
                            → Notification (both: "dispute resolved")

# Financial events
escrow.deposited            → Order (update status → PAID)
                            → Analytics (escrow volume)

escrow.released             → Order (update status → COMPLETED)
                            → Buyback Engine (route fee portion to buyback pool)

escrow.refunded             → Order (update status → REFUNDED)
                            → Notification (buyer: "refund processed")

token.burned                → Analytics (update burn dashboard)

buyback.executed            → Analytics (record buyback amount + burn)

cashout.completed           → Notification (seller: "USD payout sent")
                            → Analytics (cashout volume)

# Staking events
stake.created               → Analytics (TVL update)
stake.withdrawn             → Analytics (TVL update)
rewards.claimed             → Analytics (reward distribution)
```

---

## 6. Database Schema (PostgreSQL)

### Core Tables

```sql
-- Users
CREATE TABLE users (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email             VARCHAR(255) UNIQUE NOT NULL,
    username          VARCHAR(50) UNIQUE NOT NULL,
    password_hash     VARCHAR(255),
    wallet_address    VARCHAR(42),           -- custodial or connected external
    kyc_status        VARCHAR(20) DEFAULT 'NONE',   -- NONE, PENDING, VERIFIED, REJECTED
    kyc_tier          INT DEFAULT 0,                 -- 0=browse, 1=transact, 2=unlimited
    payout_preference VARCHAR(10) DEFAULT 'USD',     -- USD or RV
    seller_tier       VARCHAR(20) DEFAULT 'NEW',     -- NEW, BRONZE, SILVER, GOLD
    trust_score       DECIMAL(5,2) DEFAULT 50.00,    -- 0-100, computed by Risk service
    rating_avg        DECIMAL(3,2) DEFAULT 0,
    rating_count      INT DEFAULT 0,
    device_fingerprint VARCHAR(64),
    created_at        TIMESTAMPTZ DEFAULT NOW(),
    updated_at        TIMESTAMPTZ DEFAULT NOW()
);

-- Listings (USD-denominated)
CREATE TABLE listings (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id         UUID REFERENCES users(id) NOT NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    price_usd         DECIMAL(10,2) NOT NULL,         -- PRIMARY: price in USD
    category          VARCHAR(100) NOT NULL,
    subcategory       VARCHAR(100),
    condition         VARCHAR(20),                     -- NEW, LIKE_NEW, GOOD, FAIR, POOR
    status            VARCHAR(20) DEFAULT 'PENDING_REVIEW',
                      -- PENDING_REVIEW, ACTIVE, SOLD, REMOVED, EXPIRED, REJECTED
    images            JSONB NOT NULL,                  -- [{url, thumbnail_url, order}]
    shipping_options  JSONB,                           -- {weight_oz, methods: ["usps_priority"]}
    location_city     VARCHAR(100),
    location_state    VARCHAR(50),
    is_promoted       BOOLEAN DEFAULT FALSE,
    promoted_until    TIMESTAMPTZ,
    view_count        INT DEFAULT 0,
    offer_count       INT DEFAULT 0,
    source            VARCHAR(20) DEFAULT 'USER',      -- USER, PLATFORM, ANCHOR_SELLER
    created_at        TIMESTAMPTZ DEFAULT NOW(),
    expires_at        TIMESTAMPTZ DEFAULT NOW() + INTERVAL '90 days'
);

-- Orders (USD-denominated with RV conversion snapshot)
CREATE TABLE orders (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id        UUID REFERENCES listings(id) NOT NULL,
    buyer_id          UUID REFERENCES users(id) NOT NULL,
    seller_id         UUID REFERENCES users(id) NOT NULL,

    -- USD amounts (source of truth)
    price_usd         DECIMAL(10,2) NOT NULL,
    buyer_fee_usd     DECIMAL(10,2) NOT NULL,          -- 2.5% of price
    total_charged_usd DECIMAL(10,2) NOT NULL,          -- price + buyer_fee

    -- RV conversion snapshot (at time of payment)
    rv_rate           DECIMAL(18,8),                   -- TWAP rate used: 1 RV = X USD
    amount_rv         DECIMAL(18,8),                   -- total RV for escrow
    burn_amount_rv    DECIMAL(18,8),                   -- 1% burn
    fee_amount_rv     DECIMAL(18,8),                   -- platform fee in RV
    seller_amount_rv  DECIMAL(18,8),                   -- net to seller

    -- Settlement
    payment_method    VARCHAR(20),                     -- CARD, ACH, APPLE_PAY, RV_BALANCE
    payment_intent_id VARCHAR(100) UNIQUE,             -- Stripe or internal idempotency key
    escrow_tx_hash    VARCHAR(66),                     -- on-chain deposit tx
    release_tx_hash   VARCHAR(66),                     -- on-chain release tx
    cashout_status    VARCHAR(20),                     -- PENDING, COMPLETED, FAILED, SKIPPED

    -- Lifecycle
    status            VARCHAR(20) DEFAULT 'CREATED',
                      -- CREATED, PAYMENT_PENDING, PAID, SHIPPED, DELIVERED,
                      -- COMPLETED, DISPUTED, RESOLVED, CANCELLED, REFUNDED
    shipped_at        TIMESTAMPTZ,
    delivered_at      TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ,
    tracking_number   VARCHAR(100),
    tracking_carrier  VARCHAR(50),
    auto_release_at   TIMESTAMPTZ,                     -- 14 days after SHIPPED

    created_at        TIMESTAMPTZ DEFAULT NOW(),
    updated_at        TIMESTAMPTZ DEFAULT NOW()
);

-- Offers (negotiation)
CREATE TABLE offers (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id        UUID REFERENCES listings(id) NOT NULL,
    buyer_id          UUID REFERENCES users(id) NOT NULL,
    amount_usd        DECIMAL(10,2) NOT NULL,          -- offer in USD
    status            VARCHAR(20) DEFAULT 'PENDING',   -- PENDING, ACCEPTED, REJECTED, EXPIRED, COUNTERED
    counter_amount_usd DECIMAL(10,2),                  -- seller's counter
    message           TEXT,
    created_at        TIMESTAMPTZ DEFAULT NOW(),
    expires_at        TIMESTAMPTZ DEFAULT NOW() + INTERVAL '48 hours'
);

-- Ratings
CREATE TABLE ratings (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id          UUID REFERENCES orders(id) UNIQUE NOT NULL,
    reviewer_id       UUID REFERENCES users(id) NOT NULL,
    reviewee_id       UUID REFERENCES users(id) NOT NULL,
    score             INT CHECK (score BETWEEN 1 AND 5) NOT NULL,
    comment           TEXT,
    created_at        TIMESTAMPTZ DEFAULT NOW()
);

-- Disputes
CREATE TABLE disputes (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id          UUID REFERENCES orders(id) UNIQUE NOT NULL,
    opened_by         UUID REFERENCES users(id) NOT NULL,
    reason            VARCHAR(50) NOT NULL,            -- NOT_RECEIVED, NOT_AS_DESCRIBED, DAMAGED, OTHER
    description       TEXT NOT NULL,
    evidence          JSONB,                           -- [{type: "image"|"text", url, description}]
    status            VARCHAR(20) DEFAULT 'OPEN',      -- OPEN, UNDER_REVIEW, RESOLVED
    resolution_type   VARCHAR(20),                     -- FULL_REFUND, PARTIAL_REFUND, RELEASED_TO_SELLER, SPLIT
    resolution_note   TEXT,
    buyer_percent     INT,                             -- 0-100 split
    resolved_by       UUID,                            -- admin user
    created_at        TIMESTAMPTZ DEFAULT NOW(),
    resolved_at       TIMESTAMPTZ
);

-- Internal ledger (reconciliation with on-chain)
CREATE TABLE ledger_entries (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id          UUID REFERENCES orders(id),
    entry_type        VARCHAR(30) NOT NULL,            -- ESCROW_DEPOSIT, ESCROW_RELEASE, BURN,
                                                       -- FEE_COLLECTED, CASHOUT, BUYBACK
    amount_rv         DECIMAL(18,8) NOT NULL,
    amount_usd        DECIMAL(10,2),
    tx_hash           VARCHAR(66),                     -- on-chain tx hash
    status            VARCHAR(20) DEFAULT 'PENDING',   -- PENDING, CONFIRMED, FAILED
    created_at        TIMESTAMPTZ DEFAULT NOW()
);

-- Buyback tracking
CREATE TABLE buyback_executions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pool_balance_usd  DECIMAL(10,2) NOT NULL,          -- pool balance before buyback
    rv_purchased      DECIMAL(18,8) NOT NULL,
    rv_burned         DECIMAL(18,8) NOT NULL,
    avg_price_usd     DECIMAL(18,8) NOT NULL,          -- avg price paid per RV
    tx_hashes         JSONB NOT NULL,                  -- array of DEX trade tx hashes
    burn_tx_hash      VARCHAR(66),
    executed_at       TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_listings_seller ON listings(seller_id);
CREATE INDEX idx_listings_category ON listings(category);
CREATE INDEX idx_listings_status ON listings(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_listings_price ON listings(price_usd);
CREATE INDEX idx_listings_created ON listings(created_at DESC);
CREATE INDEX idx_listings_source ON listings(source);

CREATE INDEX idx_orders_buyer ON orders(buyer_id);
CREATE INDEX idx_orders_seller ON orders(seller_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_payment_intent ON orders(payment_intent_id);

CREATE INDEX idx_offers_listing ON offers(listing_id);
CREATE INDEX idx_offers_buyer ON offers(buyer_id);
CREATE INDEX idx_offers_status ON offers(status) WHERE status = 'PENDING';

CREATE INDEX idx_ledger_order ON ledger_entries(order_id);
CREATE INDEX idx_ledger_type ON ledger_entries(entry_type);
CREATE INDEX idx_ledger_status ON ledger_entries(status) WHERE status = 'PENDING';
```

### Elasticsearch Mapping

```json
{
  "mappings": {
    "properties": {
      "id":              { "type": "keyword" },
      "title":           { "type": "text", "analyzer": "standard",
                           "fields": { "keyword": { "type": "keyword" } } },
      "description":     { "type": "text", "analyzer": "standard" },
      "category":        { "type": "keyword" },
      "subcategory":     { "type": "keyword" },
      "condition":       { "type": "keyword" },
      "price_usd":       { "type": "float" },
      "seller_id":       { "type": "keyword" },
      "seller_rating":   { "type": "float" },
      "seller_tier":     { "type": "keyword" },
      "location": {
        "properties": {
          "city":        { "type": "keyword" },
          "state":       { "type": "keyword" },
          "geo":         { "type": "geo_point" }
        }
      },
      "images":          { "type": "keyword", "index": false },
      "is_promoted":     { "type": "boolean" },
      "source":          { "type": "keyword" },
      "status":          { "type": "keyword" },
      "created_at":      { "type": "date" },
      "view_count":      { "type": "integer" },
      "offer_count":     { "type": "integer" }
    }
  }
}
```

Sync: Marketplace Service emits `listing.created`, `listing.updated`, `listing.removed` → Search indexer consumer updates Elasticsearch.

---

## 7. API Design (REST)

All endpoints return JSON. Pagination uses cursor-based pagination. All monetary values in USD unless noted.

```
AUTH
  POST   /api/auth/register              ← create account
  POST   /api/auth/login                 ← returns JWT + refresh token
  POST   /api/auth/refresh               ← rotate refresh token
  POST   /api/auth/verify-email          ← email verification
  POST   /api/auth/2fa/setup             ← TOTP setup
  POST   /api/auth/2fa/verify            ← verify 2FA code
  POST   /api/auth/forgot-password       ← password reset flow

LISTINGS
  GET    /api/listings                   ← browse/search (paginated, faceted)
  GET    /api/listings/:id               ← single listing (price_usd + RV estimate)
  POST   /api/listings                   ← create listing (price_usd required)
  PUT    /api/listings/:id               ← update listing
  DELETE /api/listings/:id               ← remove listing
  POST   /api/listings/:id/report        ← flag listing
  POST   /api/listings/:id/promote       ← pay to boost

ORDERS
  POST   /api/orders                     ← initiate purchase (returns payment intent)
  GET    /api/orders                     ← user's orders (buyer + seller views)
  GET    /api/orders/:id                 ← order detail with RV conversion snapshot
  POST   /api/orders/:id/pay             ← complete payment (card, ACH, or RV balance)
  POST   /api/orders/:id/ship            ← seller enters tracking
  POST   /api/orders/:id/confirm         ← buyer confirms receipt
  POST   /api/orders/:id/dispute         ← open dispute with evidence

OFFERS
  POST   /api/listings/:id/offers        ← make offer (amount_usd)
  PUT    /api/offers/:id/accept          ← accept offer → creates order
  PUT    /api/offers/:id/reject          ← reject offer
  POST   /api/offers/:id/counter         ← counter offer (new amount_usd)

WALLET
  GET    /api/wallet/balance             ← RV balance + USD equivalent
  GET    /api/wallet/transactions        ← transaction history
  POST   /api/wallet/withdraw            ← withdraw RV to external wallet
  GET    /api/wallet/address             ← deposit address (for receiving RV)
  PUT    /api/wallet/payout-preference   ← set USD (default) or RV

STAKING
  GET    /api/staking/tiers              ← available tiers + current APY
  GET    /api/staking/positions          ← user's stake positions
  POST   /api/staking/stake              ← stake RV (amount + tier)
  POST   /api/staking/unstake            ← unstake (position ID)
  POST   /api/staking/claim              ← claim rewards (position ID)

CONVERSION
  GET    /api/conversion/rate            ← current TWAP rate (RV/USD)
  GET    /api/conversion/quote           ← quote for a specific USD amount → RV

USER
  GET    /api/users/:username            ← public profile + ratings
  PUT    /api/users/me                   ← update profile
  GET    /api/users/me/ratings           ← received ratings
  POST   /api/users/me/kyc              ← submit KYC documents

ADMIN (internal)
  GET    /api/admin/disputes             ← pending disputes
  POST   /api/admin/disputes/:id/resolve ← resolve dispute (buyer_percent)
  GET    /api/admin/moderation           ← review queue
  POST   /api/admin/moderation/:id/approve ← approve listing
  POST   /api/admin/moderation/:id/reject  ← reject listing
  GET    /api/admin/metrics              ← platform health dashboard
  POST   /api/admin/buyback/execute      ← trigger manual buyback
```

---

## 8. Infrastructure (AWS)

```
┌──────────────────────────────────────────────────────┐
│  Route 53 (DNS)                                       │
│       │                                               │
│  CloudFront (CDN) ← S3 (listing images)               │
│       │                                               │
│  WAF (rate limiting, IP blocking, OWASP rules)        │
│       │                                               │
│  ALB (Application Load Balancer)                      │
│       │                                               │
│  EKS (Kubernetes)                                     │
│  ├── api-gateway          (2–4 pods)                  │
│  ├── auth-service         (2–4 pods)                  │
│  ├── marketplace-service  (4–8 pods)                  │
│  ├── order-service        (2–4 pods)                  │
│  ├── conversion-engine    (2–3 pods)                  │
│  ├── payment-service      (2–4 pods)                  │
│  ├── wallet-service       (2–3 pods, HSM-connected)   │
│  ├── risk-fraud-service   (2–3 pods)                  │
│  ├── notification-service (2–3 pods)                  │
│  ├── moderation-service   (1–2 pods)                  │
│  ├── buyback-engine       (1 pod, cron-scheduled)     │
│  ├── search-indexer       (1–2 pods)                  │
│  └── analytics-service    (1–2 pods)                  │
│                                                       │
│  RDS PostgreSQL (Multi-AZ, read replicas)             │
│  ElastiCache Redis (cluster mode)                     │
│  OpenSearch Service (managed Elasticsearch)           │
│  Amazon MQ (RabbitMQ managed)                         │
│  S3 (image storage + CDN origin)                      │
│  CloudHSM (wallet key management, FIPS 140-2 L3)     │
│  KMS (general encryption keys)                        │
│  Secrets Manager (API keys, DB credentials)           │
│  CloudWatch (logs, metrics, alerting)                 │
│  X-Ray (distributed tracing)                          │
└──────────────────────────────────────────────────────┘
```

### Blockchain Infrastructure

- **RPC providers**: Alchemy (primary) + QuickNode (failover) — both Polygon
- **Indexer**: The Graph subgraph for token transfers, escrow events, staking events, burn events
- **Event listener**: WebSocket subscription to contract events, with polling fallback
- **Meta-transaction relayer**: OpenZeppelin Defender Relayer — sponsors gas for custodial wallet transactions
- **Oracle**: Chainlink RV/USD price feed (or custom TWAP aggregator from DEX pair)

### Monitoring & Observability

- **Metrics**: Prometheus + Grafana dashboards
- **Logging**: Structured JSON → CloudWatch Logs (or ELK)
- **Tracing**: OpenTelemetry + AWS X-Ray
- **Alerting**: PagerDuty for P0/P1; Slack for P2/P3
- **Blockchain dashboards**: Custom Grafana panels for supply, burn rate, staking TVL, escrow volume
- **Reconciliation alerts**: automated check every 5 minutes — off-chain ledger vs on-chain state. Any discrepancy triggers P1 alert.

---

## 9. Key Data Flows

### Purchase Flow (Crypto-Invisible, Happy Path)

```
1. Buyer browses listings — all prices shown in USD ("$45.00")
2. Buyer clicks "Buy Now" on a $45 listing
3. Frontend calls POST /api/orders { listing_id }
4. Order Service creates order record, calculates buyer fee (2.5% = $1.13)
5. Conversion Engine fetches TWAP rate (e.g., 1 RV = $0.10)
6. Order Service locks conversion rate, computes:
     total_charged_usd = $46.13
     amount_rv = 461.3 RV
     burn = 4.5 RV (1% of item price in RV)
     fee = 11.28 RV (2.5% buyer fee)
     seller_receives = 445.52 RV
7. Buyer sees checkout: "$46.13 total (includes $1.13 platform fee)"
   Payment options: Credit Card | Apple Pay | ACH | Pay with RV Balance
8. Buyer pays with credit card ($46.13)
9. Payment Service charges Stripe, receives confirmation
10. Conversion Engine converts $46.13 to 461.3 RV via:
     - Platform treasury reserve (instant, if available)
     - or DEX market buy (RV/USDC pool)
11. Wallet Service signs on-chain escrow deposit (meta-transaction, no gas for user)
12. On-chain: RVEscrow.deposit(orderId, seller, 461.3 RV)
13. Event listener detects EscrowDeposited event
14. Order Service updates status → PAID
15. Notification: seller gets "You made a sale! Ship within 5 days."
16. Seller ships, enters tracking number
17. Order Service validates tracking via carrier API
18. Buyer receives item, clicks "Confirm Delivery"
    (or auto-confirmed after 14 days)
19. Payment Service calls RVEscrow.release(orderId)
20. On-chain: 4.5 RV burned, 11.28 RV to fee recipient, 445.52 RV to seller
21. If seller payout_preference = 'USD' (default):
     - Conversion Engine converts 445.52 RV → ~$44.55 USD
     - Payment Service initiates ACH/Stripe payout to seller
22. If seller payout_preference = 'RV':
     - RV stays in seller's custodial wallet
23. Order status → COMPLETED
24. Both parties prompted to leave ratings
```

### Dispute Flow

```
1. Buyer opens dispute within 48 hours of delivery
   POST /api/orders/:id/dispute { reason, description, evidence[] }
2. Order Service emits order.disputed
3. Notification: both parties notified
4. Both submit additional evidence (photos, screenshots, messages)
5. Admin reviews in dashboard, decides:
   a) 100% buyer  → full refund (seller gets nothing from this order)
   b) 100% seller → released to seller (buyer claim rejected)
   c) Split       → partial refund, partial release
6. Admin calls POST /api/admin/disputes/:id/resolve { buyer_percent: 70 }
7. Payment Service calls RVEscrow.resolveDispute(orderId, 70)
8. On-chain: burn still happens, fee still taken, remainder split 70/30
9. Auto-cashout applies to each party's portion based on their preference
10. Dispute status → RESOLVED
```

---

## 10. Critical Design Decisions

### Custodial-First Wallets

**Decision: Custodial by default, non-custodial optional.**

Most users coming from eBay/Mercari don't have MetaMask. Requiring wallet setup kills onboarding.

- **Default**: Platform creates custodial wallet (HD-derived, keys in CloudHSM). User sees "RV Balance: $12.50 (125 RV)" without understanding blockchain.
- **Optional**: Power users connect external wallets.
- **Regulatory**: Custodial = money transmitter. We partner with a licensed provider (see [SECURITY.md](SECURITY.md) for compliance strategy).

### USD-First Pricing

**All items priced and stored in USD.** RV is the payment rail, not the unit of account.

This solves the velocity paradox: if RV appreciates, users would hoard it instead of spending. By pricing in USD, the marketplace functions identically regardless of RV price. A $45 item is always $45 — the RV amount adjusts at checkout.

### Gas Abstraction

Users never hold or spend MATIC. The platform sponsors gas via meta-transactions (EIP-2771) through OpenZeppelin Defender Relayer. Gas cost (~$0.005/tx) is absorbed into the platform fee and is negligible.

### Off-chain/On-chain Reconciliation

The internal ledger (`ledger_entries` table) is the off-chain shadow of on-chain state. A reconciliation job runs every 5 minutes:

1. Query The Graph for all recent escrow events
2. Compare against `ledger_entries` with matching `tx_hash`
3. Flag any discrepancies as P1 alerts
4. Dashboard shows reconciliation health: "last 24h: 1,247 entries, 0 discrepancies"

This is non-negotiable for a financial system. The off-chain DB is the fast path; the chain is the source of truth.
