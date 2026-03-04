# RVSwaps — User Flows (Crypto-Invisible)

---

## Design Principle

The user never needs to know they're using crypto. Every flow is designed so that a user familiar with eBay or Mercari can complete it without learning anything new. Crypto features (staking, RV balance, burn dashboard) are available but hidden behind an opt-in "Advanced" section.

---

## 1. Buyer Journey — First-Time Purchase

### 1.1 Registration

```
Screen: Landing Page
┌─────────────────────────────────────┐
│  RVSwaps — Buy & Sell for Less      │
│                                     │
│  [Browse Deals]  [Sign Up Free]     │
│                                     │
│  ┌─────────┐ ┌─────────┐ ┌──────┐  │
│  │ $45     │ │ $120    │ │ $28  │  │
│  │ PS5     │ │ Jordan  │ │ Poke │  │
│  │ Control │ │ 1 Retro │ │ Card │  │
│  └─────────┘ └─────────┘ └──────┘  │
│                                     │
│  Daily Deals · Rare Finds · New     │
└─────────────────────────────────────┘

User clicks "Sign Up Free"

Screen: Registration
┌─────────────────────────────────────┐
│  Create Your Account                │
│                                     │
│  Email:     [________________]      │
│  Username:  [________________]      │
│  Password:  [________________]      │
│                                     │
│  — or —                             │
│  [Continue with Google]             │
│  [Continue with Apple]              │
│                                     │
│  [Create Account]                   │
│                                     │
│  No crypto knowledge required.      │
│  Pay with card, get great deals.    │
└─────────────────────────────────────┘

→ Behind the scenes: Wallet Service creates a custodial wallet.
   User never sees this.
→ Email verification sent. User can browse immediately.
```

### 1.2 Browsing & Discovery

```
Screen: Browse / Home Feed
┌─────────────────────────────────────┐
│  [🔍 Search for anything...]        │
│                                     │
│  Categories:                        │
│  Electronics · Sneakers · Cards     │
│  Gaming · Fashion · Collectibles    │
│                                     │
│  ── Daily Deals ──                  │
│  ┌────────┐ ┌────────┐ ┌────────┐  │
│  │ $35    │ │ $89    │ │ $15    │  │
│  │ AirPods│ │ Dunks  │ │ Chariz │  │
│  │ Pro    │ │ Low    │ │ ard    │  │
│  │ ★★★★☆  │ │ ★★★★★  │ │ ★★★★☆  │  │
│  └────────┘ └────────┘ └────────┘  │
│                                     │
│  ── Just Listed ──                  │
│  ┌────────┐ ┌────────┐ ┌────────┐  │
│  │ $210   │ │ $45    │ │ $67    │  │
│  │ Switch │ │ Yeezy  │ │ Vinyl  │  │
│  │ OLED   │ │ Slide  │ │ Record │  │
│  └────────┘ └────────┘ └────────┘  │
└─────────────────────────────────────┘

KEY: All prices are in USD. No RV visible anywhere
on the browse experience.
```

### 1.3 Listing Detail & Purchase

```
Screen: Listing Detail
┌─────────────────────────────────────┐
│  ← Back                            │
│                                     │
│  [    Photo of AirPods Pro    ]     │
│  [  ·  ·  ●  ·  ]  (photo dots)   │
│                                     │
│  AirPods Pro (2nd Gen)              │
│  $35.00                             │
│  Condition: Like New                │
│  Seller: @techdeals (★★★★☆ · 47)   │
│                                     │
│  Free shipping · Ships in 2 days    │
│                                     │
│  [Buy Now — $35.88]                 │
│  [Make Offer]                       │
│                                     │
│  ───────────────────                │
│  Description:                       │
│  Barely used, original box...       │
└─────────────────────────────────────┘

Price breakdown (shown on hover/tap of $35.88):
  Item price:     $35.00
  Platform fee:    $0.88  (2.5%)
  Shipping:        Free
  Total:          $35.88

→ NO mention of RV, tokens, crypto, or blockchain.
→ "Platform fee" is the only added cost.
```

