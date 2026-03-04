# RVSwaps — ReValue Swaps

A **crypto-powered P2P marketplace** where users buy and sell physical goods — with a payment rail built on **ReValue Coin (RV)** that makes crypto invisible by default.

---

## Executive Summary

RVSwaps is a Mercari/eBay-style marketplace where every transaction feeds a deflationary token economy through burn, staking, and fee routing. But the product must succeed as a **mainstream marketplace first**.

**Core design principles:**

1. **Crypto is invisible by default** — Buyers and sellers use USD. They never need to touch wallets, gas, or chains.
2. **Listings are priced in USD** — RV is computed at checkout using a TWAP oracle.
3. **Sellers get paid in USD by default** — Auto-Cashout converts RV→USD silently. "Keep RV" is opt-in.
4. **Protocol-controlled liquidity + treasury reserves** stabilize all conversions.
5. **Automated buyback → burn** — A portion of fees funds on-market RV buybacks that are burned.
6. **"Amazon Mode" launch** — The platform seeds inventory to solve the cold-start problem. Day one has goods to buy.

RVSwaps is **not** a crypto project with a marketplace bolted on. It is a **marketplace** with crypto economics running under the hood.

---

## Architecture at a Glance

```
┌───────────────────────────────────────────────────────────────────────┐
│                          CLIENT LAYER                                  │
│   Next.js Web (PWA)  ·  React Native (iOS/Android)  ·  Admin Panel    │
└────────────────────────────────┬──────────────────────────────────────┘
                                 │
                     ┌───────────▼────────────┐
                     │   API Gateway (Kong)    │
                     └───────────┬────────────┘
                                 │
┌────────────────────────────────┼──────────────────────────────────────┐
│                        SERVICE LAYER                                   │
│                                │                                       │
│  Auth · KYC · Marketplace · Orders · Search · Payments ·               │
│  Conversion Engine · Wallet · Notifications · Risk/Fraud ·             │
│  Moderation · Analytics · Buyback Engine                               │
│                                                                        │
│  Message Bus (RabbitMQ) — event-driven state transitions               │
└────────────────────────────────┼──────────────────────────────────────┘
                                 │
┌────────────────────────────────┼──────────────────────────────────────┐
│                          DATA LAYER                                    │
│  PostgreSQL · Redis · Elasticsearch · S3/CDN · Event Indexer           │
└────────────────────────────────┼──────────────────────────────────────┘
                                 │
┌────────────────────────────────┼──────────────────────────────────────┐
│                      BLOCKCHAIN LAYER (Polygon PoS)                    │
│  RVToken (ERC-20 + burn) · RVEscrow · RVStaking · RVBuybackBurn       │
│  Oracle (Chainlink TWAP) · The Graph (indexer) · Meta-tx Relayer       │
└───────────────────────────────────────────────────────────────────────┘
```

---

## Technical Stack

| Layer | Technology |
|---|---|
| Blockchain | Polygon PoS (primary), Arbitrum (future) |
| Smart Contracts | Solidity 0.8.24, OpenZeppelin 5.x, Hardhat + Foundry |
| Backend | Node.js + Express, PostgreSQL, Redis, Elasticsearch, RabbitMQ |
| Frontend | Next.js 14+ (web), React Native (mobile) |
| Payments | Stripe (fiat), MoonPay/Transak (on-ramp), Chainlink (oracle) |
| Storage | S3 + CloudFront CDN (images), The Graph (chain indexing) |
| Infrastructure | AWS EKS, RDS, ElastiCache, CloudHSM, WAF, CloudWatch |

---

## Documentation

| Document | What's Inside |
|---|---|
| [Architecture](docs/ARCHITECTURE.md) | Full system design, service boundaries, DB schema, API endpoints, data flows |
| [User Flows](docs/USER_FLOWS.md) | Buyer + seller journeys, crypto-invisible UX, screen-by-screen flows |
| [Conversion Engine](docs/CONVERSION_ENGINE.md) | USD↔RV conversion, TWAP oracle, slippage controls, circuit breakers, reserves |
| [Tokenomics](docs/TOKENOMICS.md) | 1B supply, burns, buyback engine, POL, staking sustainability, death spiral analysis |
| [Smart Contracts](docs/SMART_CONTRACTS.md) | Full Solidity code, interfaces, invariants, upgrade patterns, testing strategy |
| [Security](docs/SECURITY.md) | Threat model, mitigations across contracts/platform/economics, incident response |
| [Monetization & Growth](docs/MONETIZATION_AND_GROWTH.md) | Fee schedule, unit economics, "Amazon Mode" seeding, anchor sellers, growth phases |
| [Roadmap](docs/ROADMAP.md) | 6-phase build plan with gates, staffing, budget, risk list, lean MVP approach |
| [Unknowns & Assumptions](docs/UNKNOWNS.md) | Open questions, assumptions to validate, regulatory risks |

---

## Quick Reference

| Property | Value |
|---|---|
| Token | ReValue Coin (RV) — ERC-20 on Polygon |
| Max Supply | 1,000,000,000 RV |
| Pricing | All listings in USD; RV computed at checkout via TWAP |
| Transaction Burn | 1% of sale price |
| Platform Fee | 2.5% (buyer-side) |
| Staking Tiers | Flex/Bronze/Silver/Gold/Diamond (3–18% APY) |
| Seller Payout | USD by default (auto-cashout), or keep RV |
| Buyback | 10% of fees → automated market buyback → burn |

---

## License

Proprietary — All rights reserved.
