# 1.21.1-0.1.3 (BiggerReactors-Rework, unreleased)

## Industrial viewer visuals (2026-10-27)
 - use the machine's decorated animated fluid gauges in JEI and EMI, preserving ingredient interactions
 - remove the empty dark tooltip over the Industrial Processor operating-condition label


## Correct hot-volume yield and recipe-viewer layout (2026-10-27)
 - correct obsidian's yield target to steam per equal volume of hot coolant, not nuclear fuel: 1000 mB hot obsidian gives 9000 mB ideal steam versus 7500 mB for hot Cryo
 - cap fractional phase-change heat at the available batch volume
 - show both Industrial Processor recipes in JEI/EMI with input water beside FE and liquid product in the output tank, preserving searchable ingredients


## Industrial processing and obsidian coolant revision (2026-10-27)
 - expand the existing Cyanite Reprocessor into the Industrial Processor while preserving its registry IDs, crafting recipe and cyanite processing
 - add obsidian and water processing into Liquid Obsidian, a separate product tank, fluid output routes, automatic ejection and the revised water/energy layout
 - replace the lava coolant pair with identified heated Liquid Obsidian; legacy pairs stop as invalid until manually emptied
 - give hot obsidian 20% more heat per mB than unchanged hot Cryo Fuel, verified with equal-volume exchanger batches; leave other coolant and turbine coefficients unchanged
 - update native recipes, JEI/EMI, guide, locales and protocol 4 synchronization; verify 56 unit tests and 79 GameTests per environment


## Battery terminal and Unicode fixes (2026-10-27)

- Move the item-charging slot to the top (+) and the item-discharging slot to the bottom (-), preserving persisted slot IDs and energy direction.
- Charge equipped armor and offhand FE items only while a valid battery terminal is open on the server; share the output budget with item slots, ports and other viewers.
- Deduplicate equipment charging within each server tick and synchronize modified item components through the menu.
- Correct corrupted dimension/conversion separators and serialize new ROBN strings as length-prefixed UTF-8 with an explicit type tag. Retain legacy string decoding and bounds checks; reject malformed UTF-8.
- Bump internal network protocol to 3 so incompatible clients cannot decode the new string representation.
- Add Unicode round-trip, legacy compatibility and malformed-payload tests, plus a real battery/menu GameTest covering equipment charging, closing, shared limits and plus/minus behavior.

## Contained-fluid rendering and reactor repair (2026-10-27)
 - render only exterior fluid surfaces, omitting shared phase, segment and bend caps; use consistent UVs, outward normals, face culling, native tint alpha and fluid emission light
 - use vanilla cutout rendering for opaque contained fluids and the culled translucent layer for translucent tints, avoiding forced transparency behind machine glass without depending on Iris
 - preserve coolant profile/origin alongside simulation volumes when disassembling or rebuilding a reactor; fixes stored coolant becoming unidentified and rejecting refill after casing repair
 - show unidentified coolant instead of Air for orphaned stored amounts; require an explicit coolant-port manual dump before refilling an already unidentified tank, without silently changing its contents
 - add exterior-mesh/winding regression tests and real reactor repair/refill tests

## Turbine reload regression (2026-10-26)
 - resolve queued controller aliases before checking emptiness or removing controllers from the registry; prevents loaded turbine parts and rotor bearings from remaining split between controllers
 - use deterministic inventory/size/creation-order merge selection while preserving the independent-inventory protection
 - add fresh, stocked, staged, alias-chain and shuffled saved-turbine regression cases; exclude their fixtures from distribution artifacts

## Production audit fixes (2026-10-26)
 - validate binary lengths/nesting before collection allocation, reject nontext compound keys and bind GUI packets to their menu and position (internal protocol 2)
 - validate rod commands and numeric configuration bounds, including open endpoints and nonfinite numbers
 - replace removed persistence delegates; preserve machine origin across parts and refuse merging independently stocked machines, including inventories awaiting NBT load
 - respect liquid output protection when settling pending asynchronous radiation
 - reprocessor automatic input uses recipe fluids and selects matching fluid alternatives; machine recipes stay out of the vanilla crafting book
 - add recovery to transition data maps, distinguish empty client snapshots from missing sync and clear coolant cache on disconnect
 - derive only eligible heated sprites, repair Cryo Fuel texture fallback and label turbine generation as gross without changing turbine mechanics
 - remove five unused textures and 64 duplicate JSON keys; synchronize locale keys, keeping English fallback where Chinese translations are unavailable
 - exclude GameTests and their empty template from the distribution jar; keep generated numerical studies local

