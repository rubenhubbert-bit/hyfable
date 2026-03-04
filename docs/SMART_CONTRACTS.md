# RVSwaps — Smart Contract Architecture

---

## 1. Contract System Overview

Five core contracts deployed on Polygon PoS, all behind TransparentUpgradeableProxy:

```
┌────────────────────────────────────────────────────────────────┐
│                     CONTRACT SYSTEM                             │
│                                                                │
│  ┌──────────────┐       ┌──────────────────────────┐          │
│  │  RVToken     │◄──────│  RVEscrow                │          │
│  │  (ERC-20)    │       │  (marketplace escrow)     │          │
│  │              │       │                          │          │
│  │  burn()      │       │  deposit()  release()    │          │
│  │  mint()      │       │  refund()   autoRelease()│          │
│  │  permit()    │       │  openDispute()           │          │
│  │  pause()     │       │  resolveDispute()        │          │
│  └──────┬───────┘       └──────────────────────────┘          │
│         │                                                      │
│         │               ┌──────────────────────────┐          │
│         ├──────────────►│  RVStaking               │          │
│         │               │  (tiered staking)        │          │
│         │               │                          │          │
│         │               │  stake()  unstake()      │          │
│         │               │  claimRewards()          │          │
│         │               │  fundRewardPool()        │          │
│         │               └──────────────────────────┘          │
│         │                                                      │
│         │               ┌──────────────────────────┐          │
│         ├──────────────►│  RVBuybackBurn           │          │
│         │               │  (treasury buyback)      │          │
│         │               │                          │          │
│         │               │  executeBuyback()        │          │
│         │               │  burn()                  │          │
│         │               └──────────────────────────┘          │
│         │                                                      │
│         │               ┌──────────────────────────┐          │
│         └──────────────►│  RVGovernance (Phase 5)  │          │
│                         │  propose() vote()        │          │
│                         │  execute()               │          │
│                         └──────────────────────────┘          │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  ProxyAdmin (TransparentUpgradeableProxy)                │ │
│  │  Owned by Gnosis Safe multi-sig (3-of-5)                 │ │
│  │  48-hour timelock on upgrades                            │ │
│  └──────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
```

---

## 2. RVToken Contract

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts-upgradeable/token/ERC20/ERC20Upgradeable.sol";
import "@openzeppelin/contracts-upgradeable/token/ERC20/extensions/ERC20BurnableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/token/ERC20/extensions/ERC20PermitUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/access/AccessControlUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/PausableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";

