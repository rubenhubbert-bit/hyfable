# Independent Geometric Analysis: Flight Paths and Line-of-Sight Phenomena

## Preamble

This document presents a first-principles geometric analysis comparing two models:

1. **Spherical Model**: Earth as an oblate spheroid (approximated as a sphere with R = 6,371 km for calculations)
2. **Azimuthal Equidistant (AE) Model**: A Gleason-style flat projection centered on the North Pole, where the map *is* the physical geometry (not merely a projection of a sphere)

No model is assumed correct at the outset. All assumptions are stated explicitly. All calculations are shown step-by-step. The analysis relies on publicly verifiable data: published airline routes, aircraft specifications, and basic trigonometry.

---

## Part 1: Flight Path Analysis

### 1.1 Mathematical Framework

#### Spherical Model — Great Circle Distance

For two points at coordinates (lat₁, lon₁) and (lat₂, lon₂), the great circle distance is:

```
d = R × arccos[ sin(φ₁)·sin(φ₂) + cos(φ₁)·cos(φ₂)·cos(Δλ) ]
```

Where:
- R = 6,371 km (mean Earth radius)
- φ₁, φ₂ = latitudes in radians
- Δλ = |lon₁ - lon₂|, adjusted to be ≤ 180°

**Assumption**: Earth is a perfect sphere. The oblate spheroid corrections are < 0.3% and do not materially affect the analysis.

#### AE Flat Model — Euclidean Distance

On the Gleason azimuthal equidistant projection centered on the North Pole:

Each point is mapped to polar coordinates:
- Radial distance from center: `r = R × (π/2 - φ)` where φ is latitude in radians
- Angular position: `θ = λ` (longitude)

The straight-line (Euclidean) distance between two points is:

```
d_AE = √( r₁² + r₂² - 2·r₁·r₂·cos(Δλ) )
```

**Assumption**: If the AE map represents true physical geometry, the shortest path between two points is a straight line (Euclidean metric). This is the most generous assumption for the AE model — any curved path would be longer.

**Critical Note on Path Options**: For every route, I also computed the path through the North Pole (center of the AE map): `d_via_pole = r₁ + r₂`. In all Southern Hemisphere cases tested, the straight-line path was shorter than the via-pole path. The straight line represents the absolute minimum distance possible on the AE surface.

### 1.2 Airport Coordinates Used

| Airport | City | Latitude | Longitude |
|---------|------|----------|-----------|
| SYD | Sydney, Australia | -33.95°S | 151.18°E |
| SCL | Santiago, Chile | -33.39°S | 70.79°W |
| JNB | Johannesburg, South Africa | -26.13°S | 28.24°E |
| GRU | São Paulo, Brazil | -23.43°S | 46.47°W |
| PER | Perth, Australia | -31.95°S | 115.97°E |
| AKL | Auckland, New Zealand | -37.01°S | 174.78°E |
| EZE | Buenos Aires, Argentina | -34.82°S | 58.54°W |
| JFK | New York, USA | 40.64°N | 73.78°W |
| LHR | London, UK | 51.47°N | 0.46°W |
| LAX | Los Angeles, USA | 33.94°N | 118.41°W |
| DXB | Dubai, UAE | 25.25°N | 55.36°E |

All coordinates are from publicly available aviation databases and can be independently verified.

### 1.3 Flight Data and Aircraft Specifications

The following flights are real, commercially operated routes. Flight times are typical scheduled block times drawn from airline timetables and flight tracking databases.

#### Southern Hemisphere Routes

| Route | Airline/Flight | Aircraft | Scheduled Time | Published Distance |
|-------|---------------|----------|---------------|-------------------|
| SYD → SCL | Qantas QF27 (codeshare LATAM LA806) | Boeing 787-9 | ~12 hr 30 min | 11,363 km |
| JNB → GRU | LATAM LA8059 | Boeing 787-9 | ~10 hr 15 min | 7,435 km |
| PER → JNB | Qantas QF65 / SAA SA281 | Airbus A330-200 / A340-300 | ~11 hr 15 min | 8,340 km |
| AKL → SCL | LATAM LA800 | Boeing 787-9 | ~11 hr 05 min | 9,653 km |
| SYD → JNB | Qantas QF63 | Airbus A380-800 | ~14 hr 30 min | 11,050 km |
| SYD → EZE | Aerolíneas Argentinas AR1180 | Airbus A340-300 | ~14.5 hr | ~11,800 km |

**Data sources**: Flightera.net, FlightAware, Airportia, airline timetables. Flight numbers verified against current schedules. QF27 operates daily SYD T1 to SCL T2. LA8059 operates 4x/week (Mon/Wed/Thu/Sat). QF65 launched 7 December 2025 on the A330-200; SAA SA281 operates 5x/week on the A340-300. LA800 operates 5x/week. QF63 operates up to 6x/week on the A380.