### 1.4 Checkout & Payment

```
Screen: Checkout
┌─────────────────────────────────────┐
│  Checkout                           │
│                                     │
│  AirPods Pro (2nd Gen)      $35.00  │
│  Platform fee                $0.88  │
│  Shipping                    FREE   │
│  ─────────────────────────────────  │
│  Total                      $35.88  │
│                                     │
│  Ship to:                           │
│  [123 Main St, Austin TX]  [Edit]   │
│                                     │
│  Payment Method:                    │
│  ○ Credit/Debit Card               │
│    [4242 ···· ···· 1234]   [Edit]   │
│  ○ Apple Pay                        │
│  ○ ACH Bank Transfer                │
│  ○ Pay with RV Balance ($0.00)      │
│                                     │
│  [Place Order — $35.88]             │
│                                     │
│  🔒 Protected by RVSwaps Buyer      │
│     Guarantee. Full refund if item  │
│     doesn't arrive or isn't as      │
│     described.                      │
└─────────────────────────────────────┘

→ "Pay with RV Balance" shown but grayed out for
   new users with $0 balance. This is the crypto
   opt-in — experienced users can top up RV and
   pay directly.

→ KYC check: if user is Tier 0 and order > $50,
   prompt for Tier 1 verification before payment.
```

### 1.5 Post-Purchase

```
Screen: Order Confirmation
┌─────────────────────────────────────┐
│  ✓ Order Placed!                    │
│                                     │
│  Order #RVS-A8F3X2                  │
│  AirPods Pro (2nd Gen)              │
│  Total: $35.88                      │
│                                     │
│  What happens next:                 │
│  1. Seller ships within 5 days      │
│  2. You'll get a tracking number    │
│  3. Confirm when it arrives         │
│                                     │
│  Your payment is held safely in     │
│  escrow until you confirm receipt.  │
│                                     │
│  [View Order]  [Keep Shopping]      │
└─────────────────────────────────────┘

→ "escrow" is the only crypto-adjacent word,
   and it's used in its traditional financial
   meaning. No mention of blockchain.

→ Behind the scenes:
   - Stripe charged $35.88
   - Conversion Engine bought 358.8 RV at TWAP
   - RVEscrow.deposit() called via meta-tx
   - Escrow holds 358.8 RV on-chain
```

### 1.6 Order Tracking & Delivery Confirmation

```
Screen: Order Detail (post-ship)
┌─────────────────────────────────────┐
│  Order #RVS-A8F3X2                  │
│                                     │
│  Status: Shipped ✈                  │
│  Tracking: 9400111899223847652      │
│  Carrier: USPS Priority Mail       │
│                                     │
│  ── Tracking Updates ──            │
│  Mar 3  Delivered to mailbox        │
│  Mar 2  Out for delivery            │
│  Mar 1  In transit, Austin TX       │
│  Feb 28 Shipped from Portland OR    │
│                                     │
│  [Confirm Delivery]                 │
│  [Open Dispute]                     │
│                                     │
│  Auto-confirms in 12 days if no     │
│  action taken.                      │
└─────────────────────────────────────┘
```

---

## 2. Seller Journey — Listing & Getting Paid

### 2.1 Creating a Listing

```
Screen: Sell an Item
┌─────────────────────────────────────┐
│  Sell an Item                       │
│                                     │
│  Photos (up to 10):                 │
│  [📷 Add Photos]                    │
│  [img1] [img2] [img3] [+]          │
│                                     │
│  Title:                             │
│  [AirPods Pro 2nd Gen, Like New   ] │
│                                     │
│  Category:    [Electronics ▼]       │
│  Subcategory: [Headphones ▼]        │
│  Condition:   [Like New ▼]          │
│                                     │
│  Price: $ [35.00]                   │
│                                     │
│  You'll receive: ~$34.12            │
│  (after 2.5% buyer fee, we don't   │
│   charge sellers)                   │
│                                     │
│  Shipping:                          │
│  ○ Free shipping (you pay)         │
│  ○ Buyer pays shipping             │
│  Weight: [__] oz                    │
│                                     │
│  Description:                       │
│  [Barely used, comes with box...  ] │
│                                     │
│  [List Item]                        │
└─────────────────────────────────────┘

→ Seller enters USD price. Period.
→ "You'll receive" is an estimate in USD.
→ No mention of RV, tokens, or crypto.
→ KYC Tier 1 required to list (prompted if needed).
```

