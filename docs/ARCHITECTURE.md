# RVSwaps — Technical Architecture

---

## 1. Architecture Overview

RVSwaps uses a **hybrid architecture** — off-chain for performance-critical marketplace operations, on-chain for financial settlement and token mechanics. This is the only viable approach for a marketplace handling high-frequency listing updates, search queries, and messaging while still guaranteeing trustless payments.

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                             │
│   ┌─────────────┐    ┌──────────────┐    ┌─────────────────┐   │
│   │  Next.js Web │    │ React Native │    │  Admin Dashboard │   │
│   │   (PWA)      │    │  iOS/Android │    │   (Internal)     │   │
│   └──────┬───────┘    └──────┬───────┘    └────────┬────────┘   │
│          └───────────────────┼──────────────────────┘            │
│                              │                                   │
│                    ┌─────────▼──────────┐                       │
│                    │   API Gateway      │                       │
│                    │   (Kong / AWS ALB) │                       │
│                    └─────────┬──────────┘                       │
└──────────────────────────────┼───────────────────────────────────┘
                               │
┌──────────────────────────────┼───────────────────────────────────┐
│                       SERVICE LAYER                              │
│                              │                                   │
│   ┌──────────┐  ┌───────────▼────────┐  ┌───────────────────┐  │
│   │ Auth     │  │ Marketplace        │  │ Payment           │  │
│   │ Service  │  │ Service            │  │ Service           │  │
│   │          │  │ (listings, search, │  │ (escrow, settle,  │  │
│   │ (JWT,    │  │  offers, disputes) │  │  fiat gateway)    │  │
│   │  KYC)    │  │                    │  │                   │  │
│   └──────────┘  └────────────────────┘  └───────────────────┘  │
│                                                                  │
│   ┌──────────┐  ┌────────────────────┐  ┌───────────────────┐  │
│   │ Wallet   │  │ Notification       │  │ Analytics         │  │
│   │ Service  │  │ Service            │  │ Service           │  │
│   │ (keys,   │  │ (push, email,      │  │ (metrics,         │  │
│   │  signing)│  │  in-app)           │  │  reporting)       │  │
│   └──────────┘  └────────────────────┘  └───────────────────┘  │
└──────────────────────────────┼───────────────────────────────────┘
                               │
┌──────────────────────────────┼───────────────────────────────────┐
│                       DATA LAYER                                 │
│                              │                                   │
│   ┌──────────┐  ┌───────────▼────────┐  ┌───────────────────┐  │
│   │PostgreSQL│  │ Redis              │  │ Elasticsearch     │  │
│   │(users,   │  │ (sessions, cache,  │  │ (listing search,  │  │
│   │ listings,│  │  rate limiting,    │  │  full-text,       │  │
│   │ orders)  │  │  pub/sub)          │  │  filters)         │  │
│   └──────────┘  └────────────────────┘  └───────────────────┘  │
│                                                                  │
│   ┌──────────┐  ┌────────────────────┐                          │
│   │ IPFS /   │  │ S3                 │                          │
│   │ Arweave  │  │ (image thumbnails, │                          │
│   │ (perma-  │  │  user uploads,     │                          │
│   │  nent)   │  │  temp storage)     │                          │
│   └──────────┘  └────────────────────┘                          │
└──────────────────────────────┼───────────────────────────────────┘
                               │
┌──────────────────────────────┼───────────────────────────────────┐
│                    BLOCKCHAIN LAYER                               │
│                              │                                   │
│   ┌──────────────────────────▼───────────────────────────────┐  │
│   │                   Polygon PoS                             │  │
│   │                                                           │  │
│   │  ┌────────────┐ ┌────────────┐ ┌────────────────────┐   │  │
│   │  │ RVToken    │ │ RVEscrow   │ │ RVStaking          │   │  │
│   │  │ (ERC-20    │ │ (payment   │ │ (tiered lockups,   │   │  │
│   │  │  + burn)   │ │  escrow)   │ │  reward distrib.)  │   │  │
│   │  └────────────┘ └────────────┘ └────────────────────┘   │  │
│   │                                                           │  │
│   │  ┌────────────┐ ┌────────────────────────────────────┐  │  │
│   │  │ RVGov      │ │ The Graph (indexer)                │   │  │
│   │  │ (future    │ │ Event listeners                    │   │  │
│   │  │  DAO)      │ │ Transaction monitoring             │   │  │
│   │  └────────────┘ └────────────────────────────────────┘  │  │
│   └───────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. Blockchain Selection: Why Polygon