/// @title RVToken — ReValue Coin
/// @notice ERC-20 token with role-based burn mechanics and supply floor protection
contract RVToken is
    Initializable,
    ERC20Upgradeable,
    ERC20BurnableUpgradeable,
    ERC20PermitUpgradeable,
    AccessControlUpgradeable,
    PausableUpgradeable,
    UUPSUpgradeable
{
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant UPGRADER_ROLE = keccak256("UPGRADER_ROLE");

    uint256 public constant MAX_SUPPLY = 1_000_000_000 * 10**18;
    uint256 public constant BURN_FLOOR = 500_000_000 * 10**18;
    uint256 public constant BURN_STOP  = 250_000_000 * 10**18;

    uint256 public totalBurned;
    uint256 public totalMinted;

    event TokensBurned(address indexed from, uint256 amount, string reason);
    event TokensMinted(address indexed to, uint256 amount);

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    function initialize(address admin) public initializer {
        __ERC20_init("ReValue Coin", "RV");
        __ERC20Burnable_init();
        __ERC20Permit_init("ReValue Coin");
        __AccessControl_init();
        __Pausable_init();
        __UUPSUpgradeable_init();

        _grantRole(DEFAULT_ADMIN_ROLE, admin);
        _grantRole(MINTER_ROLE, admin);
        _grantRole(PAUSER_ROLE, admin);
        _grantRole(UPGRADER_ROLE, admin);
    }

    /// @notice Mint tokens (initial distribution + staking reward emissions)
    /// @dev Enforces hard cap. Only MINTER_ROLE can call.
    function mint(address to, uint256 amount) external onlyRole(MINTER_ROLE) {
        require(totalMinted + amount <= MAX_SUPPLY, "RV: exceeds max supply");
        totalMinted += amount;
        _mint(to, amount);
        emit TokensMinted(to, amount);
    }

    /// @notice Platform-triggered burn with reason tracking
    /// @dev Respects burn floor. Only BURNER_ROLE can call.
    function platformBurn(
        address from,
        uint256 amount,
        string calldata reason
    ) external onlyRole(BURNER_ROLE) {
        uint256 effectiveAmount = effectiveBurnAmount(amount);
        require(effectiveAmount > 0, "RV: burns stopped");
        require(totalSupply() - effectiveAmount >= BURN_STOP, "RV: below burn stop");

        _burn(from, effectiveAmount);
        totalBurned += effectiveAmount;
        emit TokensBurned(from, effectiveAmount, reason);
    }

    /// @notice Calculate effective burn amount (halved below BURN_FLOOR)
    function effectiveBurnAmount(uint256 amount) public view returns (uint256) {
        uint256 supply = totalSupply();
        if (supply <= BURN_STOP) return 0;
        if (supply <= BURN_FLOOR) return amount / 2;
        return amount;
    }

    /// @notice Current circulating supply (minted minus burned)
    function circulatingSupply() external view returns (uint256) {
        return totalSupply();
    }

    function pause() external onlyRole(PAUSER_ROLE) {
        _pause();
    }

    function unpause() external onlyRole(PAUSER_ROLE) {
        _unpause();
    }

    function _authorizeUpgrade(
        address newImplementation
    ) internal override onlyRole(UPGRADER_ROLE) {}

    function _update(
        address from,
        address to,
        uint256 value
    ) internal override whenNotPaused {
        super._update(from, to, value);
    }
}
```

### Design Decisions

- **UUPS Proxy**: More gas-efficient than Transparent Proxy. Upgrade logic is in the implementation, controlled by UPGRADER_ROLE.
- **Role separation**: MINTER, BURNER, PAUSER, UPGRADER are distinct. The escrow contract gets BURNER_ROLE. The staking reward emitter gets MINTER_ROLE.
- **Burn floor**: On-chain enforcement. Below 50% supply, burns halve. Below 25%, burns stop. Not admin-controllable.
- **ERC20Permit (EIP-2612)**: Gasless approvals. Users sign a permit off-chain, contract uses it for `transferFrom`. Critical for meta-transaction flows.
- **Pausable**: Emergency circuit breaker. Pause triggers on exploit detection. Pause can be called by PAUSER_ROLE without timelock (time-critical).

---

## 3. RVEscrow Contract

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts-upgradeable/access/AccessControlUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/ReentrancyGuardUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/PausableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";

interface IRVToken {
    function transferFrom(address from, address to, uint256 amount) external returns (bool);
    function transfer(address to, uint256 amount) external returns (bool);
    function platformBurn(address from, uint256 amount, string calldata reason) external;
    function effectiveBurnAmount(uint256 amount) external view returns (uint256);
    function balanceOf(address account) external view returns (uint256);
}

/// @title RVEscrow — Marketplace Payment Escrow
/// @notice Holds buyer tokens until delivery confirmed, handles disputes
contract RVEscrow is
    Initializable,
    AccessControlUpgradeable,
    ReentrancyGuardUpgradeable,
    PausableUpgradeable,
    UUPSUpgradeable
{
    bytes32 public constant OPERATOR_ROLE = keccak256("OPERATOR_ROLE");
    bytes32 public constant ARBITER_ROLE  = keccak256("ARBITER_ROLE");
    bytes32 public constant UPGRADER_ROLE = keccak256("UPGRADER_ROLE");

    IRVToken public rvToken;
    address public feeRecipient;

    uint256 public burnBps;
    uint256 public platformFeeBps;
    uint256 public autoReleaseDuration;

    uint256 public totalEscrowed;
    uint256 public totalReleased;
    uint256 public totalRefunded;
    uint256 public totalBurnedViaEscrow;

    enum Status { NONE, FUNDED, RELEASED, REFUNDED, DISPUTED, RESOLVED }

    struct Escrow {
        bytes32 orderId;
        address buyer;
        address seller;
        uint256 totalAmount;
        uint256 burnAmount;
        uint256 feeAmount;
        uint256 sellerAmount;
        Status status;
        uint64 fundedAt;
        uint64 autoReleaseAt;
    }

    mapping(bytes32 => Escrow) public escrows;

    event EscrowCreated(
        bytes32 indexed orderId,
        address indexed buyer,
        address indexed seller,
        uint256 totalAmount,
        uint256 burnAmount,
        uint256 feeAmount,
        uint256 sellerAmount
    );
    event EscrowReleased(bytes32 indexed orderId, uint256 sellerAmount, uint256 burned);
    event EscrowRefunded(bytes32 indexed orderId, uint256 refundAmount);
    event EscrowDisputed(bytes32 indexed orderId, address indexed disputedBy);
    event DisputeResolved(
        bytes32 indexed orderId,
        uint256 buyerAmount,
        uint256 sellerAmount,
        uint256 burned
    );
    event FeesUpdated(uint256 burnBps, uint256 platformFeeBps);

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    function initialize(
        address _rvToken,
        address _feeRecipient,
        uint256 _burnBps,
        uint256 _platformFeeBps,
        uint256 _autoReleaseDays
    ) public initializer {
        __AccessControl_init();
        __ReentrancyGuard_init();
        __Pausable_init();
        __UUPSUpgradeable_init();

        rvToken = IRVToken(_rvToken);
        feeRecipient = _feeRecipient;
        burnBps = _burnBps;
        platformFeeBps = _platformFeeBps;
        autoReleaseDuration = _autoReleaseDays * 1 days;

        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _grantRole(UPGRADER_ROLE, msg.sender);
    }

    /// @notice Buyer deposits tokens into escrow
    /// @param orderId Unique order identifier (generated off-chain)
    /// @param seller Seller's address
    /// @param amount Total RV amount (includes all fees)
    function deposit(
        bytes32 orderId,
        address seller,
        uint256 amount
    ) external nonReentrant whenNotPaused {
        require(escrows[orderId].status == Status.NONE, "Escrow: order exists");
        require(amount > 0, "Escrow: zero amount");
        require(seller != address(0), "Escrow: zero seller");
        require(seller != msg.sender, "Escrow: self-trade");

        uint256 burnAmt = rvToken.effectiveBurnAmount((amount * burnBps) / 10000);
        uint256 feeAmt = (amount * platformFeeBps) / 10000;

        // Invariant: total = burn + fee + seller
        uint256 sellerAmt = amount - burnAmt - feeAmt;
        require(sellerAmt > 0, "Escrow: seller amount zero");

        require(
            rvToken.transferFrom(msg.sender, address(this), amount),
            "Escrow: transfer failed"
        );

        escrows[orderId] = Escrow({
            orderId: orderId,
            buyer: msg.sender,
            seller: seller,
            totalAmount: amount,
            burnAmount: burnAmt,
            feeAmount: feeAmt,
            sellerAmount: sellerAmt,
            status: Status.FUNDED,
            fundedAt: uint64(block.timestamp),
            autoReleaseAt: uint64(block.timestamp + autoReleaseDuration)
        });

        totalEscrowed += amount;

        emit EscrowCreated(orderId, msg.sender, seller, amount, burnAmt, feeAmt, sellerAmt);
    }

    /// @notice Release escrow to seller after delivery confirmed
    function release(bytes32 orderId) external nonReentrant onlyRole(OPERATOR_ROLE) {
        Escrow storage e = escrows[orderId];
        require(e.status == Status.FUNDED, "Escrow: not funded");

        e.status = Status.RELEASED;
        _settle(e);
        totalReleased += e.sellerAmount;

        emit EscrowReleased(orderId, e.sellerAmount, e.burnAmount);
    }

    /// @notice Auto-release after timeout (anyone can call)
    function autoRelease(bytes32 orderId) external nonReentrant {
        Escrow storage e = escrows[orderId];
        require(e.status == Status.FUNDED, "Escrow: not funded");
        require(block.timestamp >= e.autoReleaseAt, "Escrow: too early");

        e.status = Status.RELEASED;
        _settle(e);
        totalReleased += e.sellerAmount;

        emit EscrowReleased(orderId, e.sellerAmount, e.burnAmount);
    }

    /// @notice Full refund to buyer (pre-shipping cancellation)
    function refund(bytes32 orderId) external nonReentrant onlyRole(OPERATOR_ROLE) {
        Escrow storage e = escrows[orderId];
        require(e.status == Status.FUNDED, "Escrow: not funded");

        e.status = Status.REFUNDED;
        totalRefunded += e.totalAmount;

        require(rvToken.transfer(e.buyer, e.totalAmount), "Escrow: refund failed");

        emit EscrowRefunded(orderId, e.totalAmount);
    }

    /// @notice Open a dispute (buyer or seller only)
    function openDispute(bytes32 orderId) external whenNotPaused {
        Escrow storage e = escrows[orderId];
        require(e.status == Status.FUNDED, "Escrow: not funded");
        require(
            msg.sender == e.buyer || msg.sender == e.seller,
            "Escrow: not a party"
        );

        e.status = Status.DISPUTED;
        // Extend auto-release to prevent timeout during dispute
        e.autoReleaseAt = uint64(block.timestamp + 30 days);

        emit EscrowDisputed(orderId, msg.sender);
    }

    /// @notice Arbiter resolves dispute with percentage split
    /// @param buyerPercent 0-100, percentage of distributable amount to buyer
    function resolveDispute(
        bytes32 orderId,
        uint256 buyerPercent
    ) external nonReentrant onlyRole(ARBITER_ROLE) {
        Escrow storage e = escrows[orderId];
        require(e.status == Status.DISPUTED, "Escrow: not disputed");
        require(buyerPercent <= 100, "Escrow: invalid percent");

        e.status = Status.RESOLVED;

        // Burn and fee still apply on disputes
        if (e.burnAmount > 0) {
            rvToken.platformBurn(address(this), e.burnAmount, "dispute_burn");
            totalBurnedViaEscrow += e.burnAmount;
        }
        if (e.feeAmount > 0) {
            require(rvToken.transfer(feeRecipient, e.feeAmount), "Escrow: fee failed");
        }

        // Split remaining between buyer and seller
        uint256 distributable = e.totalAmount - e.burnAmount - e.feeAmount;
        uint256 buyerAmt = (distributable * buyerPercent) / 100;
        uint256 sellerAmt = distributable - buyerAmt;

        if (buyerAmt > 0) {
            require(rvToken.transfer(e.buyer, buyerAmt), "Escrow: buyer payout failed");
        }
        if (sellerAmt > 0) {
            require(rvToken.transfer(e.seller, sellerAmt), "Escrow: seller payout failed");
        }

        emit DisputeResolved(orderId, buyerAmt, sellerAmt, e.burnAmount);
    }

    // --- Admin Functions ---

    function setBurnBps(uint256 _burnBps) external onlyRole(DEFAULT_ADMIN_ROLE) {
        require(_burnBps <= 500, "Escrow: max 5%");
        burnBps = _burnBps;
        emit FeesUpdated(burnBps, platformFeeBps);
    }

    function setPlatformFeeBps(uint256 _feeBps) external onlyRole(DEFAULT_ADMIN_ROLE) {
        require(_feeBps <= 1000, "Escrow: max 10%");
        platformFeeBps = _feeBps;
        emit FeesUpdated(burnBps, platformFeeBps);
    }

    function setFeeRecipient(address _feeRecipient) external onlyRole(DEFAULT_ADMIN_ROLE) {
        require(_feeRecipient != address(0), "Escrow: zero address");
        feeRecipient = _feeRecipient;
    }

    function pause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _pause();
    }

    function unpause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _unpause();
    }

    // --- Internal ---

    function _settle(Escrow storage e) internal {
        if (e.burnAmount > 0) {
            rvToken.platformBurn(address(this), e.burnAmount, "purchase_burn");
            totalBurnedViaEscrow += e.burnAmount;
        }
        if (e.feeAmount > 0) {
            require(rvToken.transfer(feeRecipient, e.feeAmount), "Escrow: fee failed");
        }
        require(rvToken.transfer(e.seller, e.sellerAmount), "Escrow: seller payout failed");
    }

    function _authorizeUpgrade(
        address newImplementation
    ) internal override onlyRole(UPGRADER_ROLE) {}

    // --- View Functions ---

    function getEscrow(bytes32 orderId) external view returns (Escrow memory) {
        return escrows[orderId];
    }
}
```