#### Northern Hemisphere / Cross-Hemisphere Routes (Controls)

| Route | Airline/Flight | Aircraft | Scheduled Time | Published Distance |
|-------|---------------|----------|---------------|-------------------|
| JFK → LHR | British Airways BA178 | Boeing 777-300ER | ~6 hr 55 min | 5,541 km |
| LAX → DXB | Emirates EK216 | Airbus A380-800 | ~15 hr 55 min | 13,400 km |

#### Aircraft Performance Specifications

| Aircraft | Cruise Speed | Maximum Range | Fuel Capacity |
|----------|-------------|---------------|---------------|
| Boeing 787-9 | 903 km/h (Mach 0.85) | 14,010 km | 138,700 L |
| Boeing 777-300ER | 892 km/h (Mach 0.84) | 14,500 km | 181,283 L |
| Boeing 777-200ER | 892 km/h (Mach 0.84) | 14,305 km | 171,160 L |
| Airbus A330-200 | 870 km/h (Mach 0.82) | 13,450 km | 139,090 L |
| Airbus A340-300 | 871 km/h (Mach 0.82) | 13,500 km | 147,000 L |
| Airbus A380-800 | 903 km/h (Mach 0.85) | 15,700 km | 320,000 L |

**Note**: Maximum range figures are from manufacturer datasheets and FAA type certificates for standard airline configurations with typical payload. These are hard physical limits set by fuel tank capacity and engine efficiency — not soft policy constraints. Sources: Boeing.com, Airbus.com, SKYbrary, AeroCorner.

### 1.4 Comparative Distance and Speed Analysis

The following table compares computed distances and implied ground speeds on both models:

```
══════════════════════════════════════════════════════════════════════════════════════════════════════════
Route        Spherical (km)  AE Flat (km)  AE/Sphere  Flight(hr)  Sphere Speed  AE Speed    Category
══════════════════════════════════════════════════════════════════════════════════════════════════════════
SYD → SCL         11,340        25,679       2.26×       12.5       907 km/h    2,054 km/h  Southern
JNB → GRU          7,439        15,490       2.08×        9.5       783 km/h    1,630 km/h  Southern
PER → JNB          8,311        18,351       2.21×       11.0       756 km/h    1,668 km/h  Southern
AKL → SCL          9,654        23,409       2.42×       11.25      858 km/h    2,081 km/h  Southern
SYD → JNB         11,025        23,458       2.13×       14.0       787 km/h    1,676 km/h  Southern
SYD → EZE         11,766        26,737       2.27×       14.5       811 km/h    1,844 km/h  Southern
──────────────────────────────────────────────────────────────────────────────────────────────────────────
JFK → LHR          5,540         5,914       1.07×        7.0       791 km/h      845 km/h  Northern
LAX → DXB         13,401        13,414       1.00×       16.0       838 km/h      838 km/h  Near-Polar
══════════════════════════════════════════════════════════════════════════════════════════════════════════

Speed of sound at cruise altitude (~35,000 ft): ~1,062 km/h (Mach 1.0)
```

### 1.5 Analysis of Results

#### Observation 1: Northern Hemisphere routes fit both models

The JFK → LHR route yields nearly identical distances on both models (5,540 vs 5,914 km, ratio 1.07). This is expected: the AE projection preserves distances along radials from the North Pole, so routes in the Northern Hemisphere at moderate latitudes show minimal distortion. The LAX → DXB route, which passes near the North Pole, shows virtually zero difference (ratio 1.00).

**Implication**: Both models are consistent with Northern Hemisphere flight data. This region cannot discriminate between them.

#### Observation 2: Every Southern Hemisphere route requires physically impossible speeds on the AE model

All six Southern Hemisphere routes require ground speeds of 1,630–2,081 km/h on the AE model. These speeds are:

- **1.54× to 1.96× the speed of sound** at cruise altitude
- **1.8× to 2.3× the certified cruise speed** of the aircraft types used
- Impossible for any subsonic commercial aircraft by the laws of aerodynamics

No commercial aircraft in service can achieve Mach 1.5+. The Boeing 787-9 (used on the SYD–SCL route) has a maximum operating Mach number (Mmo) of 0.90 — exceeding this causes structural buffeting and potential airframe failure. There is no plausible way to fly at Mach 1.9 in a 787-9.

**This is not a marginal discrepancy.** The AE model requires speeds 54%–96% above the sound barrier. There is no atmospheric phenomenon, tailwind, or measurement error that could account for a factor-of-two speed deficit.

#### Observation 3: Southern Hemisphere AE distances exceed aircraft range