## Liquid cooling
 - added measured restricted-extraction experiments for obsidian-to-lava reactors and lava/water exchangers, checking steady bottlenecks, complete blockage, recovery and per-tick fluid/heat balances
 - active reactors now reduce burn and coolant conversion linearly with persistent hot output stock; exchangers limit both conversions from retained steam only, preserving turbine behavior and solid-mode generation
 - output monitoring ignores normal transfer batches cleared within one second, stops immediately at full capacity and recovers by up to five percentage points per tick; saved monitoring state survives reloads
 - added output occupancy, retained occupancy and operating-limit statistics with localized bottleneck/recovery reasons, plus guide explanations in English and Portuguese
 - reactor thermal protection now accounts for the output conversion limit when estimating sustainable heat removal
 - reactor manifolds and exchanger channels now fill their whole visual path using the relative cold/hot proportions, including abrupt stock changes and tiny nonempty amounts; actual inventory gauges and conversion remain unchanged
 - corrected sodium's heat-exchange temperature so the same reactor and exchanger produce more steam with sodium than Cryo Fuel or Liquid Obsidian; added a finite closed-loop comparison of all coolant tiers using identical geometry and control rods
 - preserved existing sodium inventories across this specific temperature correction without accepting unrelated coolant profile changes
 - JEI and EMI coolant entries show heat-exchange temperature and the correct destination of the hot output instead of labeling coolant heat capacity as FE/mB
 - empty coolant tank pairs now clear their selected fluid, origin and fractional heat so another coolant can enter; fixes both manual emptying and loading previously empty saves, while keeping nonempty fluids separated
 - added an automated water/obsidian comparison for the saved 6×6×6 reactor, preserving individual rod insertions and checking finite closed-loop coolant conservation and steady-state steam/fuel progression
 - fixed a client crash when rendering exchanger fluids: channels resolve assembly through their terminal instead of reading an ASSEMBLED property they do not have
 - assembled turbines now send rotor geometry even at zero RPM and accept geometry arriving before the assembled block state
 - repaired 119 malformed English color codes that displayed stray characters in machine screens and tooltips
 - both heat exchanger circuits now display their input/output fluids inside straight channels and elbows, sharing tube space according to synchronized tank proportions; only aggregate views are synchronized during operation
 - manifold fluids now span continuous vertical columns, with hot coolant rising from below and cold coolant descending from above in shared space
 - heat exchangers retain operating temperatures on load/reassembly; statistics identify warmup, partial supply and indirect heat transfer, and show channel temperatures
 - the guide and both channel item tooltips explain face contact, indirect transfer across a gap and the full-inlet steady-state assumptions behind potential conversion
 - heated coolant texture generation now isolates external fluids without client texture properties, preventing an atlas reload failure and subsequent duplicate mod setup registrations

## Battery
 - recipes are more expensive: the casing uses blutonium ingots instead of iron, the energy port uses redstone blocks, and the terminal and cell use ludicrite ingots
 - fixed a crash when saving and closing a world with a battery, caused by cells updating their neighbors while their chunks were unloading

## JEI and EMI
 - added the Heat Exchanger category with indexed hot-to-cold coolant conversions and water-to-steam evaporation; terminal, both channel types and fluid port expose it, and optional hot fluid origins remain intact
 - moderator blocks, moderator fluids and turbine coils now show each property as a colored bar, scaled to the best value available and colored by how it ranks against the other options
 - hovering a property explains it and lists its benefits and drawbacks
 - reactor coolant and heat exchange pages now follow the moderator page layout and width: cold and hot fluids stacked with an arrow, names on top, ranked bars for heat per mB, temperature, conductivity and recovery, and notes for the destination or circuit
 - statistics side tab sits under the appearance tab on the same ear instead of overlapping it on reactor, turbine and heat exchanger terminals
 - statistics window grouped into Structure, Fuel, Fluids and Performance sections, with the operating condition pinned on top, terminal-style units and a bar comparing current and potential rates
 - guide coolant tables keep names at normal size, sliding inside their column, and show the fluid on hover; the heat exchanger chapter lists the fluids it exchanges
 - guide models no longer list or draw empty interior spaces as air, and reactor manifolds are drawn closed with coolant inside instead of showing only their outer faces
 - statistics window can be dragged by its title and uses its own icon on the side tab
 - side tabs, ears and open popups are reported to the recipe viewer, which moves its ingredient list out of the way instead of covering the screen
 - recipe viewer tooltips are drawn above the reprocessor's face configuration window
 - appearance, statistics and face configuration popups can stay open together and close in any order; the last one clicked comes to the front, and each popup only blocks the area it covers

# 1.21.1-0.1.0 (BiggerReactors-Rework)

Independent fork and rework of BiggerReactors. Mod id `biggerreactorsrework`, meant for new worlds:
worlds and machines from the original mod are not migrated.