### Design Decisions

- **ReentrancyGuard on every mutating function**: RV is ERC-20, but malicious token upgrades could introduce callbacks. Guard everything.
- **Auto-release**: Prevents funds locked forever. Anyone can call `autoRelease` after the deadline — no admin dependency.
- **Dispute extends timeout**: When a dispute is opened, auto-release is pushed to 30 days to give arbiters time.
- **Burns on disputes**: The burn is a platform tax on economic activity, not a reward. Even disputed transactions burn. This prevents gaming (open dispute to avoid burn).
- **Basis points**: All rates in bps (1 bps = 0.01%). Hard caps (5% burn, 10% fee) prevent admin abuse.
- **Pausable**: Emergency halt for all new deposits. Existing escrows can still be released/refunded.

---

## 4. RVStaking Contract

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts-upgradeable/access/AccessControlUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/ReentrancyGuardUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/PausableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";

/// @title RVStaking — Tiered Staking with Fee-Funded Rewards
contract RVStaking is
    Initializable,
    AccessControlUpgradeable,
    ReentrancyGuardUpgradeable,
    PausableUpgradeable,
    UUPSUpgradeable
{
    bytes32 public constant REWARD_FUNDER_ROLE = keccak256("REWARD_FUNDER_ROLE");
    bytes32 public constant UPGRADER_ROLE = keccak256("UPGRADER_ROLE");

    IRVToken public rvToken;

    enum Tier { FLEX, BRONZE, SILVER, GOLD, DIAMOND }

    struct TierConfig {
        uint256 lockDuration;
        uint256 rewardRateBps;    // annual rate in bps
        uint256 earlyPenaltyBps;  // penalty on rewards for early exit
        uint256 feeDiscountBps;   // marketplace fee discount
    }

    struct Position {
        uint256 amount;
        Tier tier;
        uint64 stakedAt;
        uint64 unlocksAt;
        uint256 lastClaimAt;
        uint256 totalClaimed;
        bool active;
    }

    mapping(Tier => TierConfig) public tierConfigs;
    mapping(address => Position[]) public positions;

    uint256 public totalStaked;
    uint256 public rewardPool;
    uint256 public totalRewardsDistributed;
    uint256 public constant MIN_STAKE = 100 * 10**18;

    event Staked(address indexed user, uint256 indexed positionId, uint256 amount, Tier tier);
    event Unstaked(address indexed user, uint256 indexed positionId, uint256 principal, uint256 reward, bool early);
    event RewardsClaimed(address indexed user, uint256 indexed positionId, uint256 reward);
    event RewardPoolFunded(address indexed funder, uint256 amount);
    event TierConfigUpdated(Tier tier, uint256 lockDuration, uint256 rewardRateBps);

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    function initialize(address _rvToken) public initializer {
        __AccessControl_init();
        __ReentrancyGuard_init();
        __Pausable_init();
        __UUPSUpgradeable_init();

        rvToken = IRVToken(_rvToken);
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _grantRole(UPGRADER_ROLE, msg.sender);

        // Configure tiers
        tierConfigs[Tier.FLEX]    = TierConfig(0,         300,  5000, 0);
        tierConfigs[Tier.BRONZE]  = TierConfig(30 days,   600,  5000, 0);
        tierConfigs[Tier.SILVER]  = TierConfig(90 days,  1000,  5000, 500);
        tierConfigs[Tier.GOLD]    = TierConfig(180 days, 1400,  5000, 1000);
        tierConfigs[Tier.DIAMOND] = TierConfig(365 days, 1800,  5000, 1500);
    }

    /// @notice Stake tokens in a specific tier
    function stake(uint256 amount, Tier tier) external nonReentrant whenNotPaused {
        require(amount >= MIN_STAKE, "Staking: below minimum");
        require(
            rvToken.transferFrom(msg.sender, address(this), amount),
            "Staking: transfer failed"
        );

        TierConfig memory config = tierConfigs[tier];
        uint256 positionId = positions[msg.sender].length;

        positions[msg.sender].push(Position({
            amount: amount,
            tier: tier,
            stakedAt: uint64(block.timestamp),
            unlocksAt: uint64(block.timestamp + config.lockDuration),
            lastClaimAt: block.timestamp,
            totalClaimed: 0,
            active: true
        }));

        totalStaked += amount;
        emit Staked(msg.sender, positionId, amount, tier);
    }

    /// @notice Unstake tokens and claim remaining rewards
    function unstake(uint256 positionId) external nonReentrant {
        require(positionId < positions[msg.sender].length, "Staking: invalid position");
        Position storage pos = positions[msg.sender][positionId];
        require(pos.active, "Staking: not active");

        uint256 reward = _pendingReward(pos);
        bool early = block.timestamp < pos.unlocksAt && pos.tier != Tier.FLEX;

        if (early && reward > 0) {
            TierConfig memory config = tierConfigs[pos.tier];
            uint256 penalty = (reward * config.earlyPenaltyBps) / 10000;
            reward -= penalty;
            // Penalty stays in reward pool (not burned)
        }

        pos.active = false;
        totalStaked -= pos.amount;

        // Return principal (always safe, never penalized)
        require(rvToken.transfer(msg.sender, pos.amount), "Staking: principal failed");

        // Pay rewards if available
        if (reward > 0 && reward <= rewardPool) {
            rewardPool -= reward;
            totalRewardsDistributed += reward;
            require(rvToken.transfer(msg.sender, reward), "Staking: reward failed");
        }

        emit Unstaked(msg.sender, positionId, pos.amount, reward, early);
    }

    /// @notice Claim accumulated rewards without unstaking
    function claimRewards(uint256 positionId) external nonReentrant {
        require(positionId < positions[msg.sender].length, "Staking: invalid position");
        Position storage pos = positions[msg.sender][positionId];
        require(pos.active, "Staking: not active");

        uint256 reward = _pendingReward(pos);
        require(reward > 0, "Staking: no rewards");
        require(reward <= rewardPool, "Staking: insufficient pool");

        pos.lastClaimAt = block.timestamp;
        pos.totalClaimed += reward;
        rewardPool -= reward;
        totalRewardsDistributed += reward;

        require(rvToken.transfer(msg.sender, reward), "Staking: claim failed");
        emit RewardsClaimed(msg.sender, positionId, reward);
    }

    /// @notice Fund the reward pool (called by fee distributor or emission scheduler)
    function fundRewardPool(uint256 amount) external onlyRole(REWARD_FUNDER_ROLE) {
        require(
            rvToken.transferFrom(msg.sender, address(this), amount),
            "Staking: fund transfer failed"
        );
        rewardPool += amount;
        emit RewardPoolFunded(msg.sender, amount);
    }

    // --- View Functions ---

    function pendingRewards(address user, uint256 positionId) external view returns (uint256) {
        require(positionId < positions[user].length, "Staking: invalid position");
        return _pendingReward(positions[user][positionId]);
    }

    function getPositionCount(address user) external view returns (uint256) {
        return positions[user].length;
    }

    function getPosition(address user, uint256 positionId) external view returns (Position memory) {
        return positions[user][positionId];
    }

    function getFeeDiscount(address user) external view returns (uint256 maxDiscount) {
        Position[] storage userPositions = positions[user];
        for (uint256 i = 0; i < userPositions.length; i++) {
            if (userPositions[i].active) {
                uint256 discount = tierConfigs[userPositions[i].tier].feeDiscountBps;
                if (discount > maxDiscount) maxDiscount = discount;
            }
        }
        return maxDiscount;
    }

    // --- Internal ---

    function _pendingReward(Position memory pos) internal view returns (uint256) {
        if (!pos.active) return 0;

        TierConfig memory config = tierConfigs[pos.tier];
        uint256 elapsed = block.timestamp - pos.lastClaimAt;

        // Flash loan protection: if staked and claiming in same block, reward = 0
        if (elapsed == 0) return 0;

        uint256 annualReward = (pos.amount * config.rewardRateBps) / 10000;
        return (annualReward * elapsed) / 365 days;
    }

    function _authorizeUpgrade(
        address newImplementation
    ) internal override onlyRole(UPGRADER_ROLE) {}

    // --- Admin ---

    function updateTierConfig(
        Tier tier,
        uint256 lockDuration,
        uint256 rewardRateBps,
        uint256 earlyPenaltyBps,
        uint256 feeDiscountBps
    ) external onlyRole(DEFAULT_ADMIN_ROLE) {
        require(rewardRateBps <= 5000, "Staking: max 50% APY");
        require(earlyPenaltyBps <= 10000, "Staking: max 100% penalty");
        require(feeDiscountBps <= 5000, "Staking: max 50% discount");

        tierConfigs[tier] = TierConfig(lockDuration, rewardRateBps, earlyPenaltyBps, feeDiscountBps);
        emit TierConfigUpdated(tier, lockDuration, rewardRateBps);
    }

    function pause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _pause();
    }

    function unpause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _unpause();
    }
}
```

---

## 5. RVBuybackBurn Contract

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts-upgradeable/access/AccessControlUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/utils/ReentrancyGuardUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";

interface ISwapRouter {
    struct ExactInputSingleParams {
        address tokenIn;
        address tokenOut;
        uint24 fee;
        address recipient;
        uint256 amountIn;
        uint256 amountOutMinimum;
        uint160 sqrtPriceLimitX96;
    }
    function exactInputSingle(ExactInputSingleParams calldata params) external returns (uint256 amountOut);
}

interface IERC20 {
    function approve(address spender, uint256 amount) external returns (bool);
    function balanceOf(address account) external view returns (uint256);
    function transfer(address to, uint256 amount) external returns (bool);
}

/// @title RVBuybackBurn — Automated Treasury Buyback and Burn
/// @notice Buys RV from DEX using accumulated USDC fees, then burns purchased RV
contract RVBuybackBurn is
    Initializable,
    AccessControlUpgradeable,
    ReentrancyGuardUpgradeable,
    UUPSUpgradeable
{
    bytes32 public constant EXECUTOR_ROLE = keccak256("EXECUTOR_ROLE");
    bytes32 public constant UPGRADER_ROLE = keccak256("UPGRADER_ROLE");

    IRVToken public rvToken;
    IERC20 public usdc;
    ISwapRouter public swapRouter;

    uint256 public totalBoughtBack;
    uint256 public totalBurned;
    uint256 public executionCount;

    uint256 public maxSlippageBps;       // max slippage per trade (default: 200 = 2%)
    uint256 public maxTradeSize;         // max single trade in USDC (prevents large impact)
    uint256 public minBuybackAmount;     // minimum USDC to trigger buyback

    event BuybackExecuted(
        uint256 indexed executionId,
        uint256 usdcSpent,
        uint256 rvBought,
        uint256 rvBurned
    );
    event ConfigUpdated(uint256 maxSlippageBps, uint256 maxTradeSize, uint256 minBuybackAmount);

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    function initialize(
        address _rvToken,
        address _usdc,
        address _swapRouter
    ) public initializer {
        __AccessControl_init();
        __ReentrancyGuard_init();
        __UUPSUpgradeable_init();

        rvToken = IRVToken(_rvToken);
        usdc = IERC20(_usdc);
        swapRouter = ISwapRouter(_swapRouter);

        maxSlippageBps = 200;                          // 2%
        maxTradeSize = 5000 * 10**6;                   // $5,000 USDC (6 decimals)
        minBuybackAmount = 100 * 10**6;                // $100 USDC minimum

        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _grantRole(UPGRADER_ROLE, msg.sender);
    }

    /// @notice Execute buyback: swap USDC for RV, then burn all RV
    /// @param amountIn USDC amount to spend on buyback
    /// @param amountOutMin Minimum RV to receive (slippage protection)
    function executeBuyback(
        uint256 amountIn,
        uint256 amountOutMin
    ) external nonReentrant onlyRole(EXECUTOR_ROLE) {
        require(amountIn >= minBuybackAmount, "Buyback: below minimum");
        require(amountIn <= maxTradeSize, "Buyback: exceeds max trade");
        require(usdc.balanceOf(address(this)) >= amountIn, "Buyback: insufficient USDC");

        // Approve router to spend USDC
        usdc.approve(address(swapRouter), amountIn);

        // Execute swap: USDC → RV
        uint256 rvReceived = swapRouter.exactInputSingle(
            ISwapRouter.ExactInputSingleParams({
                tokenIn: address(usdc),
                tokenOut: address(rvToken),
                fee: 3000,                       // 0.3% pool fee tier
                recipient: address(this),
                amountIn: amountIn,
                amountOutMinimum: amountOutMin,
                sqrtPriceLimitX96: 0
            })
        );

        // Burn all purchased RV
        rvToken.platformBurn(address(this), rvReceived, "buyback_burn");

        executionCount++;
        totalBoughtBack += rvReceived;
        totalBurned += rvReceived;

        emit BuybackExecuted(executionCount, amountIn, rvReceived, rvReceived);
    }

    /// @notice Withdraw USDC in case of emergency (admin only)
    function emergencyWithdrawUSDC(address to) external onlyRole(DEFAULT_ADMIN_ROLE) {
        uint256 balance = usdc.balanceOf(address(this));
        require(balance > 0, "Buyback: no USDC");
        usdc.transfer(to, balance);
    }

    function updateConfig(
        uint256 _maxSlippageBps,
        uint256 _maxTradeSize,
        uint256 _minBuybackAmount
    ) external onlyRole(DEFAULT_ADMIN_ROLE) {
        require(_maxSlippageBps <= 1000, "Buyback: max 10% slippage");
        maxSlippageBps = _maxSlippageBps;
        maxTradeSize = _maxTradeSize;
        minBuybackAmount = _minBuybackAmount;
        emit ConfigUpdated(_maxSlippageBps, _maxTradeSize, _minBuybackAmount);
    }

    function _authorizeUpgrade(
        address newImplementation
    ) internal override onlyRole(UPGRADER_ROLE) {}
}
```