### 2.2 Seller Dashboard

```
Screen: Seller Dashboard
┌─────────────────────────────────────┐
│  Your Shop                          │
│                                     │
│  Revenue (this month):  $1,247.00   │
│  Items sold:            23          │
│  Active listings:       47          │
│  Rating: ★★★★☆ (4.6 · 89 reviews)  │
│                                     │
│  ── Active Listings ──              │
│  AirPods Pro          $35   3 views │
│  Nike Dunk Low        $89  12 views │
│  Pokémon Booster Box  $120  8 views │
│  [+ List New Item]                  │
│                                     │
│  ── Recent Sales ──                 │
│  PS5 Controller   $45  → Paid Out  │
│  Jordan 1 Retro  $120  → Shipping  │
│  MacBook Charger  $28  → Completed │
│                                     │
│  ── Payout Settings ──              │
│  Payout method: USD (bank transfer) │
│  [Change to: Keep as RV]           │
│                                     │
│  Next payout: $73.00 on Mar 7      │
└─────────────────────────────────────┘

→ Default: seller sees everything in USD.
→ "Change to: Keep as RV" is the opt-in for
   crypto-savvy sellers who want exposure to RV.
→ Payouts arrive as USD via ACH (2-3 business days).
```

### 2.3 Payout Flow (Default: Auto-Cashout to USD)

```
Seller Flow (behind the scenes):

1. Buyer confirms delivery
2. RVEscrow.release() executes on-chain:
   - 4.5 RV burned (1% of item price)
   - 11.28 RV sent to platform fee wallet
   - 445.52 RV sent to seller's custodial wallet
3. Seller payout_preference = 'USD' (default):
   - Conversion Engine sells 445.52 RV via DEX or treasury
   - Receives ~$44.55 USDC
   - Payment Service queues ACH payout of $44.55
   - Seller receives bank deposit in 2-3 business days
4. Seller sees in dashboard:
   "Order #RVS-A8F3X2: $44.55 paid out via bank transfer"

→ Seller never sees RV amounts. Just USD in, USD out.
→ The entire crypto layer is invisible.
```

### 2.4 Payout Flow (Opt-in: Keep RV)

```
If seller chooses "Keep as RV":

1. Same escrow release as above
2. 445.52 RV stays in seller's custodial wallet
3. Seller sees in dashboard:
   "Order #RVS-A8F3X2: 445.52 RV received ($44.55 value)"
4. Seller can:
   - Use RV to buy items on the marketplace
   - Stake RV for yield (3-18% APY depending on tier)
   - Withdraw RV to external wallet (2% burn applies)
   - Switch back to USD payouts anytime

→ This is the crypto opt-in path. Sellers who
   understand crypto can accumulate and stake RV.
→ Staking tiers give fee discounts, creating a
   loyalty loop for power sellers.
```

---

## 3. Wallet & Staking (Opt-In Power User Flows)

### 3.1 Wallet Dashboard

```
Screen: Wallet (accessible from profile menu)
┌─────────────────────────────────────┐
│  Your Wallet                        │
│                                     │
│  Balance: 2,450.00 RV              │
│  ≈ $245.00 USD                      │
│                                     │
│  [Buy RV]  [Sell RV]  [Send]       │
│                                     │
│  ── Staking ──                      │
│  Staked: 1,000 RV (Gold tier)       │
│  Earning: 14% APY                   │
│  Rewards pending: 38.2 RV          │
│  Unlocks in: 87 days                │
│  [Claim Rewards]                    │
│                                     │
│  ── Activity ──                     │
│  Mar 3  +445.52 RV  Sale received  │
│  Mar 2   -50.00 RV  Purchase       │
│  Mar 1   +19.10 RV  Rewards claim  │
│  Feb 28 -1000.0 RV  Staked (Gold)  │
│                                     │
│  ── RV Stats ──                     │
│  Total burned: 12.4M RV            │
│  Circulating: 847.6M RV            │
│  Your fee discount: 10% (Gold)     │
└─────────────────────────────────────┘

→ This screen is NEVER shown to new users.
→ It's behind a "Wallet" menu item that only
   appears after the user opts in or receives RV.
→ Shows both RV and USD equivalent.
```