## Port
 - main machine screens (reprocessor and reactor/turbine/heat exchanger terminals) have a Settings side tab with a draggable Light/Dark popup and persistent per-machine or shared appearance; darkening covers slots, buttons and neutral gauge artwork, preserving casing frames, icons, items, fluids and colored fills
 - optional Dark Mode Everywhere integration excludes this mod's drawing code from its shader and text recoloring
 - ingot sprites redesigned with a compact Minecraft-style silhouette and each material's color; ingots now use the standard generated item model, and the wrench sprite depicts a steel open-end wrench
 - lowered both redstone screens' side tabs to match the face configuration window's top inset, keeping icons, tooltips and click areas aligned
 - turbine redstone configuration now uses the reactor's metal panel, icon tabs, selectors and apply/revert glyphs, with contextual explanations, correct units and validation for thresholds and flow percentages
 - added the Jade energy-bar option's missing English and Portuguese translation, preventing its development-time assertion from interrupting the initial resource reload
 - guide cover's raised center seal removed to reveal the updated cover art; page headings, muted labels, highlights and material table headers now use coordinated green inks
 - reactor, turbine and heat exchanger connected glass now closes inward hazard-stripe corners around terminals, ports and other gaps, using native chunk models and the existing border textures
 - guide material pages now follow ore smelting, graphite smelting, reactor waste, cyanite reprocessing and ludicrite acquisition instead of storage unpacking; recipe arrows and quantity symbols corrected
 - guide consolidated into five chapters: Getting started, Materials, Reactor, Turbine and Heat exchanger; cyanite reprocessing stays in Materials, and energy/vapor modes share the Reactor chapter
 - material property tables group identical air/material variants, resolve readable names, show separate property columns with explanatory hover tooltips and filter unavailable entries; synchronized client material caches are cleared before replacement
 - material names scroll at their normal font size inside table cells; hover shows the material icon and readable name, and rows sort by increasing efficiency (E) with deterministic ties
 - multiblock size limits shown in text using current configuration; maximum-size 3D preview removed, keeping small assembly examples, zoom, facing material lists and yellow casing/glass hover outlines
 - blutonium acquisition explicitly identifies the Cyanite Reprocessor, with its crafting recipe, processing recipe and operating instructions together in Materials
 - turbine redstone port uses its own lit and unlit textures, with the lit state following command input or sensor output
 - port to Minecraft 1.21.1 and NeoForge 21.1 (ModDevGradle, Java 21)
 - mod id, registry namespace, resources and Java package moved to `biggerreactorsrework`
 - creative tab named BiggerReactors-Rework
 - datapack files moved to the 1.21 layout and formats (recipes, loot tables, tags, biome modifiers); common tags use the `c` namespace
 - no external library mods required: the multiblock, registration, config, data loading, networking and screen code the mod used is now part of the mod
 - the Phosphophyllite and Quartz submodules were removed from the repository
 - turbine rotor drawn by a block entity renderer and heat exchanger channels baked into the chunk mesh, keeping the previous models and transforms
 - OpenCL/Vulkan reactor simulation removed, along with its shaders and the `allowAcceleratedSimulation` config option; the reactor uses the CPU simulation
 - the unregistered `reactor2` prototype is left out of the build for now

## Battery
 - new battery multiblock: casing, connected glass, terminal, energy ports and cells, from 3x3x3 up to 32x32x32 (configurable)
 - each cell stores 3.2 GFE and adds 300 kFE/t to the input limit and 300 kFE/t to the output limit; capacity and limits grow linearly with the cell count
 - an assembled battery needs a terminal, at least two energy ports and at least one cell; if every port points the same way on assembly, one is switched
 - energy ports switch between input and output with a wrench or from their screen
 - right-clicking any battery block other than a port opens the terminal: charge gauge, last tick's input and output, transfer limit, discharge and charge item slots, player inventory and an armor/offhand tab
 - every cell keeps its share of the stored energy: a broken cell keeps it as an item (shown in its tooltip) and brings it into the next battery it joins
 - connected, animated cell textures; cells light up when the battery is assembled and animate while it holds energy
 - Jade shows the battery's total energy with the mod's energy bar on any block of an assembled battery
 - new guide chapter with the battery's blocks, recipes, size limits and a 3D example

## Cyanite Reprocessor
 - restored (it was disabled in the original 1.20.4 code): 2 cyanite ingots + water + energy make 1 blutonium ingot, with the original config values
 - water in the tank is kept when the world reloads (it was saved and read under different names)
 - accepts energy from other blocks but does not give it back
 - side configuration window, opened from a tab on the right of the screen, with Items, Water and Energy tabs: each face (except the front) can be set as cyanite input, blutonium output, both, water input or energy input; all faces accept energy by default
 - optional auto input (pulls cyanite and water from neighbors) and auto eject (pushes blutonium to neighbors), per tab
 - its recipe shows in JEI and EMI, drawn like the machine's screen; clicking the progress arrow opens it
 - Jade shows the Cyanite Reprocessor's energy with the mod's own energy bar instead of Jade's default one; the reactor and turbine power taps show their battery the same way