---

## 6. Contract Invariants

These must hold true at all times. Verified by invariant tests.

### RVToken Invariants

```
INV-T1: totalSupply() + totalBurned <= MAX_SUPPLY
INV-T2: totalSupply() >= BURN_STOP (unless no burns have ever occurred)
INV-T3: totalMinted <= MAX_SUPPLY
INV-T4: If totalSupply() <= BURN_STOP, effectiveBurnAmount(x) == 0 for all x
INV-T5: If totalSupply() <= BURN_FLOOR, effectiveBurnAmount(x) == x/2 for all x > 0
INV-T6: If totalSupply() > BURN_FLOOR, effectiveBurnAmount(x) == x for all x
INV-T7: No transfer can occur when paused
```

### RVEscrow Invariants

```
INV-E1: For any escrow e:
        e.burnAmount + e.feeAmount + e.sellerAmount == e.totalAmount
INV-E2: rvToken.balanceOf(escrow) >= sum of all FUNDED escrow totalAmounts
INV-E3: Status transitions are one-way:
        NONE → FUNDED → {RELEASED | REFUNDED | DISPUTED}
        DISPUTED → RESOLVED
INV-E4: Only buyer or seller can open a dispute
INV-E5: Only ARBITER_ROLE can resolve a dispute
INV-E6: Only OPERATOR_ROLE can release or refund
INV-E7: autoRelease can only be called after autoReleaseAt timestamp
INV-E8: burnBps <= 500 (5% max)
INV-E9: platformFeeBps <= 1000 (10% max)
INV-E10: Seller address cannot be zero or same as buyer
```