| Criteria | Ethereum | Polygon PoS | Solana | Arbitrum | Avalanche |
|---|---|---|---|---|---|
| Transaction fee | $2-50 | $0.001-0.01 | $0.0005 | $0.01-0.10 | $0.01-0.05 |
| Block time | 12s | 2s | 0.4s | 0.25s | 2s |
| EVM compatible | Yes | Yes | No (Rust) | Yes | Yes |
| Ecosystem maturity | Highest | High | Medium | Growing | Medium |
| Bridge infrastructure | N/A | Excellent | Limited | Good | Good |
| Developer tooling | Best | Best (EVM) | Growing | Best (EVM) | Good |
| Fiat on-ramp support | Excellent | Excellent | Good | Limited | Limited |

**Decision: Polygon PoS as primary chain.**

Rationale:
- Sub-cent fees make micro-transactions viable (listing fees, small purchases)
- 2-second finality is fast enough for marketplace UX
- Full EVM compatibility means Solidity smart contracts, Hardhat tooling, ethers.js — the largest developer talent pool
- Mature fiat on-ramp ecosystem (MoonPay, Transak, Wyre all support Polygon)
- Polygon bridge to Ethereum is battle-tested for users who want to move RV to mainnet
- The Graph supports Polygon natively for indexing

**Future expansion**: Deploy on Arbitrum or Base as a secondary chain with a canonical bridge. This is Phase 5+ work.

---

## 3. Frontend Architecture

### Web Application — Next.js 14+

```
next-app/
├── app/
│   ├── (auth)/
│   │   ├── login/
│   │   ├── register/
│   │   └── verify/
│   ├── (marketplace)/
│   │   ├── browse/
│   │   ├── listing/[id]/
│   │   ├── sell/
│   │   └── search/
│   ├── (wallet)/
│   │   ├── dashboard/
│   │   ├── send/
│   │   ├── stake/
│   │   └── history/
│   ├── (profile)/
│   │   ├── [username]/
│   │   ├── settings/
│   │   └── orders/
│   └── api/           ← BFF (Backend-for-Frontend) routes
├── components/
│   ├── marketplace/   ← listing cards, filters, search bar
│   ├── wallet/        ← balance display, staking UI, transaction list
│   ├── common/        ← buttons, modals, navigation
│   └── auth/          ← login forms, KYC flow
├── hooks/
│   ├── useWallet.ts
│   ├── useListings.ts
│   └── useStaking.ts
├── lib/
│   ├── api.ts         ← API client
│   ├── contracts.ts   ← ethers.js contract bindings
│   └── auth.ts        ← session management
└── public/
```

Key decisions:
- **Server-side rendering** for marketplace pages (SEO for listings)
- **Client-side** for wallet/staking interactions (requires wallet connection)
- **PWA-enabled** so mobile web users get near-native experience
- **wagmi + viem** for wallet connection (MetaMask, WalletConnect, Coinbase Wallet)
- Images served via CloudFront CDN with on-the-fly resizing (Sharp/imgproxy)

### Mobile Application — React Native

Shared business logic with web where possible via a shared TypeScript package. Key mobile-specific features:
- Push notifications for offers, sales, shipping updates
- Camera integration for listing photo capture
- Biometric auth (Face ID / fingerprint) for transaction signing
- Deep linking for sharing listings

---

## 4. Backend Architecture

### Service Decomposition

The backend is a set of focused microservices behind an API gateway. Each service owns its data and communicates via REST (synchronous) or message queue (asynchronous).

#### Auth Service
- User registration and login (email + password, OAuth, wallet-based)
- JWT access tokens (15-min expiry) + refresh tokens (7-day, rotated)
- KYC/AML integration via third-party provider (Jumio, Onfido, or Sumsub)
- Rate limiting on auth endpoints (Redis-backed sliding window)
- 2FA via TOTP (Google Authenticator) or SMS

#### Marketplace Service
- CRUD for listings (title, description, price in RV, category, images, condition)
- Offer/counter-offer negotiation flow
- Order lifecycle: `CREATED → PAID → SHIPPED → DELIVERED → COMPLETED → DISPUTED`
- Seller/buyer ratings (1-5 stars + text, only after completed transaction)
- Category taxonomy with faceted search
- Listing moderation queue (ML-assisted content screening + manual review)