## Screens
 - gauge bars (fuel mix, heat, turbine speed, fluid tanks, control rod and coolant flow) have their own textures under `textures/gui/<bar>/`, separate from the screen backgrounds, with the same look as before
 - new fuel, waste, heat and turbine speed bars, with an animated fill like the energy bar
 - new fluid tank and control rod bars: fluids keep their own animated texture and color at 80% opacity, with a transparent reflection moving over them
 - screens are drawn in code instead of background images (panel, slots, buttons, side tabs, selectors, text boxes, the coolant flow tank), looking the same as before; each icon is its own file under `textures/gui/icons/`
 - side configuration window drawn the same way, with smaller automation and close buttons and their own small glyphs
 - new toggle design: on/off switches and lit-cell selectors with a dark frame that turns cyan on hover; the turbine vent selector reads no exhaust, overflow only, all exhaust from left to right
 - every button (eject, arrows, apply, revert, enter, side configuration) in the same style: slate fill with a light border, cyan outline on hover, teal fill when an automation switch is on
 - status, gauge, heat exchanger and fluid symbol icons redrawn by hand, same sizes and places

## Blocks
 - reactor, turbine and heat exchanger terminals: once the multiblock is assembled, only the face pointing out of the structure shows the terminal screen and the other faces show the disassembled casing texture; disassembled terminals look the same as before

## Recipe viewers
 - EMI support, with the same categories as JEI: cyanite reprocessing, reactor moderator blocks and fluids, turbine coils; the JEI plugin steps aside when EMI is installed

## Fixes
 - liquid uranium, liquid obsidian and steam show their names again instead of the `fluid_type` translation key
 - menu state sync runs on the server thread instead of a separate thread reading the world
 - menu and data sync packets are actually serialized, so they work on dedicated servers
 - client requests are only accepted for the menu the player has open
 - the disabled cyanite reprocessor no longer breaks the `minecraft:mineable/pickaxe` and `minecraft:needs_iron_tools` tags
# 1.20.1-0.6.0-beta.10.2

## Reactor
 - fix coolant port not accepting coolant
 - add Vulkan backed experimental simulation
 - simulation fully able to be loaded without Neo, backing web accessible simulator

# Misc
 - migrate to Neo

# 1.20.1-0.6.0-beta.10.1