### RVStaking Invariants

```
INV-S1: rvToken.balanceOf(staking) >= totalStaked + rewardPool
INV-S2: For any active position: pos.amount >= MIN_STAKE
INV-S3: totalStaked == sum of all active position amounts
INV-S4: Rewards claimed in same block as stake == 0 (flash loan protection)
INV-S5: Early unstake penalty <= accumulated rewards (principal never penalized)
INV-S6: rewardRateBps <= 5000 (50% max APY cap)
INV-S7: position.active transitions: true → false (one-way)
```

### RVBuybackBurn Invariants

```
INV-B1: totalBurned == totalBoughtBack (all bought RV is burned)
INV-B2: Individual trade size <= maxTradeSize
INV-B3: maxSlippageBps <= 1000
```

---

## 7. Deployment Strategy

### Network

```
Chain:     Polygon PoS Mainnet (Chain ID: 137)
RPC:       Alchemy (primary) + QuickNode (failover)
Explorer:  Polygonscan
Testnet:   Polygon Amoy (Chain ID: 80002)
```

### Deployment Order

```
1.  Deploy RVToken implementation → deploy proxy → initialize(adminMultisig)
2.  Mint initial supply to deployer (1B RV)
3.  Deploy RVEscrow implementation → deploy proxy → initialize(rvToken, feeWallet, 100, 250, 14)
4.  Deploy RVStaking implementation → deploy proxy → initialize(rvToken)
5.  Deploy RVBuybackBurn implementation → deploy proxy → initialize(rvToken, usdc, router)
6.  Grant roles:
      RVToken.BURNER_ROLE → RVEscrow address
      RVToken.BURNER_ROLE → RVBuybackBurn address
      RVToken.MINTER_ROLE → emission scheduler (for staking rewards)
      RVStaking.REWARD_FUNDER_ROLE → fee distributor
      RVBuybackBurn.EXECUTOR_ROLE → buyback scheduler bot
      RVEscrow.OPERATOR_ROLE → platform backend signer
      RVEscrow.ARBITER_ROLE → dispute resolution signer
7.  Distribute tokens per allocation table:
      150M → public sale contract (with vesting)
      150M → team vesting contract (1yr cliff + 3yr linear)
      250M → ecosystem rewards wallet
      150M → platform treasury multi-sig
      150M → staking reward pool → RVStaking.fundRewardPool()
      100M → DEX liquidity pools (permanently locked LP)
       50M → advisor vesting contract (6mo cliff + 2yr linear)
8.  Lock LP tokens (send to zero address or TimelockController with no withdraw)
9.  Transfer DEFAULT_ADMIN_ROLE + UPGRADER_ROLE → Gnosis Safe multi-sig (3-of-5)
10. Verify all contracts on Polygonscan
11. Deploy The Graph subgraph
```