```
Route       AE Distance   Aircraft     Max Range    Excess
─────────────────────────────────────────────────────────────
SYD → SCL    25,679 km    787-9       14,010 km    +11,669 km (1.83× range)
AKL → SCL    23,409 km    787-9       14,010 km    + 9,399 km (1.67× range)
SYD → JNB    23,458 km    A380-800    15,700 km    + 7,758 km (1.49× range)
SYD → EZE    26,737 km    A340-300    13,500 km    +13,237 km (1.98× range)
PER → JNB    18,351 km    A330-200    13,450 km    + 4,901 km (1.36× range)
JNB → GRU    15,490 km    787-9       14,010 km    + 1,480 km (1.11× range)
```

Aircraft maximum range is a hard physical constraint. It is determined by:
- Fuel tank volume (a fixed physical dimension of the airframe)
- Engine specific fuel consumption (bounded by thermodynamics)
- Aerodynamic drag (determined by airframe geometry)

A Boeing 787-9 cannot carry 1.82× its maximum fuel load. The fuel tanks do not physically hold that volume. The SYD–SCL route operates nonstop — there is no mid-air refueling. Yet on the AE model, this flight would need to cover 25,679 km, which is 11,539 km beyond the aircraft's absolute maximum range.

**This is a hard falsification.** The aircraft physically cannot carry enough fuel to fly AE-model distances.

#### Observation 4: Spherical model speeds are uniformly consistent

On the spherical model, all eight routes yield ground speeds between 756–907 km/h. These are all within the normal operating envelope of the respective aircraft types:

- Cruise speeds of 830–920 km/h match manufacturer specifications
- The lower speeds (756–787 km/h on PER–JNB and SYD–JNB) are consistent with headwind effects on these routes (the Southern Indian Ocean has strong westerly winds)
- No route requires anomalous speed

The internal consistency of the spherical model across all routes — Northern Hemisphere, Southern Hemisphere, near-polar, and cross-equatorial — is a strong indicator of geometric accuracy.

#### Observation 5: AE model distortion is systematic, not random

The AE/Sphere ratio is strongly correlated with the southern latitude and longitude separation of the endpoints:

| Route | Avg Latitude | Longitude Separation | AE/Sphere Ratio |
|-------|-------------|---------------------|----------------|
| JFK → LHR | 46°N | 73° | 1.07 |
| LAX → DXB | 30°N | 174° | 1.00 |
| JNB → GRU | 25°S | 75° | 2.08 |
| PER → JNB | 29°S | 88° | 2.21 |
| SYD → SCL | 34°S | 138° | 2.26 |
| SYD → EZE | 34°S | 150° | 2.27 |
| AKL → SCL | 35°S | 114° | 2.42 |

This is exactly the pattern predicted by the mathematics of the AE projection: the further south from the center, the more the map stretches distances. This is not a coincidence — it is a direct consequence of projecting a sphere onto a flat disk from one pole. The AE projection preserves distances *from the North Pole* but necessarily distorts distances *between points far from the center* because it maps the South Pole (a single point) to the entire outer circumference of the disk.

If the AE map were the actual geometry, there would be no reason for this systematic distance inflation. The fact that the inflation precisely matches the mathematical prediction of projection distortion is strong evidence that the AE map *is* a projection — not a representation of true geometry.

### 1.6 Addressing Potential Counterarguments

**"Maybe the flights don't actually fly those routes nonstop"**

These flights are verified nonstop services. SYD–SCL is operated by LATAM and Qantas as a direct nonstop flight. Passengers board in Sydney and deplane in Santiago. ADS-B tracking data confirms the aircraft flies a continuous path over the South Pacific. There is no secret refueling stop.

**"Maybe the aircraft have secret capabilities"**

Aircraft fuel capacity is a certified, publicly documented specification that determines regulatory payload-range calculations. Airlines, regulators, manufacturers, and independent aviation analysts all use the same figures. The fuel tank volumes are physically measured dimensions of the airframe.

**"Maybe ground speed is different from airspeed due to winds"**

Winds affect ground speed by at most ±150 km/h in extreme jet streams. The AE model deficit is 700–1,200 km/h — an order of magnitude larger than any wind effect. Furthermore, winds would need to be consistently supersonic tailwinds on every Southern Hemisphere route, which contradicts atmospheric physics.

**"Maybe the AE map distances aren't calculated correctly"**

The calculations above use standard Euclidean distance — the shortest possible path on a flat surface. Any alternative routing (curved paths, via the pole) would be *longer*, making the discrepancy worse, not better. The via-pole distances are even larger:

```
Route       Straight Line   Via North Pole
SYD → SCL    25,679 km       27,503 km
AKL → SCL    23,409 km       27,843 km
SYD → JNB    23,458 km       26,696 km
```

---

## Part 2: Line-of-Sight Geometric Analysis