#### Payment Service
- Initiates on-chain escrow transactions
- Monitors blockchain events for payment confirmation
- Handles fiat on-ramp (user buys RV with card/bank)
- Handles fiat off-ramp (user sells RV for fiat)
- Calculates and applies burn amounts
- Distributes platform fees

#### Wallet Service
- Custodial wallet creation for each user (HD wallet derivation from master seed)
- Non-custodial option: user connects external wallet
- Transaction signing (custodial: server-side HSM; non-custodial: client-side)
- Balance queries (on-chain + cached in Redis)
- Withdrawal processing with rate limits and security holds

#### Notification Service
- Email (SendGrid / AWS SES)
- Push notifications (Firebase Cloud Messaging)
- In-app notification feed
- Event-driven: listens to message queue for triggers

#### Analytics Service
- Transaction volume tracking
- Token velocity and burn metrics
- User engagement and retention analytics
- Marketplace health metrics (time-to-sale, average listing price)

### Message Queue

**RabbitMQ** (or AWS SQS) for async event processing:

```
Events:
  order.created      → Payment Service (initiate escrow)
  payment.confirmed  → Marketplace Service (update order status)
  payment.confirmed  → Notification Service (notify seller)
  order.shipped      → Notification Service (notify buyer)
  order.completed    → Payment Service (release escrow)
  order.completed    → Analytics Service (record metrics)
  order.disputed     → Dispute Service (open case)
  token.burned       → Analytics Service (update burn metrics)
  user.registered    → Notification Service (welcome email)
  listing.created    → Search Service (index listing)
  listing.flagged    → Moderation Service (review queue)
```

### Database Schema (PostgreSQL — Core Tables)

```sql
-- Users
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) UNIQUE NOT NULL,
    username        VARCHAR(50) UNIQUE NOT NULL,
    password_hash   VARCHAR(255),
    wallet_address  VARCHAR(42),
    kyc_status      VARCHAR(20) DEFAULT 'NONE',  -- NONE, PENDING, VERIFIED, REJECTED
    kyc_tier        INT DEFAULT 0,               -- 0=none, 1=basic, 2=full
    rating_avg      DECIMAL(3,2) DEFAULT 0,
    rating_count    INT DEFAULT 0,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Listings
CREATE TABLE listings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id       UUID REFERENCES users(id),
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    price_rv        DECIMAL(18,8) NOT NULL,      -- price in RV tokens
    price_usd_equiv DECIMAL(10,2),               -- USD equivalent at listing time
    category        VARCHAR(100) NOT NULL,
    condition       VARCHAR(20),                  -- NEW, LIKE_NEW, GOOD, FAIR, POOR
    status          VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, SOLD, REMOVED, EXPIRED
    images          JSONB,                        -- array of image URLs
    shipping_options JSONB,                       -- weight, dimensions, methods
    location        VARCHAR(100),
    view_count      INT DEFAULT 0,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    expires_at      TIMESTAMPTZ
);

-- Orders
CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id      UUID REFERENCES listings(id),
    buyer_id        UUID REFERENCES users(id),
    seller_id       UUID REFERENCES users(id),
    amount_rv       DECIMAL(18,8) NOT NULL,
    burn_amount     DECIMAL(18,8) NOT NULL,
    platform_fee    DECIMAL(18,8) NOT NULL,
    seller_receives DECIMAL(18,8) NOT NULL,
    escrow_tx_hash  VARCHAR(66),                 -- on-chain escrow transaction
    release_tx_hash VARCHAR(66),                 -- on-chain release transaction
    status          VARCHAR(20) DEFAULT 'CREATED',
    shipped_at      TIMESTAMPTZ,
    delivered_at    TIMESTAMPTZ,
    tracking_number VARCHAR(100),
    tracking_carrier VARCHAR(50),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

-- Offers (negotiation)
CREATE TABLE offers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id      UUID REFERENCES listings(id),
    buyer_id        UUID REFERENCES users(id),
    amount_rv       DECIMAL(18,8) NOT NULL,
    status          VARCHAR(20) DEFAULT 'PENDING', -- PENDING, ACCEPTED, REJECTED, EXPIRED
    message         TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    expires_at      TIMESTAMPTZ
);

-- Ratings
CREATE TABLE ratings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID REFERENCES orders(id) UNIQUE,
    reviewer_id     UUID REFERENCES users(id),
    reviewee_id     UUID REFERENCES users(id),
    score           INT CHECK (score BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Disputes
CREATE TABLE disputes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID REFERENCES orders(id) UNIQUE,
    opened_by       UUID REFERENCES users(id),
    reason          VARCHAR(50),
    description     TEXT,
    status          VARCHAR(20) DEFAULT 'OPEN',   -- OPEN, UNDER_REVIEW, RESOLVED
    resolution      TEXT,
    resolved_by     UUID,                          -- admin user
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    resolved_at     TIMESTAMPTZ
);

-- Indexes
CREATE INDEX idx_listings_seller ON listings(seller_id);
CREATE INDEX idx_listings_category ON listings(category);
CREATE INDEX idx_listings_status ON listings(status);
CREATE INDEX idx_listings_created ON listings(created_at DESC);
CREATE INDEX idx_orders_buyer ON orders(buyer_id);
CREATE INDEX idx_orders_seller ON orders(seller_id);
CREATE INDEX idx_orders_status ON orders(status);
```