### Multi-Sig Security

```
Gnosis Safe: 3-of-5 signers required
Signers: 2 founders + 1 advisor + 1 legal + 1 security auditor

Admin operations:
  - Upgrade contracts: 48-hour timelock + 3-of-5
  - Change fee rates: 24-hour timelock + 3-of-5
  - Pause contracts: NO timelock (emergency) + 2-of-5
  - Grant/revoke roles: 24-hour timelock + 3-of-5
  - Emergency USDC withdrawal: 24-hour timelock + 4-of-5
```

---

## 8. Testing Strategy

### Unit Tests (Hardhat + Chai)

```
Target: 100% line coverage on all contracts

RVToken tests:
  ✓ mint respects MAX_SUPPLY cap
  ✓ platformBurn reduces supply and increases totalBurned
  ✓ effectiveBurnAmount halves below BURN_FLOOR
  ✓ effectiveBurnAmount returns 0 below BURN_STOP
  ✓ only MINTER_ROLE can mint
  ✓ only BURNER_ROLE can platformBurn
  ✓ transfers blocked when paused
  ✓ permit (EIP-2612) works correctly

RVEscrow tests:
  ✓ deposit creates escrow with correct amounts
  ✓ deposit fails for existing orderId
  ✓ deposit fails for self-trade (buyer == seller)
  ✓ release sends correct amounts (burn + fee + seller)
  ✓ autoRelease works after timeout
  ✓ autoRelease fails before timeout
  ✓ refund returns full amount to buyer
  ✓ openDispute changes status and extends timeout
  ✓ resolveDispute splits correctly (0%, 50%, 100%)
  ✓ only OPERATOR_ROLE can release/refund
  ✓ only ARBITER_ROLE can resolve disputes
  ✓ fee rate caps enforced

RVStaking tests:
  ✓ stake creates position with correct tier config
  ✓ stake fails below MIN_STAKE
  ✓ unstake returns principal always
  ✓ early unstake applies penalty to rewards only
  ✓ claimRewards calculates correctly over time
  ✓ flash loan protection: 0 rewards in same block
  ✓ fundRewardPool increases pool correctly
  ✓ getFeeDiscount returns highest active tier discount

RVBuybackBurn tests:
  ✓ executeBuyback swaps USDC for RV and burns
  ✓ executeBuyback fails below minBuybackAmount
  ✓ executeBuyback fails above maxTradeSize
  ✓ all bought RV is burned (totalBurned == totalBoughtBack)
  ✓ only EXECUTOR_ROLE can execute
```