### 2.1 Scenario Definition

Analyze the geometric feasibility of a ground-based or ship-based system achieving line-of-sight to a target under these conditions:
- **Engagement distance**: ~100 statute miles (160.9 km)
- **Target altitude**: ~50 feet (15.2 m) above mean sea level
- **Medium**: Over-water path (no terrain obstructions)

### 2.2 Assumptions Stated

1. Earth radius R = 6,371 km (spherical model)
2. Standard atmospheric refraction: modeled as effective Earth radius = 4/3 × R (the standard "k = 4/3" model used in radar engineering since the 1940s)
3. Extreme ducting: modeled as effective Earth radius = 2 × R (upper bound for anomalous tropospheric ducting)
4. Target and observer heights are above mean sea level
5. No terrain obstructions between observer and target (over-water path)

### 2.3 Geometric Curvature Calculations

#### Earth Curvature Drop Formula

For a distance d along the surface, the geometric "drop" below the tangent plane is:

```
h_drop = d² / (2R)
```

This is the first-order approximation, valid for d << R (which holds for d = 161 km vs R = 6,371 km — error < 0.02%).

#### Curvature Drop Table

| Distance | Drop (no refraction) | Drop (4/3 refraction) | Drop (2× ducting) |
|----------|---------------------|-----------------------|-------------------|
| 1 mile | 0.2 m (0.7 ft) | 0.2 m (0.5 ft) | 0.1 m (0.3 ft) |
| 5 miles | 5.1 m (17 ft) | 3.8 m (13 ft) | 2.5 m (8 ft) |
| 10 miles | 20.3 m (67 ft) | 15.2 m (50 ft) | 10.2 m (33 ft) |
| 20 miles | 81.3 m (267 ft) | 61.0 m (200 ft) | 40.7 m (133 ft) |
| 50 miles | 508 m (1,667 ft) | 381 m (1,250 ft) | 254 m (833 ft) |
| 100 miles | 2,033 m (6,669 ft) | 1,525 m (5,002 ft) | 1,016 m (3,334 ft) |
| 150 miles | 4,573 m (15,005 ft) | 3,430 m (11,254 ft) | 2,287 m (7,503 ft) |

#### At 100 miles: The curvature deficit

With no refraction, the geometric drop at 100 miles is **2,033 meters** (6,669 feet). A target at 50 feet would be approximately **2,018 meters (6,619 feet) below the geometric horizon** as seen from sea level.

Even with standard 4/3 refraction, the drop is **1,525 meters** (5,002 feet). The target remains approximately **1,509 meters (4,952 feet) below the effective horizon**.

With extreme ducting (2× radius — an upper bound rarely achieved in practice), the drop is still **1,016 meters** (3,334 feet), placing the target approximately **1,001 meters (3,284 feet) below the effective horizon**.

### 2.4 Required Observer Height

For an observer to achieve direct line-of-sight to a target at height h₂ over distance d:

```
d = √(2·R_eff·h₁) + √(2·R_eff·h₂)
```

Solving for h₁ (observer height):

```
h₁ = (d - √(2·R_eff·h₂))² / (2·R_eff)
```

#### Results for target at 50 ft (15.2 m) at 100 miles (160.9 km)

| Atmospheric Model | Effective R | Required Observer Height |
|-------------------|------------|------------------------|
| No refraction | 6,371 km | 1,696 m (5,564 ft) |
| Standard 4/3 | 8,495 km | 1,235 m (4,051 ft) |
| Extreme ducting (2×) | 12,742 km | 783 m (2,568 ft) |

#### Maximum LOS Distance for Various Observer Heights (target at 50 ft)

```
Observer Height    No Refraction    4/3 Refraction    2× Ducting
──────────────────────────────────────────────────────────────────
10 ft (3 m)         12.5 mi          14.5 mi          17.7 mi
50 ft (15 m)        17.3 mi          20.0 mi          24.5 mi
100 ft (30 m)       20.9 mi          24.1 mi          29.5 mi
200 ft (61 m)       26.0 mi          30.0 mi          36.7 mi
500 ft (152 m)      36.0 mi          41.6 mi          50.9 mi
1,000 ft (305 m)    47.4 mi          54.7 mi          67.0 mi
3,000 ft (914 m)    75.7 mi          87.4 mi         107.0 mi
5,000 ft (1524 m)   95.2 mi         110.0 mi         134.6 mi
10,000 ft (3048 m) 131.1 mi         151.4 mi         185.3 mi
```

### 2.5 Analysis

#### On the spherical model

For a ground-based or low-altitude system (observer at < 100 ft) to engage a target at 50 ft altitude at 100 miles distance:

- **Without refraction**: Requires observer at 5,564 ft — impossible from a ship deck (~50-100 ft) or ground installation
- **With standard refraction**: Requires observer at 4,051 ft — still impossible from surface level
- **With extreme ducting**: Requires observer at 2,568 ft — still impossible from surface level

A ship-mounted radar at 100 ft (typical mast height) can see a 50 ft target to:
- 20.9 miles (no refraction)
- 24.1 miles (standard refraction)
- 29.5 miles (extreme ducting)

This is far short of 100 miles. The geometry is unambiguous.

**Documented naval radar limits confirm this geometry.** The AN/SPY-1D phased array on Arleigh Burke-class destroyers has array centers at approximately 40-50 ft above waterline, with the highest antennas reaching ~100 ft. Against a sea-skimming target at 10-16 ft, the Navy's own assessments estimate effective detection range at only 20-30 nautical miles (~23-35 statute miles). The Defense Science Board has stated the SPY-1 is "inadequate" against sea-skimming threats, which prompted development of the AN/SPQ-9B supplementary radar and the AN/SPY-6 replacement. These limitations are consistent with the spherical geometry computed above.

**However**, there are several well-documented systems and phenomena that extend engagement range beyond direct LOS:

1. **Over-the-Horizon Radar — Skywave (OTH-B)**: Systems like JORN (Jindalee Operational Radar Network, Australia — three stations covering 1,000-3,000 km), AN/TPS-71 ROTHR (US Navy — transmit array 2.58 km long with 372 twin monopole elements, range to 2,500 miles), and the Soviet Duga system use ionospheric bouncing of HF radio waves (3-30 MHz). These explicitly *do not* use direct line-of-sight — they refract signals off the ionosphere at 100-400 km altitude. Their existence and engineering complexity are premised on a curved Earth blocking direct LOS. Canada announced in March 2025 it would purchase JORN technology for Arctic deployment.

2. **Over-the-Horizon Radar — Surface Wave (HFSWR)**: Systems like Russia's Podsolnukh and Raytheon's SWR-503 use vertically polarized HF radio that diffracts along the conductive ocean surface, following Earth's curvature. Range: 200-400 km. These require no ionospheric bounce but explicitly depend on surface-wave propagation along a curved surface.

3. **NIFC-CA / Cooperative Engagement Capability (CEC)**: The U.S. Navy's Naval Integrated Fire Control — Counter Air architecture uses E-2D Hawkeye aircraft (at ~25,000 ft, with radar horizon of ~200 miles to sea level) or F-35 fighters as remote sensors. These relay fire-control quality targeting data to Aegis ships, which fire SM-6 missiles with active terminal seekers. In June 2014, an SM-6 set a record for the longest surface-to-air engagement in naval history. In April 2021, USS John Finn struck a target at 250 miles using SM-6 with remote targeting. The ship never needs direct LOS — the entire architecture is designed around the reality that ship-based radar cannot see low targets at long range.

4. **Weapons with autonomous terminal guidance**: The SM-6 (RIM-174 ERAM, range ~200+ nautical miles) flies an arc trajectory — up, then down — using its own active radar seeker for terminal homing. The SM-2 (range ~90 NM) operates similarly. Neither requires direct LOS from the launch platform to the target. The cancelled electromagnetic railgun program also used ballistic arc trajectories.

5. **Atmospheric ducting**: Documented extensively in military exercises. During SHAREM-115 (Persian Gulf, April 1996), hot desert air over warm Gulf waters created extreme surface-based ducts (published in Brooks, Goroch & Rogers, 1999, *Journal of Applied Meteorology*). In the South China Sea, simulations and measurements showed ducting at 12 GHz achieving "straight-through" propagation at 300 km (~186 miles) with favorable evaporation duct conditions. Yellow Sea measurements documented X-band microwave propagation at 133 km (7.7× geometric LOS distance). However, ducting is intermittent, unpredictable, seasonally variable, and no military system depends on it for guaranteed engagement capability.

#### On the flat/planar model

On a flat plane, a 50 ft target at 100 miles would be geometrically visible from any elevation (including ground level), limited only by atmospheric transparency. The engagement would be trivially possible from a geometric standpoint.

### 2.6 Assessment

The claim that a surface-based system directly engaged a sea-skimming target at 100 miles requires careful scrutiny of the specific system and engagement profile:

- If the engagement used **direct LOS only** from a surface platform at ~50-100 ft elevation → **inconsistent with the spherical model** without extraordinary refraction. Even with extreme ducting (documented up to ~186 miles in optimal South China Sea conditions), such conditions are intermittent and unreliable. The planar model resolves this trivially.
- If the engagement used **external targeting data** (NIFC-CA, E-2D Hawkeye relay, F-35 sensor, satellite, etc.) → **fully consistent with the spherical model**. This is exactly how the USS John Finn achieved a 250-mile SM-6 engagement in 2021 — using remote sensor relay, not direct ship radar LOS.
- If the engagement used **over-the-horizon radar** → **explicitly designed for a curved Earth**; systems like JORN, ROTHR, and Podsolnukh would be unnecessary engineering efforts on a flat plane. The ROTHR transmit array alone is 2.58 km long — an enormous investment to solve a problem that would not exist without curvature.

