# Machine lighting

Implemented locally on 2026-10-27 for Minecraft 1.21.1 / NeoForge 21.1.264, version 0.1.3.

| Source | Block light | Condition |
| --- | --- | --- |
| Reactor / turbine terminal | 7 | Assembled and ACTIVE |
| Battery terminal | 7 | Assembled and ACTIVE; IDLE is dark |
| Heat-exchanger terminal | 7 | Assembled; this machine has no on/off switch |
| Reactor fuel rod | 7 | Visible fuel level greater than zero, including cold fuel; empty and waste-only rods are dark |
| Industrial Processor | 6 | Existing ENABLED processing animation is active |
| Exchanger condenser channel | 6 | Its visible path segment contains valid hot input fluid; excludes steam, cold return and empty segments |

This uses vanilla block lighting and needs no graphics mod. Shader bloom and colored illumination depend on the shader. Fuel quantities, fluid stocks, heat, production, controls, texture colors/alpha and geometry remain unchanged.

`multiblocks/MachineLighting` centralizes the state-based light levels. The four multiblock block bases and Industrial Processor use this function through the normal light-level property. The condenser channel adds a `lit` state, updated only when its hot-phase occupancy changes. `HeatExchangerMultiblockController` uses the same quantized snapshot and `FluidPath` span as its renderer and clears channel light on disassembly. Fluid visual/light checks retain the existing ten-tick cadence.

`core/util/Util` now invalidates world lighting when a batched state update changes emission. This covers terminal assembly/disassembly and fuel-level changes without relighting every intermediate fuel pixel. All state changes use the existing client synchronization paths; no new network protocol or configuration is introduced. The additional channel state defaults to false and is recomputed when the structure is assembled.

## Validation

- Full build and all 56 JUnit tests passed.
- All 83 required GameTests passed in the base environment, including four new lighting tests.
- New checks cover active/inactive/unassembled terminals, visible fuel versus empty/waste-only rods, real Industrial Processor operation/pause, hot/cold path segmentation, draining, steam exclusion and exchanger disassembly.
- A world-light test verifies adding an emitter through a position-map batch and removing it through a long-key batch. It uses an opaque enclosure to exclude neighboring GameTest result-marker lights.
- The updated client reached the main menu with the existing `run` modpack and began loading the test world. No new crash report or model-loading error for this mod was observed. On 2026-10-27, the author confirmed the in-game visual check was correct ("conferi e ta tudo certo"). This closes the pending visual check for the author's environment; it is not a claim of compatibility with every shader.
- Logs: `run/machine-light-validation.log` and `run/machine-light-client.log`.

No version change, commit or publication.