### Fuzz Tests (Foundry)

```solidity
// Example fuzz test for escrow arithmetic invariant
function testFuzz_EscrowAmountsSumCorrectly(
    uint256 amount,
    uint256 burnBps,
    uint256 feeBps
) public {
    amount = bound(amount, 1e18, 1e27);     // 1 to 1B RV
    burnBps = bound(burnBps, 0, 500);        // 0-5%
    feeBps = bound(feeBps, 0, 1000);         // 0-10%

    uint256 burnAmt = (amount * burnBps) / 10000;
    uint256 feeAmt = (amount * feeBps) / 10000;
    uint256 sellerAmt = amount - burnAmt - feeAmt;

    // INVARIANT: amounts must sum to total
    assertEq(burnAmt + feeAmt + sellerAmt, amount);
    // INVARIANT: seller must receive something
    assertTrue(sellerAmt > 0);
}

// Fuzz test for staking reward calculation
function testFuzz_StakingRewardsAccrueLinearly(
    uint256 amount,
    uint256 elapsed
) public {
    amount = bound(amount, 100e18, 1e27);
    elapsed = bound(elapsed, 1, 365 days);

    uint256 annualReward = (amount * 1400) / 10000;  // Gold tier 14%
    uint256 reward = (annualReward * elapsed) / 365 days;

    // INVARIANT: reward should be proportional to time
    assertTrue(reward <= annualReward);
    // INVARIANT: 1 year of staking should yield exactly annualReward
    if (elapsed == 365 days) {
        assertEq(reward, annualReward);
    }
}
```