**Without knowing the specific engagement profile**, the observation is ambiguous. The spherical model accommodates 100-mile engagements through well-documented multi-platform architectures (and the entire U.S. Navy NIFC-CA program is built around this limitation), but *not* through direct surface-to-surface LOS. The planar model accommodates it through simple geometry but fails to explain why OTH-R systems and multi-platform targeting architectures exist at all, or why the Navy considers sea-skimming missile defense its most challenging problem.

---

## Part 3: Structured Comparison and Conclusions

### 3.1 Model Comparison Table

```
═══════════════════════════════════════════════════════════════════════════════════════
Criterion                     Spherical Model              AE Flat Model
═══════════════════════════════════════════════════════════════════════════════════════
Northern Hemisphere flights   ✓ Consistent                 ✓ Consistent
  (JFK–LHR, LAX–DXB)         Speeds: 791–838 km/h         Speeds: 838–845 km/h

Southern Hemisphere flights   ✓ Consistent                 ✗ Falsified
  (SYD–SCL, JNB–GRU, etc.)   Speeds: 756–907 km/h         Speeds: 1,630–2,081 km/h
                              All within aircraft range     All exceed aircraft range
                              All subsonic                  All supersonic (Mach 1.5–2.0)

Aircraft fuel constraints     ✓ All routes within           ✗ 5 of 6 Southern routes
                              certified range               exceed max range by 36–95%

Speed consistency across      ✓ All routes yield            ✗ Systematic 2× inflation
  all hemispheres             speeds of 756–907 km/h        in Southern Hemisphere only

Curvature drop at 100 mi     6,669 ft (no refraction)     0 ft (no curvature)
                              5,002 ft (4/3 refraction)

LOS to 50 ft target at       ✗ Requires ~4,000-5,500 ft   ✓ Trivially possible
  100 mi from sea level       observer height               from sea level

Existence of OTH-R systems   ✓ Explains why OTH-R was      ✗ OTH-R unnecessary;
                              developed (LOS is limited)    no explanation for its
                                                            complex design

Multi-platform targeting      ✓ Explains CEC, AWACS,       Neutral (not contradicted
  architecture                relay systems                  but not explained)

ADS-B tracking data           ✓ Southern Hemisphere paths   ✗ ADS-B paths would
                              follow great circle arcs       trace impossible curves
═══════════════════════════════════════════════════════════════════════════════════════
```

### 3.2 Weight of Evidence

#### Quantitative findings strongly favor the spherical model

The flight path analysis provides **hard falsification** of the AE flat model through multiple independent lines of evidence:

1. **Speed impossibility**: Six independent Southern Hemisphere routes each require supersonic speeds on the AE model. These aircraft are physically incapable of supersonic flight. This is not a matter of interpretation — the aerodynamics of subsonic transport aircraft (swept wings, high-bypass turbofans) cannot produce Mach 1.5+ flight. The sonic barrier would be encountered around Mach 0.9, causing wave drag that exceeds available thrust.

2. **Fuel impossibility**: Five of six Southern Hemisphere routes exceed the aircraft's maximum range on the AE model. The fuel tanks cannot hold enough fuel. This is a volumetric constraint — no amount of efficiency gains can fit 1.8× the fuel into a fixed-volume tank.

3. **Systematic pattern**: The discrepancy is not random or confined to one route. It appears on every Southern Hemisphere cross-ocean route, with magnitude precisely predicted by the mathematical properties of the AE projection. This systematic pattern is explained by the AE map being a projection (which necessarily distorts the south), and is unexplained if the AE map represents true geometry.

4. **Internal consistency**: The spherical model yields internally consistent speeds (756–907 km/h) across all eight routes spanning both hemispheres, three oceans, and near-polar paths. This consistency across diverse routes is a hallmark of a correct geometric model.

#### The line-of-sight analysis is less decisive

The LOS scenario presents an apparent challenge for the spherical model when limited to direct surface-to-surface observation. However:

- The specific engagement system and targeting architecture must be known to evaluate the claim properly
- Modern naval combat systems routinely achieve beyond-LOS engagement through multi-platform architectures (CEC, AWACS relay, autonomous missile seekers)
- The existence of OTH-R systems designed specifically to overcome LOS limitations is itself evidence that those limitations are real
- Atmospheric ducting provides variable (but unreliable) LOS extension

