# RVSwaps — Smart Contract Architecture

---

## 1. Contract Overview

Four core contracts, deployed on Polygon PoS:

```
┌─────────────────────────────────────────────────────────┐
│                    CONTRACT SYSTEM                       │
│                                                         │
│  ┌──────────────┐       ┌──────────────────────┐       │
│  │  RVToken     │◄──────│  RVEscrow            │       │
│  │  (ERC-20)    │       │  (marketplace escrow) │       │
│  │              │       │                      │       │
│  │  - mint      │       │  - deposit           │       │
│  │  - burn      │       │  - release           │       │
│  │  - transfer  │       │  - refund            │       │
│  │  - approve   │       │  - dispute resolve   │       │
│  └──────┬───────┘       └──────────────────────┘       │
│         │                                               │
│         │               ┌──────────────────────┐       │
│         ├──────────────►│  RVStaking           │       │
│         │               │  (tiered staking)    │       │
│         │               │                      │       │
│         │               │  - stake             │       │
│         │               │  - unstake           │       │
│         │               │  - claimRewards      │       │
│         │               └──────────────────────┘       │
│         │                                               │
│         │               ┌──────────────────────┐       │
│         └──────────────►│  RVGovernance        │       │
│                         │  (future DAO)        │       │
│                         │                      │       │
│                         │  - propose           │       │
│                         │  - vote              │       │
│                         │  - execute           │       │
│                         └──────────────────────┘       │
│                                                         │
│  ┌──────────────────────────────────────────────┐      │
│  │  ProxyAdmin (upgradeable proxy pattern)      │      │
│  │  All contracts deployed behind TransparentProxy │   │
│  └──────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────┘
```

All contracts use OpenZeppelin libraries and are deployed behind **TransparentUpgradeableProxy** to allow bug fixes without redeployment.

---

## 2. RVToken Contract

### Interface

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts-upgradeable/token/ERC20/ERC20Upgradeable.sol";
import "@openzeppelin/contracts-upgradeable/token/ERC20/extensions/ERC20BurnableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/token/ERC20/extensions/ERC20PermitUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/access/AccessControlUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";
import "@openzeppelin/contracts-upgradeable/utils/PausableUpgradeable.sol";

