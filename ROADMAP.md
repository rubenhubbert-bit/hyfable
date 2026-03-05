# HyFable — Complete Launch Roadmap (Start to Finish)

> This document is the single source of truth for building, launching, and running
> the HyFable subscription business. It covers platform setup, storefront options,
> file delivery, payment processing, and a week-by-week execution plan.
>
> **Hand this to any AI or collaborator and they can pick up exactly where you left off.**

---

## Table of Contents

1. [Business Model Summary](#business-model-summary)
2. [Platform Comparison — Where to Sell](#platform-comparison--where-to-sell)
3. [Recommended Stack](#recommended-stack)
4. [Phase 1: Foundation (Weeks 1-2)](#phase-1-foundation-weeks-1-2)
5. [Phase 2: Content Production (Weeks 3-4)](#phase-2-content-production-weeks-3-4)
6. [Phase 3: Storefront Setup (Weeks 3-4, parallel)](#phase-3-storefront-setup-weeks-3-4-parallel)
7. [Phase 4: Community Infrastructure (Week 4)](#phase-4-community-infrastructure-week-4)
8. [Phase 5: Pre-Launch (Week 5)](#phase-5-pre-launch-week-5)
9. [Phase 6: Launch Day](#phase-6-launch-day)
10. [Phase 7: Ongoing Monthly Operations](#phase-7-ongoing-monthly-operations)
11. [File Delivery & Digital Product Logistics](#file-delivery--digital-product-logistics)
12. [Legal & Licensing](#legal--licensing)
13. [Financial Projections & Break-Even](#financial-projections--break-even)
14. [Tool Stack & Costs](#tool-stack--costs)
15. [Reference: Existing Documents](#reference-existing-documents)

---

## Business Model Summary

**What you're selling:** AI-generated 3D fantasy models (characters, creatures, weapons, props, environments) created with Meshy.ai, delivered as digital files (OBJ, FBX, GLB, STL).

**How you're selling it:** Subscription-based. Monthly themed drops of 8-12 models. 3 tiers ($5/$10/$25) with a future $50 tier.

**Revenue model:** Recurring monthly subscriptions, supplemented by:
- Free models on Thangs/MyMiniFactory as lead magnets
- Back-catalog access as a retention mechanic
- Commercial licensing at the $25 tier
- Loyalty rewards to reduce churn

**Target audience:** Tabletop gamers, 3D printing hobbyists, indie game devs, digital artists, D&D players, miniature painters.

---

## Platform Comparison — Where to Sell

Since everything is digital files, you have multiple platform options. Here's how they compare:

### Option A: Patreon (RECOMMENDED for primary)

| Factor | Details |
|---|---|
| **Model** | Monthly subscription tiers |
| **Cut** | 8% (Pro plan) or 12% (Premium plan) + payment processing (~2.9% + $0.30) |
| **File delivery** | Upload attachments directly to posts; patron-only posts gated by tier |
| **Strengths** | Purpose-built for recurring creative subscriptions; built-in audience discovery; patrons expect to subscribe monthly; community features (polls, DMs) |
| **Weaknesses** | No built-in storefront for one-time purchases; limited customization; you don't own the platform |
| **Setup time** | 1-2 days |

### Option B: Gumroad (RECOMMENDED for one-time sales supplement)

| Factor | Details |
|---|---|
| **Model** | One-time purchases AND memberships |
| **Cut** | 10% flat fee on all transactions |
| **File delivery** | Automatic download links on purchase; supports large ZIP files |
| **Strengths** | Dead simple setup; great for individual bundle sales; good for selling back-catalog packs; supports "pay what you want" |
| **Weaknesses** | Less community feel than Patreon; membership features are basic; higher per-transaction fee for small amounts |
| **Setup time** | 1 day |

### Option C: Ko-fi (Alternative to Patreon)

| Factor | Details |
|---|---|
| **Model** | Subscriptions, one-time purchases, and tips |
| **Cut** | 0% on tips/donations; 5% on Ko-fi Shop sales (Gold plan: $6/month removes all fees) |
| **File delivery** | Upload digital products directly; gated posts for subscribers |
| **Strengths** | Lowest fees in the space; supports both subscription and one-time sales in one place; no minimum payout |
| **Weaknesses** | Smaller built-in audience than Patreon; less brand recognition; discovery is weaker |
| **Setup time** | 1 day |

### Option D: MyMiniFactory Tribes

| Factor | Details |
|---|---|
| **Model** | Monthly subscription (Tribe) + individual model sales |
| **Cut** | ~15-20% depending on terms |
| **File delivery** | Built-in 3D model viewer and download system |
| **Strengths** | Purpose-built for 3D printing models; built-in audience of 3D printing enthusiasts; model preview/viewer; community already looking for STL files |
| **Weaknesses** | Higher cut; more competitive; less control over branding; audience expects print-ready STLs specifically |
| **Setup time** | 2-3 days (approval process) |

### Option E: Thangs (Free distribution / lead magnet only)

| Factor | Details |
|---|---|
| **Model** | Free downloads with optional tipping |
| **Cut** | Free |
| **File delivery** | Direct download |
| **Strengths** | Great for SEO and organic discovery; 3D model search engine; drives traffic to your paid platforms |
| **Weaknesses** | Not a revenue platform; no subscription model |
| **Setup time** | 1 day |

### Option F: Your Own Website (Shopify / WooCommerce / Lemonsqueezy)

| Factor | Details |
|---|---|
| **Model** | Full control — subscriptions, bundles, one-time sales |
| **Cut** | Shopify: $39/month + 2.9% + $0.30; WooCommerce: hosting costs + payment processing; Lemonsqueezy: 5% + processing |
| **File delivery** | Automatic via plugins/integrations; full control over the experience |
| **Strengths** | You own everything; full branding; no platform risk; can build email list; SEO benefits |
| **Weaknesses** | More setup work; no built-in audience; you drive all your own traffic; more to manage |
| **Setup time** | 1-2 weeks |

---

## Recommended Stack

Use a **multi-platform approach** — each platform serves a different purpose:

| Platform | Role | Priority |
|---|---|---|
| **Patreon** | Primary subscription platform. All monthly drops live here. | MUST HAVE — set up first |
| **Gumroad** | One-time sales for individual bundles and back-catalog packs. Catches people who won't commit to a subscription. | HIGH — set up in Month 2 |
| **Thangs** | Free model distribution. Lead magnet. SEO. Every monthly drop includes 1 free model here with links back to Patreon. | MUST HAVE — set up at launch |
| **MyMiniFactory** | Secondary free distribution + potential Tribe subscription later. | MEDIUM — set up in Month 2-3 |
| **Ko-fi** | Optional alternative tip jar and secondary shop if you want redundancy. | LOW — only if Patreon has issues |
| **Own website** | Long-term play. Set up a simple landing page initially, build out a full shop once revenue justifies it. | FUTURE — Month 6+ |

---

## Phase 1: Foundation (Weeks 1-2)

### Week 1: Accounts, Branding & Tools

**Day 1-2: Create accounts**
- [ ] Create Patreon account (use a dedicated email for the business)
- [ ] Create Meshy.ai account and choose a plan (Pro recommended for 4 retries per prompt)
- [ ] Create Thangs account
- [ ] Create TikTok account with business bio (see SOCIAL_MEDIA_STRATEGY.md)
- [ ] Create Instagram account with business bio
- [ ] Create YouTube channel
- [ ] Create Discord server (basic structure, channels come later)
- [ ] Create a dedicated Gmail/Outlook for HyFable business communications

**Day 3-4: Branding**
- [ ] Design or commission a HyFable logo (Canva, Fiverr, or AI-generated)
- [ ] Create a consistent color palette (dark fantasy theme: deep purples, golds, dark greys)
- [ ] Create a Patreon banner image (2560x424 px recommended)
- [ ] Create a profile image/avatar (round crop, recognizable at small size)
- [ ] Create social media header images (same brand, sized per platform)
- [ ] Choose 2-3 brand fonts for text overlays in videos

**Day 5-7: Tool setup**
- [ ] Install OBS Studio for screen recording turntables
- [ ] Install CapCut for video editing
- [ ] Set up Canva account for graphics and lore cards
- [ ] Set up Linktree or Beacons with: Patreon (top), Free model (Thangs), Discord, YouTube
- [ ] Set up a cloud storage folder (Google Drive or Dropbox) organized as:
  ```
  HyFable/
  ├── Models/
  │   ├── Month-1-Mortal-Realm/
  │   │   ├── Prompt-02-Elven-Ranger/
  │   │   │   ├── retry-1/ (OBJ, FBX, GLB, STL)
  │   │   │   ├── retry-2/
  │   │   │   ├── retry-3/
  │   │   │   ├── retry-4/
  │   │   │   └── SELECTED/ (the winner, healed + refined)
  │   │   └── ...
  │   └── Month-2-Rise-of-Darkness/
  ├── Videos/
  │   ├── Raw-Turntables/
  │   ├── Edited/
  │   └── Thumbnails/
  ├── Graphics/
  │   ├── Lore-Cards/
  │   ├── Banners/
  │   └── Promo/
  └── Delivery-ZIPs/
      ├── Month-1-Adventurer-Tier.zip
      ├── Month-1-Champion-Tier.zip
      └── Month-1-Merchant-Lord-Tier.zip
  ```

### Week 2: Meshy.ai Workflow Mastery

**Before generating any production models, do test runs:**
- [ ] Generate 3-5 test prompts to learn the Meshy.ai interface
- [ ] Practice all 4 retries on a single prompt — compare results
- [ ] Practice Smart Healing on a test model
- [ ] Practice Texture Richness adjustments
- [ ] Export in all 4 formats (OBJ, FBX, GLB, STL) and verify files open correctly
- [ ] Test STL files in a slicer (if targeting 3D printing community)
- [ ] Document any prompt tweaks that improve output quality
- [ ] Time yourself: how long does it take to fully process one prompt (all 4 retries + healing + export)?

**Estimated time per prompt:** 15-30 minutes (generation + comparison + healing + export)
**Estimated time for 10-prompt monthly drop:** 3-5 hours

---

## Phase 2: Content Production (Weeks 3-4)

### Generate Month 1: "The Mortal Realm"

Use the prompts from MESHY_AI_PROMPTS.md. Month 1 uses prompts: 2, 6, 17, 18, 19, 21, 36, 41, 43, 44.

**For each prompt:**
1. [ ] Generate all 4 retries
2. [ ] Screenshot or screen-record all 4 results side by side
3. [ ] Select the best mesh + texture combination
4. [ ] Run Smart Healing on the winner
5. [ ] Adjust Texture Richness if needed
6. [ ] Export winner in OBJ, FBX, GLB, and STL formats
7. [ ] Save the 2nd-best retry separately (for future variant releases)
8. [ ] Record a 15-30 second turntable video of the final model
9. [ ] Write a 2-3 sentence lore card (model name + backstory)
10. [ ] Save rejected retries for behind-the-scenes content

### Generate the Welcome Pack (5 bonus models)

The Welcome Pack is for new Champion+ subscribers — instant gratification on day one. Pick 5 of the simpler, more universally appealing prompts to generate ahead of time:

Recommended Welcome Pack models (not from Month 1, so they feel like a bonus):
- [ ] Prompt 33 — Holy Avenger Sword
- [ ] Prompt 37 — Cursed Battleaxe
- [ ] Prompt 44 — Alchemist's Potion Set (if not used in Month 1, swap for Prompt 45)
- [ ] Prompt 47 — Soul Lantern
- [ ] Prompt 40 — Enchanted Crown

### Prepare video content for launch week

- [ ] Edit 5-7 short-form videos (15-30 sec each) from Month 1 turntables
- [ ] Create 2-3 "process" videos showing prompt → retries → final selection
- [ ] Create lore card graphics for each of the 10 models
- [ ] Write captions with hashtags for each video (see SOCIAL_MEDIA_STRATEGY.md)
- [ ] Schedule 1 week of content in advance using Later, Buffer, or native scheduling

---

## Phase 3: Storefront Setup (Weeks 3-4, parallel)

### Patreon Page Setup (Primary — do this first)

**Page settings:**
- [ ] Choose page name: "HyFable" (or "HyFable 3D Models")
- [ ] Upload banner image and profile image
- [ ] Set page URL: patreon.com/hyfable (or similar)
- [ ] Choose category: "3D Art" or "Gaming"
- [ ] Select the Pro plan ($8/month or 8% of earnings, whichever model is current)

**About section — write this:**
```
HyFable is a monthly subscription delivering fantasy 3D models for tabletop
gaming, 3D printing, game development, and digital art collections.

Every month, we release a themed drop of 8-12 handcrafted 3D models — heroes,
villains, creatures, weapons, armor, and environment pieces — all set in the
interconnected world of HyFable.

Each model is generated using cutting-edge AI (Meshy.ai), hand-selected from
multiple iterations, refined with texture healing, and exported in multiple
formats: OBJ, FBX, GLB, and print-ready STL.

Whether you're building a D&D campaign, populating a game world, or growing a
miniature collection, HyFable gives you a steady stream of high-quality fantasy
assets with lore and story baked in.

Join the adventure.
```

**Tier setup** (full details in PATREON_TIERS.md):
- [ ] Create Tier 1: Adventurer — $5/month
- [ ] Create Tier 2: Champion — $10/month
- [ ] Create Tier 3: Merchant Lord — $25/month
- [ ] Add tier images (unique graphic for each tier)
- [ ] Add tier benefits as bullet points exactly as described in PATREON_TIERS.md

**First posts (draft before launch, publish on launch day):**
- [ ] Welcome post (public) — introduces HyFable, the mission, what subscribers get
- [ ] Month 1 drop post (patron-only, Champion+) — full model set with download links
- [ ] Month 1 curated post (patron-only, Adventurer+) — 3-4 selected models
- [ ] Welcome Pack post (patron-only, Champion+) — 5 bonus models, always pinned
- [ ] Behind-the-scenes post (patron-only, Adventurer+) — prompt iteration process
- [ ] Community poll (patron-only) — vote for Month 2 content

**File organization on Patreon:**
- Upload ZIP files per tier per month
- Name clearly: `HyFable-Month1-MortalRealm-Champion.zip`
- Include a README.txt inside each ZIP with: model list, format descriptions, license terms, and a thank-you note

### Thangs Setup (Lead magnet — do this at launch)

- [ ] Create Thangs account/profile with HyFable branding
- [ ] Upload the Month 1 free model: Prompt 19 — Tavern Keeper (Brok Alebeard)
- [ ] Write a description with lore + "Full collection available on Patreon: [link]"
- [ ] Tag appropriately: fantasy, miniature, tabletop, 3D printable, STL, RPG, NPC
- [ ] Include format files: OBJ + GLB + STL

### Gumroad Setup (Month 2 — one-time sales)

- [ ] Create Gumroad account with HyFable branding
- [ ] Set up product: "Month 1: The Mortal Realm — Complete Pack" ($15 one-time)
- [ ] Set up product: "Welcome Pack — 5 Fantasy Models" ($8 one-time)
- [ ] Set up product: "Legendary Armory — 6 Fantasy Weapons" ($12 one-time)
- [ ] Each product: upload ZIP, write description, add preview images, set price
- [ ] Add a note on each product: "Save money with a Patreon subscription — get this and more every month"

**Gumroad pricing strategy:** Price individual bundles at 1.5-2x what a single month of Patreon would cost. This makes the subscription feel like the obvious deal, while still capturing revenue from one-time buyers.

---

## Phase 4: Community Infrastructure (Week 4)

### Discord Server Setup

**Server name:** HyFable

**Channel structure:**
```
WELCOME
  #rules-and-info
  #announcements
  #role-selection

GENERAL
  #general-chat
  #introductions
  #show-your-prints (patrons share their printed models)

HYFABLE MODELS
  #monthly-drops (locked: Champion+)
  #model-requests (locked: Champion+)
  #merchant-network (locked: Merchant Lord)

COMMUNITY
  #polls-and-votes
  #lore-discussion
  #off-topic

SUPPORT
  #file-help
  #printing-tips
```

**Roles (tied to Patreon via Patreon-Discord integration):**
- [ ] Adventurer (green) — auto-assigned to $5 tier
- [ ] Champion (blue) — auto-assigned to $10 tier
- [ ] Merchant Lord (gold) — auto-assigned to $25 tier
- [ ] Founding Member (special, manual) — for anyone who joins in Month 1

**Setup steps:**
- [ ] Create server with the above channels
- [ ] Connect Patreon to Discord (Patreon Settings > Apps > Discord)
- [ ] Set up role-gated channels (Champion+ channels require Champion role or higher)
- [ ] Write #rules-and-info with community guidelines
- [ ] Post a welcome message in #announcements
- [ ] Add the HyFable bot avatar / server icon

---

## Phase 5: Pre-Launch (Week 5)

### The week before launch day

**Content seeding (build anticipation):**
- [ ] Day -7: Post first TikTok/Reel — a teaser turntable of one Month 1 model with "Coming soon" text
- [ ] Day -6: Post a "process" video — showing a prompt being typed into Meshy and the result
- [ ] Day -5: Post a second model reveal
- [ ] Day -4: Post a lore card for one of the characters
- [ ] Day -3: Post a "4 retries, 1 winner" comparison video
- [ ] Day -2: Post the full Month 1 theme reveal — "The Mortal Realm"
- [ ] Day -1: Post a countdown — "Tomorrow. Link in bio." with a montage of all 10 models

**Reddit seeding:**
- [ ] Post 1-2 models to r/3Dprinting or r/PrintedMinis (no Patreon link in the post — just the model; link only in comments when asked)
- [ ] Post a "process" post to r/3Dprinting showing the AI generation workflow

**Email/DM outreach:**
- [ ] If you have any existing audience or contacts, send a personal heads-up
- [ ] Reach out to 2-3 small 3D printing content creators for potential cross-promotion

### Final pre-launch checklist

- [ ] All Month 1 models generated, healed, exported, and organized
- [ ] Welcome Pack ready (5 models in ZIP)
- [ ] Patreon page complete with all tiers, about section, and banner
- [ ] All Patreon posts drafted and ready to publish
- [ ] Thangs profile ready with free model uploaded
- [ ] Discord server created and Patreon integration connected
- [ ] Linktree/Beacons page set up with all links
- [ ] Social media bios updated with Linktree link
- [ ] 7 days of content scheduled
- [ ] You've tested downloading your own ZIPs to make sure files work

---

## Phase 6: Launch Day

**Launch on a Saturday** (highest engagement day for hobby/creative content).

### Launch day timeline

**Morning (8-9 AM EST):**
- [ ] Publish all Patreon posts (welcome, Month 1 drop, Welcome Pack, BTS, poll)
- [ ] Publish the free model on Thangs (if not already live)
- [ ] Open the Discord to the public

**Mid-morning (10-11 AM EST):**
- [ ] Post the hero model reveal video on TikTok, YouTube Shorts, and Instagram Reels simultaneously
- [ ] Post an announcement on Instagram Stories
- [ ] Post to Reddit (r/3Dprinting, r/PrintedMinis) — share a model, mention Patreon only in comments

**Afternoon (1-3 PM EST):**
- [ ] Reply to every comment on every platform (first-hour engagement is critical for algorithm)
- [ ] Share in Discord #announcements: "We're live!"
- [ ] Post a second piece of content (a different model or a process video)

**Evening (6-8 PM EST):**
- [ ] Post a "thank you" Story/TikTok if you have any subscribers
- [ ] Engage with any Reddit threads that gained traction
- [ ] DM anyone who commented showing interest — thank them, don't hard sell

---

## Phase 7: Ongoing Monthly Operations

### Monthly production cycle (repeat every month)

| Week | Task |
|---|---|
| **Week 1** | Generate all models for the upcoming month (3-5 hours). Select winners. Heal and export. |
| **Week 1** | Record turntable videos for all models. Write lore cards. |
| **Week 2** | Edit videos (4-5 short-form). Create graphics. Package ZIPs per tier. |
| **Week 2** | Draft all Patreon posts. Prepare the free model for Thangs. |
| **Week 3** | Schedule all social media content for the drop week. |
| **Week 3** | Tease the upcoming theme on social media (silhouettes, lore hints). |
| **Week 4 Saturday** | DROP DAY — publish everything. Full social media push. |
| **Week 4** | Engage with comments, track analytics, adjust next month's plan. |

### Monthly content output

| Deliverable | Quantity |
|---|---|
| New 3D models (primary) | 8-12 |
| Model variants (from 2nd-best retries) | Hold for Months 5-6 |
| Short-form videos | 16-20 (4-5 per week) |
| Lore cards | 8-12 (one per model) |
| Patreon posts | 4-6 (drop, BTS, poll, engagement) |
| Free model on Thangs | 1 |
| Reddit posts | 4-8 |

### Monthly analytics check

Review these numbers on the last day of each month:
- [ ] Patreon: new subscribers, churned subscribers, MRR (monthly recurring revenue)
- [ ] TikTok: views per video, follower growth, profile visits
- [ ] Instagram: reach, saves, profile visits
- [ ] YouTube: views, subscriber growth
- [ ] Thangs: downloads on free models
- [ ] Which content pillar performed best? (Reveal, Process, Lore, Engagement, Education)
- [ ] Adjust next month's content mix based on what worked

---

## File Delivery & Digital Product Logistics

### File formats to deliver

| Format | Purpose | Who needs it |
|---|---|---|
| **OBJ** | Universal 3D format. Works in Blender, Unity, Unreal, most software. | Everyone |
| **FBX** | Game engine standard. Better for rigged/animated assets. | Game devs |
| **GLB** | Web-friendly, compact. Good for AR/VR and web viewers. | Digital artists, web devs |
| **STL** | 3D printing standard. Mesh only, no textures. | 3D printing community |

### ZIP packaging per tier

**Adventurer ZIP ($5):**
```
HyFable-Month1-MortalRealm-Adventurer/
├── README.txt (model list, license, links)
├── Brok-Alebeard-TavernKeeper/
│   ├── Brok-Alebeard.obj
│   └── Brok-Alebeard.glb
├── Hooded-Peddler-Merchant/
│   ├── Hooded-Peddler.obj
│   └── Hooded-Peddler.glb
├── Captain-Haldric-Guard/
│   ├── Captain-Haldric.obj
│   └── Captain-Haldric.glb
└── Starweavers-Cap-WizardHat/
    ├── Starweavers-Cap.obj
    └── Starweavers-Cap.glb
```

**Champion ZIP ($10):**
```
HyFable-Month1-MortalRealm-Champion/
├── README.txt
├── [All 10 models]/
│   └── [Each in OBJ, FBX, GLB, and STL]
└── Welcome-Pack/ (only in Month 1 or first subscription month)
    └── [5 bonus models in all formats]
```

**Merchant Lord ZIP ($25):**
```
HyFable-Month1-MortalRealm-MerchantLord/
├── README.txt (includes commercial license terms)
├── [All 10 models in all formats]
├── Promo-Pack/
│   ├── [Turntable GIFs or short videos per model]
│   ├── [Beauty shot renders per model]
│   └── [Social-ready thumbnails]
└── Commercial-License.txt
```

### README.txt template (include in every ZIP)

```
=============================================
HyFable — Month 1: The Mortal Realm
=============================================

Thank you for being a HyFable patron!

MODELS INCLUDED:
1. Sylvari Wayfinder (Elven Ranger)
2. Ronin of the Eastern Pass (Wandering Samurai)
3. Grenn Ironhand, Master Smith (Blacksmith)
[... full list]

FILE FORMATS:
- OBJ: Universal. Works in Blender, Unity, Unreal, and most 3D software.
- FBX: Best for game engines (Unity, Unreal).
- GLB: Web and AR/VR friendly. Compact.
- STL: 3D printing. Mesh geometry only (no textures baked in).

LICENSE:
- Adventurer & Champion tiers: Personal use only.
  You may use these models in personal projects, game jams,
  and non-commercial work. You may NOT resell the digital files
  or physical prints.

- Merchant Lord tier: Commercial license included.
  You may sell physical prints made from these models.
  You may NOT resell or redistribute the digital files themselves.
  See Commercial-License.txt for full terms.

NEED HELP?
- Discord: [link]
- Patreon messages: [link]

Made with Meshy.ai + love.
— HyFable
=============================================
```

---

## Legal & Licensing

### What you need

- [ ] **Commercial License for Meshy.ai outputs:** Verify your Meshy.ai plan includes commercial usage rights for generated models. Most paid plans do. Confirm this before selling.
- [ ] **Terms of Service / License Agreement:** Write a clear license document. Two versions:
  - Personal License (Adventurer + Champion): personal/non-commercial use only
  - Commercial License (Merchant Lord): physical prints for sale, no digital resale
- [ ] **Patreon Terms:** Patreon allows digital product delivery. No special setup needed.
- [ ] **Trademark:** Not required at launch, but consider registering "HyFable" if the brand grows.
- [ ] **Tax:** If in the US, you'll receive a 1099 from Patreon and Gumroad if you earn over $600/year. Track expenses (Meshy subscription, software, etc.) for deductions. Consider consulting a tax professional once revenue exceeds $1,000/month.

### Commercial License template (for Merchant Lord tier)

```
HYFABLE COMMERCIAL LICENSE

Effective Date: [Month/Year of subscription]
Licensor: HyFable
Licensee: [Patron's Patreon username]

GRANT:
You are granted a non-exclusive, non-transferable license to:
- Create physical reproductions (3D prints, casts, molds) of HyFable models
- Sell those physical reproductions commercially

RESTRICTIONS:
- You may NOT resell, redistribute, sublicense, or share the digital
  files (OBJ, FBX, GLB, STL) in any form
- You may NOT claim the models as your own original creation
- You may NOT use the models in NFT or blockchain projects
- This license is valid only while your Merchant Lord subscription is active
- This license covers only models released during your active subscription period

ATTRIBUTION:
Attribution is appreciated but not required. If you credit,
use: "Model by HyFable (patreon.com/hyfable)"

TERMINATION:
This license terminates immediately if your subscription lapses.
Models downloaded during your active subscription remain licensed
for commercial physical prints indefinitely, but no new models
are covered after cancellation.
```

---

## Financial Projections & Break-Even

### Costs

| Item | Monthly Cost | Notes |
|---|---|---|
| Meshy.ai Pro | ~$20-30/month | Verify current pricing; need enough credits for 40-48 retries/month (10-12 prompts x 4) |
| Patreon Pro | 8% of revenue | Deducted automatically |
| Payment processing | ~3% of revenue | Deducted automatically |
| Canva Pro (optional) | $13/month | Free tier may be sufficient |
| Later/Buffer (optional) | $0-18/month | Free tier may be sufficient |
| Domain name (future) | ~$12/year | When you build a website |
| **Total fixed costs** | **~$33-61/month** | |

### Break-even scenarios

| Scenario | Subscribers | Monthly Revenue | After Patreon Cut (~11%) | After Costs (~$40) | Net Profit |
|---|---|---|---|---|---|
| Minimum viable | 5 Adventurer + 3 Champion | $55 | $49 | $9 | Barely breaking even |
| Early traction | 15 Adventurer + 10 Champion + 2 Merchant | $225 | $200 | $160 | Covers costs + small profit |
| Sustainable | 30 Adventurer + 30 Champion + 5 Merchant | $575 | $512 | $472 | Real income territory |
| Goal (Month 6) | 50 Adventurer + 80 Champion + 15 Merchant | $1,425 | $1,268 | $1,228 | Strong side income |

### Revenue acceleration options

- **Gumroad one-time sales:** Sell past month bundles for $12-20 each. Even 10-20 sales/month adds $120-400.
- **Bundle sales:** "Complete Collection" bundles at premium pricing ($40-60) after 6 months.
- **Seasonal promotions:** Black Friday, holiday sales on Gumroad at 25% off.
- **Affiliate/referral:** Offer existing patrons a free month for every 3 referrals.

---

## Tool Stack & Costs

| Tool | Purpose | Cost | Priority |
|---|---|---|---|
| Meshy.ai | 3D model generation | ~$20-30/mo | Required |
| Patreon | Subscription platform | 8% of revenue | Required |
| Thangs | Free model distribution | Free | Required |
| Discord | Community | Free | Required |
| TikTok | Discovery / organic growth | Free | Required |
| Instagram | Brand presence | Free | Required |
| YouTube | Long-term content | Free | Required |
| OBS Studio | Screen recording | Free | Required |
| CapCut | Video editing | Free | Required |
| Canva | Graphics / lore cards | Free (Pro: $13/mo) | Required (free tier ok) |
| Linktree/Beacons | Link-in-bio | Free | Required |
| Gumroad | One-time sales | 10% of sales | Month 2+ |
| MyMiniFactory | Secondary distribution | Free to upload | Month 2-3 |
| Later/Buffer | Social scheduling | Free (paid: $18/mo) | Nice to have |
| Google Drive/Dropbox | File storage & organization | Free (up to limit) | Required |

---

## Reference: Existing Documents

These documents in this repository contain the detailed specifics for each area:

| Document | Contents |
|---|---|
| **MESHY_AI_PROMPTS.md** | All 50 prompts organized by category, with best practices and production workflow |
| **PATREON_TIERS.md** | Full tier descriptions, pricing rationale, milestone rewards, and launch checklist |
| **CONTENT_CALENDAR.md** | 6-month themed content calendar mapping all 50 prompts to monthly drops with lore, social strategy, and retry usage plan |
| **SOCIAL_MEDIA_STRATEGY.md** | Platform priority, content pillars, posting schedule, hashtag strategy, Reddit approach, growth milestones, and success metrics |

---

## Quick-Start Summary (If You Only Read This)

1. **Week 1-2:** Create accounts (Patreon, Meshy, Thangs, socials, Discord). Set up branding. Learn Meshy.ai.
2. **Week 3-4:** Generate all Month 1 models (10 models + Welcome Pack). Record turntable videos. Set up Patreon page with tiers and posts. Set up Discord.
3. **Week 5:** Pre-launch content blitz — post daily teasers on TikTok/Reels for 7 days.
4. **Week 5 Saturday:** LAUNCH. Publish everything. Go live. Push hard on social.
5. **Month 2+:** Follow the monthly production cycle. Add Gumroad for one-time sales. Follow the content calendar (CONTENT_CALENDAR.md). Follow the social strategy (SOCIAL_MEDIA_STRATEGY.md).
6. **Month 6:** Evaluate. If 150+ subscribers, consider adding the $50 Patron Saint tier and building a custom website.

**The single most important rule: Never launch empty.** Have Month 1 fully ready before you publish anything on Patreon. People subscribe for what they can get today, not what you promise tomorrow.