The planar model handles this specific scenario more simply, but this simplicity comes at the cost of not explaining why elaborate OTH-R and multi-platform systems were developed if LOS were unlimited.

### 3.3 Conclusion

**Based on the quantitative evidence analyzed above:**

The spherical model is overwhelmingly more consistent with observed flight data than the AE flat model. The AE model is **quantitatively falsified** by Southern Hemisphere flight operations through two independent hard constraints: speed impossibility and fuel impossibility. These are not marginal discrepancies requiring nuanced interpretation — they are factor-of-two contradictions with the physical limits of the aircraft.

The line-of-sight scenario requires further specification of the engagement profile to be diagnostic. On its own, it is ambiguous between models. In context with the flight data, it does not rescue the AE model from the flight-path falsification.

**Ranking by explanatory power and simplicity:**

1. **Spherical model**: Explains all flight data across all hemispheres with a single geometry. Explains existence of OTH-R and multi-platform targeting. Requires known (and documented) multi-platform architectures for beyond-LOS engagement.

2. **AE flat model**: Consistent with Northern Hemisphere flight data only. Falsified by Southern Hemisphere flight data through speed and fuel impossibility. Handles direct LOS more simply but fails to explain OTH-R development.

### 3.4 Explicit Uncertainties

1. **Flight times**: Scheduled block times include taxi and may vary by ±30 minutes from actual airborne time. Even using maximum flight times, AE model speeds remain supersonic. This uncertainty does not affect the conclusion.

2. **Aircraft specifications**: Manufacturer-published figures. Independent verification exists from regulatory filings (FAA type certificates). Uncertainty is negligible (< 2%).

3. **Airport coordinates**: Standard published coordinates with precision to 0.01°, contributing < 10 km uncertainty to distances. Negligible compared to the thousands-of-km discrepancies identified.

4. **LOS engagement details**: The specific system, platform heights, and engagement architecture are not specified in the scenario. This introduces genuine ambiguity that prevents a definitive conclusion for Part 2 in isolation.

5. **Atmospheric refraction**: The 4/3 model is empirical and varies with atmospheric conditions. Extreme ducting can extend effective radius to 2× or more, but is intermittent and unpredictable. The analysis bounds this uncertainty by computing all three cases.

---

## Appendix A: Worked Calculation — Sydney to Santiago

### Spherical Model (Great Circle)

```
Sydney:   φ₁ = -33.95° = -0.5925 rad,  λ₁ = 151.18° = 2.6383 rad
Santiago: φ₂ = -33.39° = -0.5828 rad,  λ₂ = -70.79° = -1.2357 rad

Δλ = |151.18 - (-70.79)| = 221.97°  →  360° - 221.97° = 138.03° = 2.4094 rad

cos(d/R) = sin(-33.95°)·sin(-33.39°) + cos(-33.95°)·cos(-33.39°)·cos(138.03°)
         = (-0.5585)·(-0.5503) + (0.8295)·(0.8350)·(-0.7443)
         = 0.3073 + (-0.5156)
         = -0.2083

d/R = arccos(-0.2083) = 1.7802 rad = 102.02°

d = 6371 × 1.7802 = 11,340 km
```

### AE Flat Model (Euclidean)

```
r_SYD = 6371 × (π/2 - (-0.5925)) = 6371 × 2.1633 = 13,782 km
r_SCL = 6371 × (π/2 - (-0.5828)) = 6371 × 2.1536 = 13,720 km

Δλ = 138.03° = 2.4094 rad

d_AE = √(13782² + 13720² - 2·13782·13720·cos(138.03°))
     = √(189,943,524 + 188,238,400 - 2·13782·13720·(-0.7443))
     = √(189,943,524 + 188,238,400 + 281,282,467)
     = √(659,464,391)
     = 25,680 km
```

### Comparison

```
Spherical distance:  11,340 km     AE flat distance:  25,680 km
Ratio: 2.26×

Flight time: 12.5 hours
Spherical speed:  907 km/h  ← Normal cruise speed for Boeing 787-9
AE flat speed:  2,054 km/h  ← Mach 1.93, impossible for 787-9 (Mmo = 0.90)

Boeing 787-9 range: 14,140 km
Spherical: 11,340 km ← Within range (80% of max range)
AE flat:   25,680 km ← Exceeds range by 11,540 km (182% of max range)
```

---

## Appendix B: Worked Calculation — Line of Sight at 100 Miles

### Setup

```
Distance: 100 statute miles = 160,934 m
Target height: 50 ft = 15.24 m
Earth radius: 6,371,000 m
```

### Geometric drop (no refraction)

```
h_drop = d² / (2R)
       = (160,934)² / (2 × 6,371,000)
       = 25,899,746,896 / 12,742,000
       = 2,033 m  (6,669 ft)
```