### Search — Elasticsearch

Listings are indexed in Elasticsearch for full-text search with faceted filtering:

```json
{
  "mappings": {
    "properties": {
      "title":       { "type": "text", "analyzer": "standard" },
      "description": { "type": "text", "analyzer": "standard" },
      "category":    { "type": "keyword" },
      "condition":   { "type": "keyword" },
      "price_rv":    { "type": "float" },
      "price_usd":   { "type": "float" },
      "location":    { "type": "geo_point" },
      "seller_rating": { "type": "float" },
      "created_at":  { "type": "date" },
      "status":      { "type": "keyword" }
    }
  }
}
```

Sync strategy: Marketplace Service publishes `listing.created`, `listing.updated`, `listing.removed` events → Search indexer consumer updates Elasticsearch.

---

## 5. Infrastructure

### Cloud Architecture (AWS)

```
┌─────────────────────────────────────────────────────┐
│  Route 53 (DNS) → CloudFront (CDN)                  │
│       │                                              │
│  ALB (Application Load Balancer)                     │
│       │                                              │
│  EKS (Kubernetes)                                    │
│  ├── auth-service        (2-4 pods)                  │
│  ├── marketplace-service (4-8 pods)                  │
│  ├── payment-service     (2-4 pods)                  │
│  ├── wallet-service      (2-3 pods)                  │
│  ├── notification-service(2-3 pods)                  │
│  ├── search-indexer      (1-2 pods)                  │
│  └── analytics-service   (1-2 pods)                  │
│                                                      │
│  RDS PostgreSQL (Multi-AZ, read replicas)            │
│  ElastiCache Redis (cluster mode)                    │
│  Amazon Elasticsearch Service                        │
│  Amazon MQ (RabbitMQ managed)                        │
│  S3 (image storage)                                  │
│  KMS (key management for wallet HSM)                 │
│  CloudWatch (monitoring, alerting)                   │
│  WAF (web application firewall)                      │
└─────────────────────────────────────────────────────┘
```

### Blockchain Infrastructure

- **RPC Provider**: Alchemy or QuickNode (Polygon) — redundant providers with failover
- **Indexer**: The Graph subgraph for token transfers, escrow events, staking events
- **Event Listener**: Dedicated service polling/websocket for on-chain events
- **Transaction Relayer**: Meta-transaction support (optional) so users without MATIC can still transact (platform sponsors gas)

### Monitoring & Observability

- **Metrics**: Prometheus + Grafana (service health, latency, throughput)
- **Logging**: ELK stack or AWS CloudWatch Logs (structured JSON logging)
- **Tracing**: OpenTelemetry + Jaeger (distributed request tracing)
- **Alerting**: PagerDuty integration for P0/P1 incidents
- **Blockchain monitoring**: Custom dashboards for token supply, burn rate, staking TVL

---

## 6. Key Data Flows

### Purchase Flow (Happy Path)

```
1. Buyer clicks "Buy Now" on listing
2. Frontend calls POST /api/orders { listing_id }
3. Marketplace Service validates listing is active, calculates fees
4. Payment Service generates escrow transaction parameters
5. Frontend prompts wallet signature (or signs custodially)
6. Buyer's wallet calls RVEscrow.deposit(orderId, amount)
7. On-chain: tokens transferred to escrow contract, burn executed
8. Event listener detects EscrowDeposited event
9. Payment Service updates order status → PAID
10. Notification Service alerts seller: "You have a sale!"
11. Seller ships item, enters tracking number
12. Buyer confirms receipt (or auto-confirm after 7 days)
13. Payment Service calls RVEscrow.release(orderId)
14. On-chain: escrowed tokens transferred to seller
15. Order status → COMPLETED
16. Both parties prompted to leave ratings
```