contract RVToken is
    Initializable,
    ERC20Upgradeable,
    ERC20BurnableUpgradeable,
    ERC20PermitUpgradeable,
    AccessControlUpgradeable,
    PausableUpgradeable
{
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");

    uint256 public constant MAX_SUPPLY = 1_000_000_000 * 10**18; // 1 billion
    uint256 public constant BURN_FLOOR = 500_000_000 * 10**18;   // 50% floor
    uint256 public constant BURN_STOP  = 250_000_000 * 10**18;   // 25% hard stop

    uint256 public totalBurned;

    event TokensBurned(address indexed from, uint256 amount, string reason);

    function initialize(address admin) public initializer {
        __ERC20_init("ReValue Coin", "RV");
        __ERC20Burnable_init();
        __ERC20Permit_init("ReValue Coin");
        __AccessControl_init();
        __Pausable_init();

        _grantRole(DEFAULT_ADMIN_ROLE, admin);
        _grantRole(MINTER_ROLE, admin);
        _grantRole(PAUSER_ROLE, admin);
    }

    /// @notice Mint tokens (for initial distribution and staking rewards)
    function mint(address to, uint256 amount) external onlyRole(MINTER_ROLE) {
        require(totalSupply() + amount <= MAX_SUPPLY, "Exceeds max supply");
        _mint(to, amount);
    }

    /// @notice Platform-triggered burn with reason tracking
    function platformBurn(
        address from,
        uint256 amount,
        string calldata reason
    ) external onlyRole(BURNER_ROLE) {
        require(totalSupply() - amount >= BURN_STOP, "Below burn stop threshold");
        _burn(from, amount);
        totalBurned += amount;
        emit TokensBurned(from, amount, reason);
    }

    /// @notice Calculate effective burn amount (halved below BURN_FLOOR)
    function effectiveBurnAmount(uint256 amount) public view returns (uint256) {
        if (totalSupply() <= BURN_STOP) return 0;
        if (totalSupply() <= BURN_FLOOR) return amount / 2;
        return amount;
    }

    function pause() external onlyRole(PAUSER_ROLE) { _pause(); }
    function unpause() external onlyRole(PAUSER_ROLE) { _unpause(); }

    function _update(
        address from,
        address to,
        uint256 value
    ) internal override whenNotPaused {
        super._update(from, to, value);
    }
}
```

### Key Design Decisions

- **Upgradeable**: Critical for a production system. Bugs in token contracts are catastrophic. Proxy pattern allows fixing logic while preserving state.
- **Role-based access**: Separate MINTER, BURNER, PAUSER roles. The escrow contract gets BURNER_ROLE. No single key controls everything.
- **Burn floor**: Prevents runaway deflation. Below 50% supply, burns halve. Below 25%, burns stop. This ensures the token remains liquid enough for marketplace use.
- **ERC20Permit**: Enables gasless approvals (EIP-2612). Users can approve token spending via signature instead of a separate transaction.
- **Pausable**: Emergency circuit breaker if an exploit is detected.

---

## 3. RVEscrow Contract

### Interface

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts-upgradeable/access/AccessControlUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/ReentrancyGuardUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

interface IRVToken {
    function transferFrom(address, address, uint256) external returns (bool);
    function transfer(address, uint256) external returns (bool);
    function platformBurn(address, uint256, string calldata) external;
    function effectiveBurnAmount(uint256) external view returns (uint256);
    function balanceOf(address) external view returns (uint256);
}

contract RVEscrow is
    Initializable,
    AccessControlUpgradeable,
    ReentrancyGuardUpgradeable
{
    bytes32 public constant OPERATOR_ROLE = keccak256("OPERATOR_ROLE");
    bytes32 public constant ARBITER_ROLE  = keccak256("ARBITER_ROLE");

    IRVToken public rvToken;
    address public feeRecipient;

    uint256 public burnBps;       // burn rate in basis points (100 = 1%)
    uint256 public platformFeeBps; // platform fee in basis points

    enum EscrowStatus { NONE, FUNDED, RELEASED, REFUNDED, DISPUTED, RESOLVED }

    struct Escrow {
        bytes32 orderId;
        address buyer;
        address seller;
        uint256 totalAmount;      // total deposited by buyer
        uint256 burnAmount;       // tokens to burn
        uint256 feeAmount;        // platform fee
        uint256 sellerAmount;     // net amount for seller
        EscrowStatus status;
        uint64 fundedAt;
        uint64 autoReleaseAt;     // auto-release timestamp
    }

    mapping(bytes32 => Escrow) public escrows;

    event EscrowCreated(bytes32 indexed orderId, address buyer, address seller, uint256 amount);
    event EscrowReleased(bytes32 indexed orderId, uint256 sellerAmount, uint256 burned);
    event EscrowRefunded(bytes32 indexed orderId, uint256 refundAmount);
    event EscrowDisputed(bytes32 indexed orderId, address disputedBy);
    event DisputeResolved(bytes32 indexed orderId, uint256 buyerAmount, uint256 sellerAmount);

    function initialize(
        address _rvToken,
        address _feeRecipient,
        uint256 _burnBps,
        uint256 _platformFeeBps
    ) public initializer {
        __AccessControl_init();
        __ReentrancyGuard_init();

        rvToken = IRVToken(_rvToken);
        feeRecipient = _feeRecipient;
        burnBps = _burnBps;           // default: 100 (1%)
        platformFeeBps = _platformFeeBps; // default: 250 (2.5%)

        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
    }

    /// @notice Buyer deposits tokens into escrow for an order
    function deposit(
        bytes32 orderId,
        address seller,
        uint256 amount
    ) external nonReentrant {
        require(escrows[orderId].status == EscrowStatus.NONE, "Order exists");
        require(amount > 0, "Zero amount");
        require(seller != address(0) && seller != msg.sender, "Invalid seller");

        uint256 burnAmt = (amount * burnBps) / 10000;
        burnAmt = rvToken.effectiveBurnAmount(burnAmt);
        uint256 feeAmt = (amount * platformFeeBps) / 10000;
        uint256 sellerAmt = amount - burnAmt - feeAmt;

        // Transfer full amount from buyer to this contract
        require(rvToken.transferFrom(msg.sender, address(this), amount), "Transfer failed");

        escrows[orderId] = Escrow({
            orderId: orderId,
            buyer: msg.sender,
            seller: seller,
            totalAmount: amount,
            burnAmount: burnAmt,
            feeAmount: feeAmt,
            sellerAmount: sellerAmt,
            status: EscrowStatus.FUNDED,
            fundedAt: uint64(block.timestamp),
            autoReleaseAt: uint64(block.timestamp + 14 days) // auto-release after 14 days
        });

        emit EscrowCreated(orderId, msg.sender, seller, amount);
    }

    /// @notice Release escrow to seller (called by operator after delivery confirmed)
    function release(bytes32 orderId) external nonReentrant onlyRole(OPERATOR_ROLE) {
        Escrow storage e = escrows[orderId];
        require(e.status == EscrowStatus.FUNDED, "Not funded");

        e.status = EscrowStatus.RELEASED;

        // Burn tokens
        if (e.burnAmount > 0) {
            rvToken.platformBurn(address(this), e.burnAmount, "purchase_burn");
        }

        // Platform fee
        if (e.feeAmount > 0) {
            require(rvToken.transfer(feeRecipient, e.feeAmount), "Fee transfer failed");
        }

        // Seller payment
        require(rvToken.transfer(e.seller, e.sellerAmount), "Seller transfer failed");

        emit EscrowReleased(orderId, e.sellerAmount, e.burnAmount);
    }

    /// @notice Auto-release if buyer hasn't confirmed or disputed after deadline
    function autoRelease(bytes32 orderId) external nonReentrant {
        Escrow storage e = escrows[orderId];
        require(e.status == EscrowStatus.FUNDED, "Not funded");
        require(block.timestamp >= e.autoReleaseAt, "Too early");

        e.status = EscrowStatus.RELEASED;

        if (e.burnAmount > 0) {
            rvToken.platformBurn(address(this), e.burnAmount, "purchase_burn_auto");
        }
        if (e.feeAmount > 0) {
            require(rvToken.transfer(feeRecipient, e.feeAmount), "Fee transfer failed");
        }
        require(rvToken.transfer(e.seller, e.sellerAmount), "Seller transfer failed");

        emit EscrowReleased(orderId, e.sellerAmount, e.burnAmount);
    }

    /// @notice Full refund to buyer (before shipping, seller cancelled, etc.)
    function refund(bytes32 orderId) external nonReentrant onlyRole(OPERATOR_ROLE) {
        Escrow storage e = escrows[orderId];
        require(e.status == EscrowStatus.FUNDED, "Not funded");

        e.status = EscrowStatus.REFUNDED;
        require(rvToken.transfer(e.buyer, e.totalAmount), "Refund transfer failed");

        emit EscrowRefunded(orderId, e.totalAmount);
    }

    /// @notice Open a dispute (freezes escrow until arbiter resolves)
    function openDispute(bytes32 orderId) external {
        Escrow storage e = escrows[orderId];
        require(e.status == EscrowStatus.FUNDED, "Not funded");
        require(msg.sender == e.buyer || msg.sender == e.seller, "Not a party");

        e.status = EscrowStatus.DISPUTED;
        emit EscrowDisputed(orderId, msg.sender);
    }

    /// @notice Arbiter resolves dispute with custom split
    function resolveDispute(
        bytes32 orderId,
        uint256 buyerPercent  // 0-100, seller gets remainder
    ) external nonReentrant onlyRole(ARBITER_ROLE) {
        Escrow storage e = escrows[orderId];
        require(e.status == EscrowStatus.DISPUTED, "Not disputed");
        require(buyerPercent <= 100, "Invalid percent");

        e.status = EscrowStatus.RESOLVED;

        // Still burn and take fee
        uint256 distributable = e.totalAmount - e.burnAmount - e.feeAmount;
        uint256 buyerAmt = (distributable * buyerPercent) / 100;
        uint256 sellerAmt = distributable - buyerAmt;

        if (e.burnAmount > 0) {
            rvToken.platformBurn(address(this), e.burnAmount, "dispute_burn");
        }
        if (e.feeAmount > 0) {
            require(rvToken.transfer(feeRecipient, e.feeAmount), "Fee transfer failed");
        }
        if (buyerAmt > 0) {
            require(rvToken.transfer(e.buyer, buyerAmt), "Buyer transfer failed");
        }
        if (sellerAmt > 0) {
            require(rvToken.transfer(e.seller, sellerAmt), "Seller transfer failed");
        }

        emit DisputeResolved(orderId, buyerAmt, sellerAmt);
    }

    /// @notice Update burn rate (admin only)
    function setBurnBps(uint256 _burnBps) external onlyRole(DEFAULT_ADMIN_ROLE) {
        require(_burnBps <= 500, "Max 5%");
        burnBps = _burnBps;
    }

    /// @notice Update platform fee rate (admin only)
    function setPlatformFeeBps(uint256 _feeBps) external onlyRole(DEFAULT_ADMIN_ROLE) {
        require(_feeBps <= 1000, "Max 10%");
        platformFeeBps = _feeBps;
    }
}
```