## Reactor
 - fix power tap capability crash bug (#84)

## Turbine
 - fix power tap capability crash bug (#84)

## Misc
 - fix uranium ore generation (#86)
 - add new material blocks
 - update material and ore block textures

# 1.20.1-0.6.0-beta.10

## Reactor
 - ported to new multiblock system
 - power tap energy handling updated
 - fix manifolds being ignored in a passive reactor

## Turbine
 - ported to new multiblock system
 - power tap energy handling updated
 - fix coil/blade validation bug

## Heat Exchanger
 - ported to new multiblock system
 - channels rendered with quartz, reduces hitches when placing/removing blocks in renderchunks containing lots of them

## Misc
 - JEI integration is back, and works in multiplayer too 

# 1.19.2-0.6.0-beta.6.1

## Reactor
 - fix bug with active reactor output multiplier
 - fix issue where fluid may not be pushed into reactor properly
 - fix NPE with restone port

## Misc
 - Update phos, breaking changes, contains multiblock bug fixes

# 1.19.2-0.6.0-beta.6

## Reactor
 - add thermal fluids as moderators
 - fix lava moderator json
 - work around a weird blockstate issue, actual problem not fixed but it shouldn't crash anymore

## Misc
 - update Phos, breaking changes

# 1.19.2-0.6.0-beta.5.1

## Reactor
 - scale radiation absorbed with amount of fuel, oversight from 1.5.2 BR, likely needs adjusting
   - use advanced config option Reactor.fuelRadScalingMultiplier to adjust

# 1.19.2-0.6.0-beta.5
 - 1.19.2

# 1.18.2-0.6.0-beta.1.3

## Reactor
 - added GUI button to dump coolant
 - added CC function to dump coolant
 - Fix mekanism gas pushing

## Heat Exchanger
 - added GUI button to dump fluid
 - added CC function to dump fluid
 - Fix mekanism gas pushing

## Turbine
 - increased default flow rate adjustement amounts by factor of 10x
   - also configurable now
 - Fix mekanism gas pushing

# 1.18.2-0.6.0-beta.1.2
 mekanism, its a thing again

## Reactor
 - new simulation implementation, no mechanic changes
   - time sliced (default)
   - full pass
     - single thread
       - faster than previous implementation
     - multi thread
       - faster than previous implementation
     - OpenCL Accelerated
       - requires addition of LWJGL binaries and a compatible OpenCL runtime, tested against and optimized for a Instint MI25 running under ROCm on Ubuntu 20.04
       - POCL is known incompatible

## Misc
 - re-enable mekanism gas integration

# 1.18.2-0.6.0-beta.1.1
 hot not-a-fix

## Misc
 - handle a CME better to give better debug info

# 1.18.2-0.6.0-beta.1
 - 1.18.2

## Reactor
 - handle re-validation

## Turbine
 - handle re-validation
 - coolant port renamed to fluid port

## Heat Exchanger
 - handle re-validation
 - coolant port renamed to fluid port

## Misc
 - lower default uranium generation amount
 - better reload handling

# 1.18.1-0.6.0-beta.0.3

## Reactor
 - Ensure reactors dont reset themselves to inactive when reloading
 - Fix waste ejection state saving
 - Add out of fuel simulation bypass

## Misc
 - Tool requirements and block break times, they exist again
 - Dont register tag for deleted fluid

# 1.18.1-0.6.0-beta.0.2

## Reactor
 - Fix crash when placing computerport when computercraft is not installed

# 1.18.1-0.6.0-beta.0.1

## Reactor
 - Fix coolant tank saving
 - Switch simulations to using Fibonacci sphere for ray generation

# 1.18.1-0.6.0-beta

## Reactor
 - Fix sim issue where the ray origin was offset
 - Fix manifolds crashing passive reactors
 - Don't save fuel tank capacity
 - Distribute fuel/waste residual instead of voiding it
 - Add RF checks to ensure buffers dont go negative

## Turbine
 - Ensure rotor direction is consistent
 - Add RF checks to ensure buffers dont go negative

## Misc
 - Cyanite reprocessor saving fixed
 - Quartz mod dependency, previously part of Phosphophyllite

# 1.18.1-0.6.0-alpha.9.1

## Reactor
 - dont constantly update full fuel rendering level

## Misc
 - Fix locale issue

# 1.18.1-0.6.0-alpha.9
1.18.1
 
## Misc
 - Phos update

# 1.18-0.6.0-alpha.8.1
ComputerCraft, again

## Reactor
 - Re-enable computer port

## Turbine
 - Re-enable computer port

## Heat Exchanger
 - Re-enable computer port

## Misc
- Fix config
   - Don't inline initialize the variables, JVM will inline those

# 1.18-0.6.0-alpha.8
Now **THAT** is a cave

## Turbine
 - dont double drain water from internal tank

## Heat exchanger
 - Mark dirty for saving

## Misc
 - 1.18
 - updated some ids

# 1.17.1-0.6.0-alpha.7
Its a bird, its a plane, its a, rock?

## Turbine
 - Rotor is rendered using Quartz render system
 - Fix validation

## Misc
 - Fix recipes that use copper

# 1.17.1-0.6.0-alpha.6

## Reactor
 - Fix NPEs caused by new simulation interface
 - Implement simulation description serialization
 - Add version to CC API
 - Temperatures are in Kelvin now
 - Remove range limit for max size

## Turbine
 - Add version to CC API
 - Remove range limit for max size

## Heat Exchanger
 - Add version to CC API
 - Add config options for max size

# 1.17.1-0.6.0-alpha.5
Not your grandpa's reactors anymore

## Reactor
 - Remove classic simulation
 - correctly write to rfTransferredLastTick
 - switch to new internal simulation interface
   - allows simulation to be loaded independent of Forge/Minecraft
 - ensure EMTPY_MODERATOR isn't going to cause divide by zero errors

## Turbine
 - Remove classic simulation

## Misc
 - Move config to new object based system

# 1.17.1-0.6.0-alpha.4
I'm a strong independent mod that doesn't need any other repository to build me 

## Misc
 - Built directly from BiggerReactors repo, w/ Phosphophyllite submodule
 - Oregen fixed (mostly in Phos)
 - Config format changed to TOML by default, will still read/use json5 for existing configs

# 1.17.1-0.6.0-alpha.3.1
update forge to 37.0.53

## Reactor
 - improve chunk unload time

# 1.17.1-0.6.0-alpha.3
*I AM, **S P E E D***

~~except on unload, that needs work~~

## Reactor
 - Assembly tick optimizations, 50% magic
 - Delegate to correct capability super method

## Turbine
 - Assembly tick optimizations
 - Delegate to correct capability super method
 - Query for ASSEMBLED blocks state existence before value

## Heat Exchanger
 - Delegate to correct capability super method
 - Query for ASSEMBLED blocks state existence before value

## Misc
 - Switch to parchment mappings

# 1.17.1-0.6.0-alpha.2
ComputerCraft

## Reactor
 - Re-enable computer port
 - fix possible deadlock state and race condition

## Turbine
 - Re-enable computer port
 - fix possible deadlock state and race condition

## Heat Exchanger
 - Re-enable computer port
 - Fix stack overflow with coolant port
 
# 1.17.1-0.6.0-alpha.1
Modular multi-blocks

## Turbine
 - Fix stack overflow with rotor bearing
 - Fix maybe rendering 

## Misc
 - update to Phos 0.6.0-alpha.1, and its multiblock changes


# 1.17.1-0.6.0-alpha
1.17.1

# 1.16.5-0.5.0
That's all folks!

Next stop, 1.17!

## Reactor
 - Add experimental reactor simulation
   - cripples servers on all cores, not recommended for normal use
 - GUI displays fuel heat instead of casing heat
 - Sync CC functions properly
 - Increase internal battery size

## Misc
 - Animated terminal textures (thx Ridanisaurus)
 - All parts of a tooltip will give their denomination
 - Reduce tile update size, avoids chunkbaning when building large reactors/etc

# 1.16.5-0.5.0-beta.3

## Reactor
 - fix manifold recipe
 - Add allthemodium/vibranium/unobtainium fluid/vapor moderator support
 - Add moderator values for superheated sodium, lava, and liquid obsidian
 - Add fluid support to moderator registry
 - Have manifold only consider cold side for dynamic moderator properties

## Heat Exchanger
 - Use correct tank sizes in GUI

## Misc
 - Add allthemodium/vibranium/unobtainium fluid/vapor transitions

# 1.16.5-0.5.0-beta.2

## Reactor
 - Fix negative power bug
 - Keep CC modem connected on reload
 - fix controlRodCount CC function

## Turbine
 - Fix negative power bug
 - fix CC battery function
 - fix CC vent controls
 - fix CC coil controls

# 1.16.5-0.5.0-beta.1
99 little bugs in the code, 99 little bugs. \
Take one down, patch it around, 127 little bugs in the code...

## Reactor
 - Fix CC battery function
 - Remove extra underscore from CC peripheral name
 - Add manifold crafting recipe

## Turbine
 - Fixed NPE crash
 - Fixed displaying NaN efficiency in some cases
 - Remove extra underscore from CC peripheral name

## Heat Exchanger
 - Updated the GUI to better reflect internal workings
 - Remove extra underscore from CC peripheral name

## Misc
 - Correct itemgroup key in lang file

# 1.16.5-0.5.0-beta
Still april fools, right? no? That was last week? *well fuck.*

## Reactor
 - FissionEventsPerFuelUnit increased by 10x for modern simulation
 - Tank size increased by 100x for modern simulation
 - Mekanism gas handling re-dded, handles all gases with valid fluid, not just steam
 - Manifold model/texture added, modified/corrected assembly behavior
 - GUIs switch on the fly, no longer requiring re-opening to switch type
 - New OOP CC API to match new simulation, works for old one as well (subject to race conditions)

## Heat Exchanger
 - Increase tank size
 - Increase internal surface area
 - Increase conductivities
 - Increase channel heat capacity
 - Terminal added (GUI still needs work)
 - Computer port added, CC API that matches new OOP based ones for reactor/turbine
 - Manifolds add to tank capacity
 - Allow power to be pulled from power taps
 - New fuel rendering textures (thx Ridanisaurus)
 - Errors in lang file

## Turbine
 - New modern simulation
    - Drag is squared, not linear/fixed
    - Rotor speed responds quicker
    - Rotor speed not coupled to blade area directly
    - Rotor fluid capacity tied to rotor RPM, faster rotor more capacity
 - Increase steam capacity per blade by 10x for modern simulation
   - Also increase coil drag and rotor mass by 10x, so, turbines are 10x denser now
 - Support for multiple coil layers (exact behavior may change)
 - New OOP CC API to match new simulation, works for old one as well (subject to race conditions)
 - Selective transition selection, so only steam, other stuff in the heat exchanger
 - State saves
 - Allow power to be pulled from power taps
 - GUI displays correct fluid being used

## Misc
 - GUIs retextured (thx Ridanisaurus)
 - Bucket textures added (#2) (thx Ridanisaurus)
 - Textures rearranged, texture packs may need to update
 - Yellorium renamed to Uranium, lang file only, ID kept the same
 - Uranium bucket no longer registered
 - Add superheated_sodium fluid for mekanism sodium compatibility

# 1.16.5-0.5.0-alpha.1
The steam is lava, the steam is lava

## Reactor
 - GUI displays correct fluid
 - manifolds added (no model/texture yet)
 - config option to switch between simulations added
 - modern simulation active cooling mode fixed
 - modern simulation saves its data
 - power extraction draws from internal energy correctly
 - fix fuel level rendering

## Turbine
 - uses transition registry for fluids
 - simulation decoupled from multiblock controller 
 - fix rotor renderer

## Misc
 - correct handling of transitions with multiple inlet fluids

# 1.16.5-0.5.0-alpha.0
Boldly go where no reactor has gone before

## Reactor
 - capability to boil fluids other than water
 - new simulation with radiation moving in all directions, identical mechanics
 - new simulation corrects heat transfer rates

## Heat Exchanger
 - added

## Misc
 - new textures all around (thx Ridanisaurus)
 - added fluid obsidian (mostly for testing, may end up staying)

# 1.16.4-0.4.3

## Turbine
 - correct tank size and flow rate calculations

## misc
 - move JEI integration to remove hard dependency

# 1.16.4-0.4.3-beta
I tip my hat to you, one legend to another

## Reactor
 - Fix NPE with access port

## Turbine
 - power port reports power 

# 1.16.4-0.4.2
I rate this bug, 0/0

## Reactor
 - Fix CC Nan issue, #50

# 1.16.4-0.4.1

## Turbine
 - fix GUIs

# 1.16.4-0.4.0
hotter, bigger, faster, stronger, *wait, that doesn't sound quite right*

## Reactor
 - default size limit increased to 128x128x192
 - fixed bug with control rod heat transfer
 - corrected fuel heat display
 - added moderator registry JEI integration (thx MutantGumdrop)
 - fix rare NPE with coolant port
 - correct enderium registration
 - send blockupdate in front of the redstone port to signal torches
 - access port manual ejection works
 - missing CC API functions added
 - moderators added, check the list

## Turbine
 - added coi registry JEI integration (thx MutantGumdrop)
 - missing CC API functions added
 - flow rate/tank size now scales with physical turbine size, not blade count

# Cyanite Reprocessor
 - fix voiding issues

## Misc
 - config options flagged as advanced
 - fixed GUI update bug
 - CSV and wikimedia moderator/coil exports added to github actions (thx MutantGumdrop)

# 1.16.4-0.3.0
you spin me right round baby

## Reactor
 - new GUIs
 - control rods can be named now
 - give correct y level for fuel rod gaps
 - some more moderators
 - yellorium tag accepted for fuel
 - errors with both coolant port and power tap
 - moderator registry moved to datapack
 - fix some redstone port state saving bugs
 - minimize fuel rod updates at assembly
 - correct getEnergyStoredUnscaled CC API name

## Turbine
 - new GUIs
 - rotor animation, it exists now
 - coil registry moved to datapack
 - fix near edge rotors confusing casing/glass as coil
 - add rotor_bearing_count error to lang file

## Misc
 - extranious blockstates reduced
 - tag yellorite ore
 - reprocessor also has a new GUI
 - fix reprocessor voiding blutonium when output slot is full 
 - LGPL v2.1 now
 - 1.16.4

# 1.16.3-0.2.6
1/0 bugs

## Reactor
 - fix divide by zero bug with fuel rendering

# 1.16.3-0.2.5
Redstone port bugs

## Reactor
 - fix active reactor overheating when output multiplier was > 1, and it had coolant
 - allow power to be extracted from the power tap, not just pushed out
 - fuel can now be extracted from a reactor 
 - add unscaled methods to CC peripheral
 - fix redstone port energy percent reporting incorrectly
 - fix redstone port not recommitting values after a world reload
 - added mode switch button for control rod insertion
 - fix redstone port powering themselves through the reactor
 - optimize fuel rendering updates

# 1.16.3-0.2.4
Blocks and bugs

## Reactor
 - accepts uranium and blutonium blocks as fuel
 - renders fuel, shows to accuracy of 1/16th of a block
 - Dynamic battery size, default 10k RF per external block
 - Steam tank size limit removed
 
## Turbine
 - Dynamic flow rate/tank size, based on blade count
 - Dynamic battery size default 30k * (coil block count + 1)
 - increased default max turbine length to 192
 - Allthemodium, Vibranium, and Unobtanium added to possible coils

## Misc
 - storage block items are tagged now, should work with other recipes and mods correctly
 - Fix CC API backwards compatibility with new battery sizes, scales it to the old sizes

# 1.16.3-0.2.3
Connecting, textures

## Reactor
 - Connecting, textures, for the glass
 
## Turbine
 - Connecting, textures, for the glass
 - fix dangling coil bug
 - add errors to lang file

## Misc
 - More ER2 compatibility
 - tag yellorite ore item as uranium

# 1.16.3-0.2.2
Bug fixes! *again*
And compat

## Reactor
 - Fix redstone port recipe

## Turbine
 - Fix a new turbine not assembling
 
## Misc
 - Compatibility with ER2, tagging for items/blocks and recipes where applicable

# 1.16.3-0.2.1
Bug fixing

## Reactor
 - Fix NPE with Mekanism gas handler
 - Allow access ports to be crafted with any wooden chest

## Turbine
 - Fix NPE with Mekanism gas handler

# 1.16.3-0.2.0
Redstone Port

## Reactor
 - Added redstone port
 - Added air types to default moderator list
 - Increase default maximum size to 64x64x96
 - Increase config maximum size limit to 192x192x256
 - Computer Port drop added
 - Change transparent blocks sound type to glass, mostly for the glass
 - Fix overproduction bug with a production multiplier in wrong place, added more config options to allow tuning of both passive and active output separately
 
## Turbine
 - Increase default maximum size to 64x64x96
 - Increase config maximum size limit to 192x192x256
 - Computer Port drop added
 - Input/Exhaust GUI icons corrected
 
## Misc
 - Bucket added for irradiated steam
 - Yellorium fluid shows up in JEI now, it was transparent

# 1.16.3-0.1.9
CC Bug fix

## Reactor
 - Clamp control rod insertion with CC API

# 1.16.3-0.1.8
ComputerCraft API

## Reactor
 - Added ability to get the maximum amount of fluid that *could* have been produced this tick
 - Added ability to query battery size, as it is now configurable
 - Ported ability to query active reactor tank size, missed in first round
 
## Turbine
 - Added ability to query batter size, as it is now configurable

## Misc
 - update to zh_cn translation

# 1.16.3-0.1.7
more bug fixes

## Reactor
 - Fix loading issue due to mekanism steam compatability
 - Fix crash when using gui to toggle directions
 - Fix dupe glitch with mekanism pipes, also causes log spawm

## Misc
 - Uranium to cyanite actually fixed now

# 1.16.3-0.1.6
bug fixes

## Reactor
 - GUIs added for ports, only allow toggle
 - OutputMultiplier fixed for active reactors
 - Control rod levels apply after reload

## Turbine
 - GUIs added for coolant port, only allows toggle
 - Default turbine max flow rate changed to 5,000mB/t, and tank size to 10,000mB
 - Internal battery size adjusted by 2.5x to match above change

## Misc
 - fixed cyanite reprocessor voiding items
 - corrected the uranium to cyanite recipe

# 1.16.3-0.1.5
more tagging, fix for ATM6 dust smelting

## Misc
 - add ingots to forge:ingots
 - add dusts to forge:dusts
 - add yellorium_dust to forge:dusts/uranium_dust

# 1.16.3-0.1.4
tagging

## Reactor
 - Reactor accepts any forge:ingots/uranium
 - Glass reports errors correctly
 - Reactor will save generated power through reload
 - Added more moderators

## Turbine
 - Inductor drag config option fixed
 - Computerport will properly set vent all
 - Added inductors

## Misc
 - Cyanite reprocessor works with pipes properly
 - Yellorite is tagged as forge:ores/uranium, and forge:ores
 - AE2 grindstone recipes added
 - zh_cn translation added (thanks qsefthuopq)

# 1.16.3-0.1.3
Mekanism compatibility

## Reactor
 - Reactor can now produce Mekanism gas steam, just connect a Mekanism gas pipe to the coolant port

## Turbine
 - Turbine can now accept Mekanism gas steam, just connect a Mekanism gas pipe to the coolant port.
 - Computer Craft API bug fixed. Steam and water were being reported as each other.

## Misc
 - Mekanism Enrichment Chamber and Crusher compatibility added. Pretty much the same as the Pulverizer.
 - Computer ports have names in the lang file now.

# 1.16.3-0.1.2
wrench compat

## Reactor
 - Fixed wrench compat with mekanism
 - Control rods save their insertion

## Turbine
 - Fixed wrench compat with mekanism

# 1.16.3-0.1.1
computer ports and rotor rotation

## Reactor
 - Computer port implemented
## Turbine
 - Computer port implemented
 - Rotor will now rotate to the correct axis
 
## Misc
 - Added config option to disable oregen

# 1.16.3-0.1.0
fist 1.16 release, minor changes

## Reactor
 - Control rod has a GUI now
## Turbine
 - Flow rate change buttons work properly (bug discovered because of control rod)
## Misc
 - Yellorium bucket named fixed
 - Phosphophyllite separated into different jar, required for BiR (see Phosphophyllite changelog for details)

# 1.15.2-0.0.1
bug fix

## Reactor
 - blocks registered with tags not added to registry correct (thermal blocks)

# 1.15.2-0.0.0
initial version, changes from BR 0.4.3A

## Reactor
 - Access port must be pushed into, and does not have a gui nor internal buffer
 - Terminal GUI works a bit different, but not majorly, also called terminal now, not controller
 - Control rod does not have a GUI (yet)
 - Computer port does not exist (yet)

## Turbine
 - Terminal GUI works a bit different, but not majorly, also called terminal now, not controller
 - Turbine requires two bearings, one on each end
 - Computer port does not exist (yet)
 - You can't over speed turbines anymore, there is a soft limit to the speed, so long as you were under 2000 rpm it's the same
     - Small sub-note there, i did modify the efficiency function in the 450-500 RPM range to have a better transition, shouldn't really matter though
 - You now need to rotor bearings, one at each end, orientation of rotor doesn't matter
 
## Misc
 - There is a pile of stuff in the config now, sooo many internal values that were magic before
 

 
#### Beginning of time?