### Dispute Flow

```
1. Buyer opens dispute within 48 hours of delivery
2. Dispute Service creates case, notifies both parties
3. Both parties submit evidence (photos, messages, tracking)
4. Admin reviews and decides:
   a. Release to seller (buyer claim rejected)
   b. Refund to buyer (full or partial from escrow)
   c. Split (partial refund, partial release)
5. Smart contract executes resolution
6. Dispute marked resolved
```

---

## 7. API Design (REST)

### Core Endpoints

```
AUTH
  POST   /api/auth/register
  POST   /api/auth/login
  POST   /api/auth/refresh
  POST   /api/auth/verify-email
  POST   /api/auth/2fa/setup
  POST   /api/auth/2fa/verify

LISTINGS
  GET    /api/listings                    ← browse/search (paginated)
  GET    /api/listings/:id                ← single listing
  POST   /api/listings                    ← create listing
  PUT    /api/listings/:id                ← update listing
  DELETE /api/listings/:id                ← remove listing
  POST   /api/listings/:id/report         ← flag listing

ORDERS
  POST   /api/orders                      ← initiate purchase
  GET    /api/orders                      ← user's orders
  GET    /api/orders/:id                  ← order detail
  POST   /api/orders/:id/ship             ← seller marks shipped
  POST   /api/orders/:id/confirm          ← buyer confirms receipt
  POST   /api/orders/:id/dispute          ← open dispute

OFFERS
  POST   /api/listings/:id/offers         ← make offer
  PUT    /api/offers/:id/accept           ← accept offer
  PUT    /api/offers/:id/reject           ← reject offer
  POST   /api/offers/:id/counter          ← counter offer

WALLET
  GET    /api/wallet/balance              ← RV balance
  GET    /api/wallet/transactions         ← transaction history
  POST   /api/wallet/withdraw             ← withdraw RV
  GET    /api/wallet/address              ← deposit address

STAKING
  GET    /api/staking/pools               ← available pools
  POST   /api/staking/stake               ← stake RV
  POST   /api/staking/unstake             ← unstake RV
  GET    /api/staking/rewards             ← pending rewards

USER
  GET    /api/users/:username             ← public profile
  PUT    /api/users/me                    ← update profile
  GET    /api/users/me/ratings            ← received ratings
  POST   /api/users/me/kyc               ← submit KYC documents
```

All endpoints return JSON. Pagination uses cursor-based pagination for listings (better for real-time feeds than offset-based).

---

## 8. Critical Design Decisions

### Custodial vs Non-Custodial Wallets

**Decision: Hybrid — custodial by default, non-custodial optional.**

Most marketplace users (coming from eBay/Mercari) will not have MetaMask. Forcing wallet setup kills onboarding. Instead:

- **Default**: Platform creates a custodial wallet (HD-derived, keys in AWS KMS / HSM). User sees a simple "RV Balance" without needing to understand blockchain.
- **Optional**: Power users can connect external wallets (MetaMask, WalletConnect) and go non-custodial.
- **Regulatory note**: Custodial wallets mean the platform is a money transmitter in most jurisdictions. This must be addressed in compliance planning.

### Price Display

**The Velocity Paradox**: If RV appreciates, rational users hoard rather than spend. This kills marketplace activity.

**Solution**: Listings are priced in **USD equivalent**, with the RV amount calculated at checkout using a live price feed. Sellers think in dollars. Buyers pay in RV. The token is the payment rail, not the unit of account. This is critical for mainstream adoption.

### Gas Fees

Users should never need to hold MATIC for gas. Options:
1. **Meta-transactions** (EIP-2771): Platform relayer pays gas, cost absorbed in platform fee
2. **Account abstraction** (EIP-4337): Smart contract wallets that can pay gas in RV
3. **Simplest approach**: Platform batches escrow operations and subsidizes gas from fee revenue

Decision: Start with option 1 (meta-transactions via OpenZeppelin Defender Relayer), migrate to option 2 as account abstraction matures on Polygon.
