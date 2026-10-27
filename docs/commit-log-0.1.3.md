Release 0.1.3: rework industrial coolant production and harden machine systems

Consolidated technical commit description for local changes after the committed
0.1.2 rework. Minecraft 1.21.1, NeoForge 21.1.264 and Java 21 remain unchanged.
No commit, tag, push or publication has been performed.

Cooling and output backpressure
- Monitor the minimum retained output over 20 ticks; throttle persistent output
  occupancy linearly and recover by up to 0.05 per tick after extraction improves.
  Promptly extracted production batches do not trigger a penalty.
- Apply reactor throttling to hot-coolant/steam conversion and associated fuel
  burn. Full output immediately blocks conversion and fuel consumption.
- Use retained steam to throttle both exchanger conversions. A full cold-coolant
  output still physically blocks further primary conversion without an added
  gradual penalty on that stock.
- Respect output limits during thermal protection and pending asynchronous
  radiation settlement; persist the monitoring state across reloads.
- Preserve solid-mode generation, turbine flow/rotor/discard behavior and the
  existing thermal dependence on input availability and tank fill.
- Expose current/potential production, retained output, permitted operation and
  localized bottleneck explanations; update guide operating instructions.

Contained-fluid rendering and coolant identity
- Build exterior fluid surfaces; omit shared phase, adjacent-segment and partial
  bend caps rather than rendering overlapping internal faces.
- Correct normals, winding and local UVs; honor native tint alpha and emissive
  light. Use vanilla cutout for opaque fluids and culled translucent rendering
  for translucent fluids across manifolds, exchanger channels and guide previews.
- Preserve coolant profile/origin together with simulation volumes before reactor
  disassembly/revalidation, fixing identified coolant becoming Air after repair.
- Show orphaned stock as unidentified coolant; require an explicit manual dump
  rather than guessing the old fluid or silently discarding/transmuting it.
- Generate heated sprites only for eligible generic profiles; repair the Cryo
  Fuel texture fallback and request missing sprite reloads. No Iris dependency.

Multiblock persistence and turbine reconstruction
- Resolve queued controller aliases before emptiness checks, inventory guards
  and registry removal, fixing turbine bearings split across stale controllers.
- Select merge destinations deterministically by stock, structure size and
  creation order. Preserve stocked controllers and their inventories.
- Persist machine origin on parts to distinguish fragments from independent
  machines. Block merging independently stocked machines, including pending
  NBT inventories and quarantined fluids.
- Invalidate removed save delegates and elect an eligible surviving part.
- Identify turbine output as gross generation without changing rotor simulation,
  energy buffer limits or condensate-discard options.

Battery terminal and equipment charging
- Put the charging slot at the top (+), transferring battery energy to the item;
  put the discharge slot below (-), transferring item energy to the battery.
  Preserve persisted logical slot IDs and contents.
- Charge FE-capable equipment in the displayed armor slots and offhand only
  while a valid terminal menu is open on the server.
- Share the output budget with item slots, energy ports and other viewers;
  debit accepted energy only and synchronize changed item components.
- Deduplicate charging within a server tick. Closing the menu, leaving range
  or invalidating the structure stops menu charging. Ordinary inventory items
  remain outside automatic equipment charging.

Networking, Unicode and validation
- Write new ROBN strings using an explicit length-prefixed UTF-8 type, preserving
  accents, arrows, dimension separators, non-Latin text and embedded NULs.
- Preserve legacy string decoding; reject malformed UTF-8 and check lengths
  before allocation. Correct corrupted dimension/conversion source literals.
- Advance the final internal channel protocol to 4 for UTF-8 and industrial
  recipe/tank synchronization, superseding protocols 2 and 3. Matching client
  and server protocol versions are required.
- Validate binary lengths, nesting, truncation, trailing bytes and compound keys
  before allocating collections. Bind GUI requests/snapshots to menu ID and pos.
- Reject malformed control-rod commands and nonfinite values; correct numeric
  configuration ranges, negative values and open-endpoint correction.

Recipes, registries and client state
- Select cyanite reprocessing recipes using the stored fluid and automatically
  pull the selected recipe fluid instead of assuming water. Exclude machine
  recipes from the vanilla crafting book.
- Support recovery in transition data maps; require origin metadata for generic
  heated coolant lookup.
- Distinguish authoritative empty coolant snapshots from missing synchronization
  and clear client coolant state on disconnect.

Resources, documentation and packaging
- Remove five verified unused textures: help, left/right arrows and copper
  fuel-rod end/side textures. Preserve reactor2 sources and used assets.
- Remove 63 duplicate render_type entries and one duplicate Chinese locale key.
- Align locale keys; preserve valid Chinese translations and use English for
  untranslated entries, without claiming a fully reviewed Chinese translation.
- Update changelog, guide, technical reports and canonical external project rules.
- Consolidate this commit log and add an English Markdown store description
  with visual sections, screenshot placeholders, feature comparisons and linked
  attribution. Remove the superseded DOCX/HTML presentation artifacts.
- Exclude GameTest classes and fixtures from the distribution JAR; ignore local
  cooling-study outputs. Synthetic FE capabilities are enabled in GameTests only.

Industrial processing and identified heated obsidian
- Reuse the cyanite_reprocessor block/item/block-entity IDs and crafting recipe;
  display Industrial Processor / Processador Industrial without replacing blocks.
- Extend native recipes with mutually exclusive item/fluid results and an optional
  total fluid cost; existing item recipe JSON and per-tick costs remain supported.
- Add one obsidian + 100 mB water -> 100 mB Liquid Obsidian, using the configured
  reprocessor duration and energy rate (default 200 ticks / 200 FE).