### 3.2 Staking Flow

```
Screen: Stake RV
┌─────────────────────────────────────┐
│  Stake Your RV                      │
│                                     │
│  Available: 2,450.00 RV ($245.00)   │
│                                     │
│  Choose a tier:                     │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ Flex    · No lock  ·  3% APY│    │
│  │ No fee discount             │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ Bronze · 30 days ·  6% APY │    │
│  │ No fee discount             │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ Silver · 90 days · 10% APY │    │
│  │ 5% fee discount             │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────────┐│
│  │ ★ Gold  · 180 days · 14% APY  ││
│  │ 10% fee discount + early access││
│  └─────────────────────────────────┘│
│  ┌─────────────────────────────┐    │
│  │ Diamond · 365 days · 18% APY│   │
│  │ 15% discount + governance   │    │
│  └─────────────────────────────┘    │
│                                     │
│  Amount to stake: [1000] RV         │
│  Selected: Gold (180 days)          │
│  Estimated annual yield: 140 RV     │
│                                     │
│  ⚠ Early unstake = 50% reward      │
│    penalty. Principal always safe.  │
│                                     │
│  [Stake 1,000 RV]                   │
└─────────────────────────────────────┘
```

---

## 4. Offer / Negotiation Flow

```
Screen: Make Offer
┌─────────────────────────────────────┐
│  Make an Offer                      │
│                                     │
│  AirPods Pro (2nd Gen)              │
│  Listed at: $35.00                  │
│                                     │
│  Your offer: $ [28.00]              │
│                                     │
│  Message (optional):                │
│  [Would you take $28? Can pay now ] │
│                                     │
│  [Send Offer]                       │
│                                     │
│  Offer expires in 48 hours.         │
└─────────────────────────────────────┘

→ Seller receives notification: "You have a $28 offer"

Screen: Seller Response
┌─────────────────────────────────────┐
│  Offer from @buyer123              │
│                                     │
│  AirPods Pro (2nd Gen)              │
│  Listed at: $35.00                  │
│  Offered:   $28.00                  │
│                                     │
│  Message: "Would you take $28?"     │
│                                     │
│  [Accept $28]                       │
│  [Counter Offer: $ [32.00] ]        │
│  [Decline]                          │
└─────────────────────────────────────┘

→ If accepted, creates an order at the agreed price.
→ Counter-offer flow: buyer gets notified, can
   accept, counter again, or walk away.
→ Max 3 rounds of counter-offers, then it expires.
```

---

## 5. Dispute Flow (User-Facing)

```
Screen: Open Dispute
┌─────────────────────────────────────┐
│  Report a Problem                   │
│                                     │
│  Order #RVS-A8F3X2                  │
│  AirPods Pro (2nd Gen) — $35.88     │
│                                     │
│  What happened?                     │
│  ○ Item never arrived               │
│  ○ Item not as described             │
│  ○ Item arrived damaged              │
│  ● Other                            │
│                                     │
│  Describe the issue:                │
│  [Left AirPod doesn't work. Seller ]│
│  [said "like new" but clearly brok ]│
│                                     │
│  Add evidence (photos/screenshots): │
│  [📷 Add Photos]                    │
│  [img1] [img2]                      │
│                                     │
│  [Submit Dispute]                   │
│                                     │
│  Your payment is protected. Our     │
│  team will review within 48 hours.  │
└─────────────────────────────────────┘

→ Seller is notified and can submit their evidence.
→ Admin resolves. User sees:

Screen: Dispute Resolved
┌─────────────────────────────────────┐
│  Dispute Resolved                   │
│                                     │
│  Order #RVS-A8F3X2                  │
│  Resolution: Partial Refund         │
│                                     │
│  You'll receive: $25.12 refund      │
│  Seller receives: $10.76            │
│                                     │
│  Reason: Item functionality issue   │
│  confirmed. Partial refund awarded. │
│                                     │
│  Refund will appear in your bank    │
│  account within 3-5 business days.  │
│                                     │
│  [View Order]  [Contact Support]    │
└─────────────────────────────────────┘
```

