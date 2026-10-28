# Add native REI support, fix login reloads and add machine lighting

- Add optional native Roughly Enough Items integration for Industrial Processor recipes, reactor moderators and coolants, turbine coils, and heat-exchanger conversions.
- Reuse shared property rankings, percentages, tooltips and animated fluid gauges; preserve searchable ingredients and heated-fluid origin components.
- Register machine workstations, route recipe shortcuts to the installed viewer, and exclude machine tabs and popups from the REI overlay. Retain EMI priority and prevent duplicate JEI categories.
- Center item and fluid icons inside their slot frames across all REI categories.
- Follow REI's active light/dark theme for slots, gauge frames, progress arrows, neutral property bars and readable text without changing machine appearance preferences.
- Preload heated Liquid Obsidian sprites during initial resource loading, preventing the redundant full resource reload when default coolant profiles arrive on world login.
- Add level 7 block light to operating multiblock terminals and visible reactor fuel. Add level 6 light to the running Industrial Processor and visible hot-fluid exchanger segments, excluding steam and cold returns.
- Refresh world lighting on emission changes, including batched assembly/fuel updates, coolant conversion, draining and exchanger disassembly.
- Add four lighting GameTests and update integration reports, README, changelog and public description.

## Validation

- Full build and all 56 JUnit tests passed.
- All 83 required GameTests passed in the base environment without REI installed.
- Client checks confirmed REI registration, industrial recipes, reactor property pages and exchanger fluid rendering. World login completed with one initial resource reload.
- The author confirmed the machine-lighting visual check was correct. REI light/dark appearance and live theme switching still require a complete visual check.
- Distribution JAR inspection confirmed the lighting class, excluded GameTest classes and no bundled REI/Architectury classes.

Minecraft 1.21.1 / NeoForge 21.1.264; version remains 0.1.3. No commit, tag or publication was performed.