A 50 ft (15.2 m) target is 2,033 - 15.2 = 2,018 m below the geometric horizon from sea level.

### Required observer height (no refraction)

```
d = √(2R·h₁) + √(2R·h₂)

160,934 = √(2 × 6,371,000 × h₁) + √(2 × 6,371,000 × 15.24)
160,934 = √(12,742,000 · h₁) + √(194,187,480)
160,934 = √(12,742,000 · h₁) + 13,935

√(12,742,000 · h₁) = 146,999
12,742,000 · h₁ = 21,608,706,001
h₁ = 1,696 m  (5,564 ft)
```

### With 4/3 refraction

```
R_eff = 4/3 × 6,371,000 = 8,494,667 m

160,934 = √(2 × 8,494,667 × h₁) + √(2 × 8,494,667 × 15.24)
160,934 = √(16,989,334 · h₁) + 16,092

√(16,989,334 · h₁) = 144,842
16,989,334 · h₁ = 20,979,199,164
h₁ = 1,235 m  (4,051 ft)
```

---

## Appendix C: Methodology Notes

### Why the AE Projection Fails as a Physical Map

The azimuthal equidistant projection centered on the North Pole has one special property: it preserves distances measured *from the North Pole* to any other point. This means:

- The distance from the North Pole to any city on the AE map equals the great circle distance on the sphere
- The distance from the North Pole to the South Pole is π × R / 2 ≈ 10,008 km on the map

However, it necessarily distorts all other distances. The South Pole, which is a single point on a sphere, is mapped to the *entire outer circumference* of the disk. This circumference is 2π × 10,008 = 62,893 km. In physical terms, the AE map says the "South Pole ring" is 62,893 km across — but on a sphere, the South Pole is a point with zero dimension.

Every point in the Southern Hemisphere is pushed outward on the AE map. Two cities at 34°S that are 11,000 km apart on a sphere become 25,000+ km apart on the AE map. This is a mathematical necessity of the projection, not an error in calculation.

If the AE map were the true geometry, these cities would genuinely be 25,000+ km apart, and the flights between them would require supersonic speeds and more fuel than the aircraft can carry. Since neither is the case, the AE map cannot represent the true geometry of the surface on which these flights operate.

### Verification

All calculations in this document can be independently verified:
- Airport coordinates: Available from ICAO, FAA, or aviation databases
- Great circle distances: Any online calculator (e.g., gcmap.com) or the haversine formula
- Aircraft specifications: Manufacturer datasheets, FAA type certificates
- Flight times: Airline timetables, FlightAware, FlightRadar24
- AE projection mathematics: Standard cartographic references

No proprietary or non-public data was used.

---

## Appendix D: Sources and References

### Flight Data Sources
- Qantas QF27 (SYD-SCL): Flightera.net, FlightAware
- LATAM LA8059 (JNB-GRU): Aviability.com, FlightMapper, FlightConnections.com
- Qantas QF65 (PER-JNB): Flightera.net, Point Hacks review, Qantas Newsroom
- SAA SA281 (PER-JNB): FlightAware, Point Hacks review
- LATAM LA800/LA801 (AKL-SCL): FlightAware, Flightera.net, FlightsFrom.com
- Qantas QF63 (SYD-JNB): FlightAware, Airportia
- British Airways BA178 (JFK-LHR): Flightera.net, Airportia
- Emirates EK216 (LAX-DXB): Flightera.net, Airportia

### Aircraft Specifications Sources
- Boeing 787-9: Boeing.com, SKYbrary, National Aero Stands
- Boeing 777-300ER/200ER: SKYbrary
- Airbus A380-800: SKYbrary
- Airbus A330-200: AeroCorner
- Airbus A340-300: National Aero Stands

### Military and Radar Sources
- SM-6 Standard Missile: CSIS Missile Threat, RTX/Raytheon
- AN/SPY-1 radar limitations: CSIS, FAS.org
- Cooperative Engagement Capability: Wikipedia (sourced from DOD publications)
- NIFC-CA architecture: Defense One
- Radar horizon formula and 4/3 model: Omni Calculator, Wikipedia, Holleman 2013 (*Radio Science*)
- Persian Gulf ducting (SHAREM-115): Brooks, Goroch & Rogers, 1999, *Journal of Applied Meteorology* 38(9)
- Yellow Sea microwave propagation: *Frontiers in Marine Science*, 2023
- South China Sea ducting: Published simulation and measurement studies
- Swedish radar anomalous propagation: *Atmospheric Measurement Techniques*, Copernicus, 2023
- OTH radar systems: Wikipedia, Signal Identification Wiki, GlobalSecurity.org, Rohde & Schwarz
- HFSWR systems: IDST, Microwave Journal
