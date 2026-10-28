# Native Roughly Enough Items integration

Implemented locally on 2026-10-27 for Minecraft 1.21.1 / NeoForge 21.1.264, version 0.1.3.

## Behavior

- Five categories share `RecipeViewerData` with JEI and EMI: Industrial Processor, solid reactor moderators, reactor coolants, turbine coils and heat exchange.
- Industrial recipes preserve the actual ingredient alternatives/counts, water requirements, duration, energy and exclusive item/fluid outputs. Decorated animated gauges use `GaugeBar`.
- Property bars, rankings, percentages and explanatory tooltips use the same synchronized registries and calculations as the existing viewers. Optional coolant profiles remain conditional on the installed fluids.
- Heated fluids retain their component patches, including coolant origin. Their native REI ingredient entries remain searchable and interactive.
- Workstations match the existing integrations: processor, reactor/turbine terminals, heat-exchanger terminal, both channels and fluid port.
- Existing screen recipe shortcuts route to EMI, then REI, then JEI. JEI categories step aside for native EMI/REI integrations. The existing `EnableJEIIntegration` setting controls recipe population for all three.
- `ModScreen.extraAreas()` supplies REI exclusion zones for tabs and movable popups.

## Architecture and dependencies

`client/deps/rei/BiggerReactorsReiPlugin` is discovered through REI's client-only annotation. `ReiBridge` is reached only when REI is installed. Viewer routing is in `RecipeViewers`; the JEI plugin checks for native REI support before registering categories.

REI 16.0.799 and Architectury 13.0.11 are compile-only dependencies. Their code is not included in the distributed JAR, and neither mod is required for base gameplay. No gameplay, balance, saved identifiers or version changes were made.

API reference: [REI 16.x / Minecraft 1.21 source](https://github.com/shedaniel/RoughlyEnoughItems/tree/16.x-1.21). API signatures were checked against the installed 16.0.799 NeoForge JAR.

## Validation

- Full build and all 56 JUnit tests passed, including after the final fluid-icon renderer change.
- All 79 required GameTests passed in the base dedicated test environment without REI or its libraries installed.
- JAR inspection confirmed the native plugin classes and license, with no bundled REI/Architectury classes or GameTest classes.
- The client launched with the existing `run` environment and REI 16.0.799. REI discovered and loaded the plugin.
- Interactive checks opened the Industrial Processor category from its workstation and showed both recipes with decorated tanks. Reactor terminal lookup showed solid moderator and coolant categories with property bars and recovery percentages.
- The default REI fluid icons appeared dark in the shader-enabled environment. They were replaced with a component-aware NeoForge fluid renderer; this correction compiled successfully.
- During the initial task, the user stopped Computer Use with physical Escape before restarting for visual verification of that correction. The follow-up validation below supersedes the fluid-icon and exchanger-page limitations. Machine-screen clicks and popup exclusion behavior remain outside the interactive checks performed.
- The modpack log contains an unrelated REI tooltip search exception originating in Malum's `CurioProspectorBelt` through Curios, plus third-party shader/model warnings. No exception from the new plugin was observed and no new crash report was produced during inspection.

Logs are local in `run/rei-*.log`; no commit or publication was performed.

## Follow-up: login loading and slot alignment (2026-10-27)

The extra Mojang loading screen came from `CoolantSync` calling `BiggerReactorsClient.refreshCoolantSprites` after login. The initial atlas prepared Cryo Fuel and locally known generic profiles, but the default Liquid Obsidian profile was not yet available. Its missing derived sprite triggered `Minecraft.reloadResourcePacks()`, reloading the entire modpack. The previous client log contains resource reloads at 20:56:51 and 20:57:15.

`HeatedFluidSprites` now includes Liquid Obsidian during the initial atlas load, alongside optional Cryo Fuel. Default profiles arriving from the server no longer require another reload. Custom server profiles introducing a genuinely new generic sprite retain their existing reload support and appearance; this change does not disable resource reloads globally.

The REI `Point` slot constructor subtracts one pixel from the supplied position to produce its frame bounds. Passing the shared frame origin therefore rendered both items and fluids one pixel above and left of their intended positions. The shared slot helper now passes an explicit 18×18 `Rectangle`; REI's inner bounds supply the correct one-pixel inset. This fixes industrial item inputs/outputs, moderators, coils and conversion icons together. The full-height industrial tanks already used explicit bounds and remain unchanged.

Validation after both fixes:

- Full build, 56 JUnit tests and all 79 required base-environment GameTests passed.
- The updated `runClient` entered the existing `run` world. Its log contains only the initial resource reload at 21:08:39, with no second resource reload after joining.
- The heat-exchanger page was visually inspected in the running client: water/steam, sodium and heated Cryo icons have their textures and sit inside the slot frames with the intended inset.
- Other categories use the same corrected slot helper; no separate post-fix interactive check of every category or popup was performed.
- Current logs: `run/rei-fixes-build.log` and `run/rei-fixes-client.log`. Version remains 0.1.3; no commit or publication.

## Follow-up: REI light/dark theme (2026-10-27)

REI's recipe backgrounds already followed its theme, but the shared machine artwork detected dark mode only on `ModScreen`. On a REI recipe screen, that meant light slot/gauge chrome and dark gray property labels on a dark background.

The native REI plugin now reads `ConfigObject.getInstance().isUsingDarkTheme()` on every drawing callback. `GuiTheme.withTheme` applies that value in a temporary scope and restores the previous context in `finally`, including nested scopes. Both the shared category painter and the separate industrial tank renderers use that scope.

All five categories now use the existing machine theme for slot frames, energy/fluid gauge chrome, progress arrows, property labels, values, conversion arrows and neutral property-bar frames/tracks. Item/fluid renderers and informative colors retain their colors. Theme changes are read live, without saving any machine theme preference or introducing REI references into the shared theme code. Light mode continues using the existing light artwork.

Validation: full build and 56 JUnit tests passed; all 79 required GameTests passed without REI installed in the base environment. The updated client launched in `run` and entered the test world. The user stopped Computer Use with physical Escape before the category/theme visual inspection; dark/light appearance and live switching still require interactive confirmation. Logs: `run/rei-theme-build.log`, `run/rei-theme-gametest.log`, `run/rei-theme-client.log`. No version change, commit or publication.