- Distribute batch input consumption with cumulative integer differences, preserve
  paid fluid-recipe progress while paused and bind progress to recipe identity.
- Add independent product tank, directional fluid input/output/combined routes and
  automatic fluid ejection while retaining existing side ordinals and NBT fields.
- Move water beside FE; keep item slots and place liquid output on the right.
  Synchronize product tank and operating condition; update JEI, EMI and guide.
- Remove the vanilla lava pairing; use origin-tagged heated_coolant for obsidian.
  Reject vanilla lava and missing/incorrect heated identities in that circuit.
- Quarantine saved obsidian/lava pairs without automatic product substitution;
  stop affected reactors/exchangers, report invalid fluid and retain explicit dump
  recovery. Preserve existing Industrial Processor stocks/progress independently.
- Calibrate only obsidian: latentHeat 36, boilingPoint 773.15 K, conductivity 2.2/1.8,
  recovery 0.71765 and turbineMultiplier 0. Measure the intended gain as
  heat/steam per equal hot volume, rather than steam per nuclear fuel.
  Native equal-batch channels produced 9000/7500 mB steam for 1000 mB of
  hot obsidian/Cryo, respectively, with no ambient losses.
- Keep sodium coefficients unchanged; sodium remains above both measured coolants.
- Add exact cost, legacy processor save, blocked-output independence, input-switch,
  no-energy, lava rejection and legacy thermal profile quarantine regressions.

Thermal conservation and industrial recipe lookup
- Bound fractional phase change when the available batch/output space is
  exhausted, preventing an extra heat fraction beyond the fluid budget.
- Add exact 1000 mB batch and native-channel heat-conservation regressions:
  9000 mB steam for hot obsidian, 7500 mB for hot Cryo, 1000 mB cold returned.
- Update JEI/EMI category layout: water next to FE, item result in its slot,
  fluid result in the output tank; register water and fluid products as
  searchable ingredients with recipe-specific quantities and components.

Industrial viewer gauge rendering and tooltip correction
- Render JEI fluid ingredients with the shared animated GaugeBar renderer,
  preserving recipe lookup, names, quantities and component-aware ingredient IDs.
- Override EMI tank drawing with the same machine gauge, retaining native tank
  bounds, tooltip and recipe-context behavior.
- Draw only empty background tanks before the ingredient renderers; do not layer
  the raw fluid renderer over markings, frame and animated highlights.
- Disable the status-label tooltip with null instead of an empty Component,
  removing the blank dark tooltip on Processing/Output full.

- Add a dedicated processor title row and move gauges, symbols, menu slots and
  inventory together to prevent the water icon from overlapping the title.

Store description and public documentation
- Update the English Markdown store description for the new Industrial Processor
  name, retained cyanite recycling and manufactured Liquid Obsidian circuit.
- Replace the obsolete lava loop with Heated Liquid Obsidian and explain that
  ordinary lava cannot supply the circuit.
- Group reactor, processor, heat exchanger, turbine and battery information with
  their own construction and interface images; preserve supplied image URLs.
- Add distinct placeholders for the Industrial Processor block and its new UI.
- Remove coolant-specific efficiency rankings and percentage comparisons from
  store copy; retain the general liquid-cooling benefit over solid moderation.
- Preserve linked credits, optional integration requirements and the distinction
  between inherited multiblocks and additions in this fork.
- Record the approved behavior and editorial rules in the canonical external
  AGENTS.md so subsequent changes follow the current requirements.

Version and artifact verification (2026-10-27)
- Confirm mod_version=0.1.3 in gradle.properties and version=0.1.3 in the built
  JAR's META-INF/neoforge.mods.toml and Implementation-Version manifest entry.
- Verified artifact: build/libs/biggerreactorsrework-1.21.1-0.1.3.jar.
- Confirm the industrial fluid recipe and obsidian-to-heated-obsidian transition
  are packaged; the obsolete lava-to-obsidian transition is absent.
- Confirm no GameTest classes are packaged and no main/test source files are
  newer than the verified JAR. Documentation edits do not require rebuilding it.

Validation evidence and limits
- Existing automated reports record 56 JUnit tests with zero failures, errors or
  skipped tests. The latest full build completed after the viewer gauge fix.
- Saved core and optional-integration logs each record all 79 required GameTests
  passing. These runs include industrial processing and hot-volume yield tests;
  they precede the final client-only gauge/tooltip changes.
- Coverage includes Unicode maps/vectors, malformed and legacy network payloads,
  battery transfer budgets and equipment charging, saved turbine load orders,
  controller aliases, inventory retention, reactor repair/refill, fluid geometry,
  restricted output extraction, full blocking and gradual recovery.
- Exact native-channel tests verify 1000 mB hot coolant converted to 1000 mB cold
  coolant, yielding 9000 mB steam for obsidian and 7500 mB for Cryo Fuel under
  controlled conditions without ambient losses and after subtracting initial heat.
- Current verification inspected artifact metadata, packaged resources and saved
  test reports. No new gameplay tests were run for this documentation-only task.
- Latest JEI/EMI gauges, processor title positioning, battery terminal visuals and
  Complementary Reimagined rendering still need an interactive check after a
  client restart. No universal shader or multiplayer-latency guarantee is made.
- Water, Cryo Fuel, sodium and turbine coefficients are unchanged by the obsidian
  correction. Potential values assume the conditions listed in each machine.
- Legacy lava circuits and obsolete identified hot-coolant profiles require
  draining invalid stock and refilling; no automatic conversion is performed.
- The author's worlds were not modified during this verification. No commit,
  staging, tag, push or publication was performed.