### Key Design Decisions

- **ReentrancyGuard**: Every external function that moves tokens is protected against reentrancy attacks.
- **Auto-release**: If the buyer doesn't confirm or dispute within 14 days, anyone can call `autoRelease` to pay the seller. This prevents funds from being locked forever.
- **Dispute resolution**: The arbiter (platform admin, later a DAO committee) can split funds arbitrarily — 100% to buyer (full refund), 0% to buyer (full release to seller), or any split.
- **Burn still happens on disputes**: Even disputed transactions burn tokens. The burn is a platform tax, not a reward for good behavior.
- **Basis points**: All rates stored as basis points (1 bps = 0.01%) for precision. Max caps prevent admin from setting predatory rates.

---

## 4. RVStaking Contract

### Interface

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts-upgradeable/access/AccessControlUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/ReentrancyGuardUpgradeable.sol";

contract RVStaking is
    Initializable,
    AccessControlUpgradeable,
    ReentrancyGuardUpgradeable
{
    IRVToken public rvToken;

    enum Tier { FLEX, BRONZE, SILVER, GOLD, DIAMOND }

    struct TierConfig {
        uint256 lockDuration;    // lock period in seconds
        uint256 rewardRateBps;   // annual reward rate in basis points
        uint256 earlyPenaltyBps; // penalty for early unstake (basis points of rewards)
    }

    struct StakePosition {
        uint256 amount;
        Tier tier;
        uint64 stakedAt;
        uint64 unlocksAt;
        uint256 rewardDebt;      // for reward calculation
        bool active;
    }

    mapping(Tier => TierConfig) public tierConfigs;
    mapping(address => StakePosition[]) public positions;

    uint256 public totalStaked;
    uint256 public rewardPool;   // tokens available for rewards

    event Staked(address indexed user, uint256 positionId, uint256 amount, Tier tier);
    event Unstaked(address indexed user, uint256 positionId, uint256 amount, uint256 reward);
    event RewardsClaimed(address indexed user, uint256 positionId, uint256 reward);
    event RewardPoolFunded(uint256 amount);

    function initialize(address _rvToken) public initializer {
        __AccessControl_init();
        __ReentrancyGuard_init();

        rvToken = IRVToken(_rvToken);
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);

        // Configure tiers
        tierConfigs[Tier.FLEX]    = TierConfig(0,          300,  5000); // 3%, 50% penalty
        tierConfigs[Tier.BRONZE]  = TierConfig(30 days,    600,  5000); // 6%
        tierConfigs[Tier.SILVER]  = TierConfig(90 days,   1000,  5000); // 10%
        tierConfigs[Tier.GOLD]    = TierConfig(180 days,  1400,  5000); // 14%
        tierConfigs[Tier.DIAMOND] = TierConfig(365 days,  1800,  5000); // 18%
    }

    /// @notice Stake tokens in a specific tier
    function stake(uint256 amount, Tier tier) external nonReentrant {
        require(amount >= 100 * 10**18, "Min stake 100 RV");

        require(rvToken.transferFrom(msg.sender, address(this), amount), "Transfer failed");

        TierConfig memory config = tierConfigs[tier];
        uint256 positionId = positions[msg.sender].length;

        positions[msg.sender].push(StakePosition({
            amount: amount,
            tier: tier,
            stakedAt: uint64(block.timestamp),
            unlocksAt: uint64(block.timestamp + config.lockDuration),
            rewardDebt: 0,
            active: true
        }));

        totalStaked += amount;
        emit Staked(msg.sender, positionId, amount, tier);
    }

    /// @notice Unstake tokens and claim rewards
    function unstake(uint256 positionId) external nonReentrant {
        StakePosition storage pos = positions[msg.sender][positionId];
        require(pos.active, "Not active");

        uint256 reward = _calculateReward(msg.sender, positionId);
        bool early = block.timestamp < pos.unlocksAt;

        if (early) {
            // Early unstake penalty: lose portion of rewards
            TierConfig memory config = tierConfigs[pos.tier];
            reward = reward - (reward * config.earlyPenaltyBps / 10000);
        }

        pos.active = false;
        totalStaked -= pos.amount;

        // Return principal
        require(rvToken.transfer(msg.sender, pos.amount), "Principal transfer failed");

        // Pay rewards from reward pool
        if (reward > 0 && reward <= rewardPool) {
            rewardPool -= reward;
            require(rvToken.transfer(msg.sender, reward), "Reward transfer failed");
        }

        emit Unstaked(msg.sender, positionId, pos.amount, reward);
    }

    /// @notice Claim accumulated rewards without unstaking
    function claimRewards(uint256 positionId) external nonReentrant {
        StakePosition storage pos = positions[msg.sender][positionId];
        require(pos.active, "Not active");

        uint256 reward = _calculateReward(msg.sender, positionId);
        require(reward > 0, "No rewards");
        require(reward <= rewardPool, "Insufficient reward pool");

        pos.rewardDebt += reward;
        rewardPool -= reward;

        require(rvToken.transfer(msg.sender, reward), "Reward transfer failed");
        emit RewardsClaimed(msg.sender, positionId, reward);
    }

    /// @notice Fund the reward pool (called by platform fee distributor)
    function fundRewardPool(uint256 amount) external {
        require(rvToken.transferFrom(msg.sender, address(this), amount), "Transfer failed");
        rewardPool += amount;
        emit RewardPoolFunded(amount);
    }

    /// @notice Calculate pending reward for a position
    function _calculateReward(
        address user,
        uint256 positionId
    ) internal view returns (uint256) {
        StakePosition memory pos = positions[user][positionId];
        TierConfig memory config = tierConfigs[pos.tier];

        uint256 elapsed = block.timestamp - pos.stakedAt;
        uint256 annualReward = (pos.amount * config.rewardRateBps) / 10000;
        uint256 reward = (annualReward * elapsed) / 365 days;

        return reward - pos.rewardDebt;
    }

    /// @notice View pending rewards
    function pendingRewards(
        address user,
        uint256 positionId
    ) external view returns (uint256) {
        return _calculateReward(user, positionId);
    }

    /// @notice Get all positions for a user
    function getPositionCount(address user) external view returns (uint256) {
        return positions[user].length;
    }
}
```

---

## 5. Deployment Strategy

### Network

```
Polygon PoS Mainnet
Chain ID: 137
RPC: Alchemy / QuickNode (redundant)
Explorer: Polygonscan
```

### Deployment Order

```
1. Deploy RVToken (behind proxy)
2. Mint initial supply to deployer
3. Deploy RVEscrow (behind proxy)
4. Deploy RVStaking (behind proxy)
5. Grant BURNER_ROLE on RVToken to RVEscrow
6. Grant MINTER_ROLE on RVToken to RVStaking (for future reward emissions)
7. Distribute tokens per allocation table
8. Lock liquidity tokens in DEX pool
9. Lock team/advisor tokens in vesting contract (OpenZeppelin VestingWallet)
10. Transfer DEFAULT_ADMIN_ROLE to multi-sig (Gnosis Safe)
```

### Multi-Sig Security

All admin operations go through a **Gnosis Safe** multi-sig:
- 3-of-5 signers required
- 24-hour timelock on admin functions
- Signers: 2 founders + 1 advisor + 1 legal + 1 independent security auditor

### Upgrade Policy

- All contracts are upgradeable via TransparentProxy
- Upgrades require multi-sig approval + 48-hour timelock
- Every upgrade must pass audit review
- Post-governance: upgrades require DAO vote

---

## 6. Testing & Audit Plan

### Testing

```
Unit tests:      Hardhat + Chai (100% coverage on critical paths)
Integration:     Hardhat forked mainnet tests
Fuzz testing:    Foundry fuzz tests on token arithmetic
Gas profiling:   Hardhat gas reporter
Testnet deploy:  Polygon Amoy testnet for E2E testing
```

### Audit

- Pre-launch: Two independent audits (e.g., Trail of Bits, OpenZeppelin, or Cyfrin)
- Bug bounty: Immunefi program ($50K-$250K payouts based on severity)
- Post-launch: Continuous monitoring via Forta Network agents

### Known Attack Vectors to Test

1. Reentrancy on escrow release/refund
2. Integer overflow on reward calculations (mitigated by Solidity 0.8+ built-in checks)
3. Front-running escrow deposits
4. Flash loan attacks on staking rewards
5. Griefing via dust deposits
6. Admin key compromise scenarios