---

## 6. KYC Upgrade Flow

KYC is progressive — never required upfront, only when needed.

```
Trigger: User tries to buy an item > $50 (Tier 0 limit)

Screen: Verify Your Identity
┌─────────────────────────────────────┐
│  Quick Verification Needed          │
│                                     │
│  To complete purchases over $50,    │
│  we need to verify your identity.   │
│  This takes about 2 minutes.        │
│                                     │
│  You'll need:                       │
│  ✓ Government-issued photo ID       │
│  ✓ A quick selfie                   │
│                                     │
│  [Start Verification]               │
│                                     │
│  Why? RVSwaps is committed to       │
│  keeping our marketplace safe.      │
│  Learn more →                       │
└─────────────────────────────────────┘

→ Opens Sumsub/Onfido embedded widget
→ User takes photo of ID + selfie
→ Usually approved in <5 minutes
→ Once Tier 1, limits increase significantly

Tier Limits:
  Tier 0 (email only):     Browse free, buy up to $50/tx, $200/month. No selling.
  Tier 1 (ID + selfie):    Buy/sell up to $5,000/tx, $20,000/month
  Tier 2 (proof of address): Unlimited
```

---

## 7. Crypto On-Ramp (For Users Who Want RV Directly)

This flow is optional — only for users who choose to engage with the crypto layer.

```
Screen: Buy RV (from Wallet tab)
┌─────────────────────────────────────┐
│  Buy RV Tokens                      │
│                                     │
│  Amount: $ [100.00] USD             │
│                                     │
│  You'll receive: ~990 RV            │
│  Rate: 1 RV ≈ $0.1010              │
│  Fee: 1.5% ($1.50)                  │
│                                     │
│  Pay with:                          │
│  ○ Credit/Debit Card               │
│  ○ ACH Bank Transfer (lower fee)   │
│  ○ Apple Pay                        │
│                                     │
│  [Buy 990 RV]                       │
│                                     │
│  Why buy RV?                        │
│  • Use it to buy items (no card     │
│    needed at checkout)              │
│  • Stake for 3-18% annual yield     │
│  • Get fee discounts as a seller    │
└─────────────────────────────────────┘

→ This is the MoonPay/Transak integration.
→ Rate shown is TWAP (not spot) for transparency.
```

---

## 8. Screen Visibility Matrix

Which elements are visible based on user engagement level:

| Element | Casual Buyer | Active Seller | Crypto-Engaged | Power User |
|---|---|---|---|---|
| Browse/search (USD prices) | ✅ | ✅ | ✅ | ✅ |
| Buy with card/Apple Pay | ✅ | ✅ | ✅ | ✅ |
| "Pay with RV Balance" option | Hidden | Visible | ✅ | ✅ |
| Wallet tab | Hidden | Hidden | ✅ | ✅ |
| RV balance in nav | Hidden | Hidden | ✅ | ✅ |
| Staking UI | Hidden | Hidden | Visible | ✅ |
| Burn dashboard | Hidden | Hidden | Hidden | ✅ |
| Governance voting | Hidden | Hidden | Hidden | ✅ |
| "Keep as RV" payout option | N/A | Visible | ✅ | ✅ |
| External wallet connect | Hidden | Hidden | Visible | ✅ |

Visibility is controlled by user flags:
- `has_rv_balance > 0` → show "Pay with RV" option
- `has_opted_into_crypto` → show Wallet tab, RV in nav
- `has_staking_position` → show Staking UI prominently
- `is_diamond_staker` → show Governance

The default experience has **zero** crypto UI. It looks and feels like Mercari.