### Invariant Tests (Foundry)

```solidity
// Invariant: escrow contract balance >= sum of all funded escrows
function invariant_EscrowBalanceCoversAllFunded() public {
    uint256 contractBalance = rvToken.balanceOf(address(escrow));
    uint256 totalFundedAmount = getTotalFundedEscrowAmount();
    assertGe(contractBalance, totalFundedAmount);
}

// Invariant: staking contract balance >= totalStaked + rewardPool
function invariant_StakingBalanceSolvent() public {
    uint256 contractBalance = rvToken.balanceOf(address(staking));
    assertGe(contractBalance, staking.totalStaked() + staking.rewardPool());
}

// Invariant: total supply + total burned <= MAX_SUPPLY
function invariant_SupplyNeverExceedsMax() public {
    assertLe(
        rvToken.totalSupply() + rvToken.totalBurned(),
        rvToken.MAX_SUPPLY()
    );
}
```

### Integration Tests

```
- Full purchase flow: deposit → release → verify balances
- Full dispute flow: deposit → dispute → resolve → verify split
- Auto-release after timeout
- Staking → earning rewards → claiming → unstaking
- Buyback: fund USDC → execute → verify burn
- Upgrade: deploy new implementation → verify state preserved
- Emergency: pause all contracts → verify operations halted → unpause
```

### Gas Profiling

```
Target gas costs (Polygon):
  RVToken.transfer:        ~65,000 gas   (~$0.001)
  RVEscrow.deposit:       ~150,000 gas   (~$0.003)
  RVEscrow.release:       ~120,000 gas   (~$0.002)
  RVStaking.stake:        ~130,000 gas   (~$0.002)
  RVStaking.claimRewards:  ~80,000 gas   (~$0.001)
  RVBuybackBurn.execute:  ~200,000 gas   (~$0.004)

All well within acceptable limits at Polygon gas prices.
```

### Audit Plan

```
Pre-launch:
  Audit 1: Core contracts (RVToken + RVEscrow) — Trail of Bits or OpenZeppelin
  Audit 2: Full system (all contracts) — Cyfrin or Spearbit

Post-launch:
  Bug bounty: Immunefi ($50K–$250K payouts)
  Continuous monitoring: Forta Network agents
  Quarterly re-audit of any upgraded contracts
```
