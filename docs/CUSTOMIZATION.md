# 🧩 Customization Guide

How to change BiggerReactors-Rework with its configuration files, datapacks or KubeJS scripts: what each option does, where its limits are, what cannot be changed without editing the code, and how to check that a change took effect.

This guide is for modpack creators, server administrators and modders. You do not need to know the mod's code, but you should know how to edit a text file and put a folder inside a world save.

| | |
|---|---|
| Minecraft | 26.3 |
| NeoForge | 26.3.0.99 or newer |
| Java | 25 |
| Mod ID | `biggerreactorsrework` |
| Datapack format | `121` |

> [!NOTE]
> If something is not listed here, assume it is **not** configurable.

---

## 📚 Contents

1. [Ways to change the mod](#1-ways-to-change-the-mod)
2. [Configuration files and screen](#2-configuration-files-and-screen)
3. [Server options](#3-server-options)
4. [Client options](#4-client-options)
5. [Datapack basics](#5-datapack-basics)
6. [Reactor fuels](#6-reactor-fuels)
7. [Solid moderators](#7-solid-moderators)
8. [Turbine coils](#8-turbine-coils)
9. [Coolants and fluid transitions](#9-coolants-and-fluid-transitions)
10. [Industrial Processor recipes](#10-industrial-processor-recipes)
11. [Uranium generation](#11-uranium-generation)
12. [KubeJS scripts](#12-kubejs-scripts)
13. [What cannot be changed](#13-what-cannot-be-changed)
14. [Applying and checking changes](#14-applying-and-checking-changes)
15. [Troubleshooting](#15-troubleshooting)

---

## 1. Ways to change the mod

| Mechanism | What it changes | How it applies |
|---|---|---|
| **Configuration screen** | The same values as the files below, from **Mods → BiggerReactors-Rework → Config** | Restart the game for machine and world-generation rules |
| **Configuration files** | Machine limits, simulation constants, battery values, uranium generation, fuel yields | Edit with the game closed, then start it |
| **Datapack** | Moderators, coils, coolants, fuels by data map, recipes, tags | `/reload`; machines already assembled may need reassembly |
| **KubeJS** (optional) | Server options in memory, plus materials and fuels | World or server restart for options; `/reload` for materials |
| **Resource pack** | Textures, models, sounds and translations only | `F3 + T` |

> [!WARNING]
> Never edit files inside the mod's `.jar`. Your changes are lost on every update.

## 2. Configuration files and screen

The mod writes its own files the first time it starts:

| File | Scope |
|---|---|
| `<instance>/config/biggerreactorsrework-server.toml` | Gameplay rules. The server's copy counts, and connected players receive its values |
| `<instance>/config/biggerreactorsrework-client.toml` | Display preferences of one player |

There is no per-world copy of these files: they apply to the whole instance or server.

**The screen.** It needs no other mod. Search by option name, open an option, and read its comment, default and valid range. Invalid text disables saving. Gameplay rules are read-only while a world is open, including singleplayer: close the world to edit them. A connected client never changes its server's rules.

**Advanced options.** The file lists only the common options until `EnableAdvancedConfig = true` is set at its top; the others appear after the next load. They are the options marked *advanced* below.

**When a change applies.** Restart the game or server after changing machine or world-generation rules. Many values are read when a machine assembles, so restart first and then reassemble if an existing machine does not follow.

**Invalid values.** When the file loads, a value of the wrong type or out of range is corrected and the file is rewritten. Check your value after starting the game. A number too large for its option falls back to the default.

**Going back.** Delete a line or the whole file, and the mod writes the default again.

## 3. Server options

Ranges use interval notation: `[1,)` means 1 or more, `(0,)` means above 0.

### 3.1 World generation

| Key | Default | Effect |
|---|---|---|
| `Worldgen.GenerateUranium` | `true` | `false` stops uranium ore in **newly generated** terrain. Existing ore stays, and the ore and items stay registered |

### 3.2 Reactor

| Key | Default | Range | Effect |
|---|---|---|---|
| `Reactor.MaxLength` / `MaxWidth` | `128` | `[3,)` | Largest size in blocks |
| `Reactor.MaxHeight` | `192` | `[3,)` | Largest height in blocks |
| `Reactor.FuelUsageMultiplier` | `1` | `(0,)` | Fuel consumption. Higher burns fuel faster |
| `Reactor.OutputMultiplier` | `1` | `(0,)` | Reactor output |
| `Reactor.MaxFuelTemperature` | `3273.15` | `(1273.15,)` | Fuel temperature ceiling, in kelvin, for automatic liquid-cooling control |
| `Reactor.CustomFuelEntries` | empty | see [6](#6-reactor-fuels) | Fuel yield per item or tag |

**Advanced:**

| Key | Default | Range |
|---|---|---|
| `Reactor.PassiveOutputMultiplier` | `0.5` | `(0,)` |
| `Reactor.ActiveOutputMultiplier` | `1` | `(0,)` |
| `Reactor.FuelMBPerIngot` | `1000` | `(0,)` |
| `Reactor.PerFuelRodCapacity` | `4000` | `[1,)` |
| `Reactor.CoolantTankAmountPerFuelRod` | `10000` | `(0,)` |
| `Reactor.PassiveBatteryPerExternalBlock` | `100000` | `(0,)` |
| `Reactor.PassiveCoolingTransferEfficiency` | `0.2` | `(0,)` |
| `Reactor.SimulationRays` | `64` (powers of two recommended) | `[16,)` |
| `Reactor.IrradiationDistance` | `4` | `(0,)` |
| `Reactor.ModeSpecific.ControlRodBatchSize` | `32` | `(0,)` |
| `Reactor.useFullPassSimulation` | `false` | true / false |
| `Reactor.allowOffThreadSimulation`, `allowMultiThreadSimulation` | `true` | true / false |

`FuelMBPerIngot` is the yield of a uranium or blutonium ingot; their blocks give nine times that value.

The remaining advanced reactor keys are constants of the simulation's physics. Change them only if you intend to rebalance it and can test the result:

| Keys | Default |
|---|---|
| `FuelFertilityMinimumDecay`, `FuelFertilityDecayDenominator`, `FuelFertilityDecayDenominatorInactiveMultiplier` | `0.1`, `20`, `200` |
| `CasingHeatTransferRFMKT`, `FuelToStackRFKTMultiplier`, `StackToCoolantRFMKT`, `StackToAmbientRFMKT` | `0.6`, `1`, `0.6`, `0.001` |
| `CaseFEPerUnitVolumeKelvin`, `RodFEPerUnitVolumeKelvin` | `10`, `10` |
| `FuelReactivity`, `FissionEventsPerFuelUnit`, `FEPerRadiationUnit`, `FuelPerRadiationUnit` | `1.05`, `0.1`, `10`, `0.0007` |
| `FuelHardnessDivisor`, `FuelAbsorptionCoefficient`, `FuelModerationFactor`, `fuelRadScalingMultiplier` | `1`, `0.5`, `1.5`, `1` |
| `RadIntensityScalingMultiplier`, `RadIntensityScalingRateExponentMultiplier`, `RadIntensityScalingShiftMultiplier` | `0.95`, `1.2`, `1` |
| `RadPenaltyShiftMultiplier`, `RadPenaltyRateMultiplier` | `15`, `2.5` |
| `FuelAbsorptionScalingMultiplier`, `FuelAbsorptionScalingShiftMultiplier`, `FuelAbsorptionScalingRateExponentMultiplier` | `0.95`, `1`, `2.2` |

### 3.3 Turbine

| Key | Default | Range | Effect |
|---|---|---|---|
| `Turbine.MaxLength` / `MaxWidth` | `32` | `[5,)` | Largest size in blocks |
| `Turbine.MaxHeight` | `192` | `[4,)` | Largest height in blocks |

**Advanced** (all `(0,)` unless noted):

| Key | Default |
|---|---|
| `Turbine.FlowRatePerBlock` | `5000` |
| `Turbine.TankVolumePerBlock` | `10000` |
| `Turbine.BatterySizePerCoilBlock` | `300000` |
| `Turbine.FluidPerBladeLinerKilometre` | `20` |
| `Turbine.RotorAxialMassPerShaft`, `RotorAxialMassPerBlade` | `100`, `100` |
| `Turbine.FrictionDragMultiplier`, `AerodynamicDragMultiplier`, `CoilDragMultiplier` | `0.0005`, `0.0005`, `10` |
| `Turbine.EffectiveGridFrequency`, `EfficiencyPeaks` (`[1,)`) | `30`, `2` |

### 3.4 Heat exchanger

| Key | Default | Range | Effect |
|---|---|---|---|
| `HeatExchanger.MaxLength` / `MaxWidth` | `64` | `[3,)` | Largest size in blocks |
| `HeatExchanger.MaxHeight` | `96` | `[4,)` | Largest height in blocks |

**Advanced** (all `(0,)`):

| Key | Default |
|---|---|
| `HeatExchanger.ChannelTankVolumePerBlock` | `10000` |
| `HeatExchanger.ChannelToChannelHeatConductivityMultiplier` | `0.5` |
| `HeatExchanger.ChannelFEPerKelvinUnitVolume`, `ChannelFEPerKelvinMetreSquared`, `ChannelInternalSurfaceArea` | `1000`, `50`, `80` |
| `HeatExchanger.AirFEPerKelvinUnitVolume`, `AirFEPerKelvinMetreSquared`, `AmbientFEPerKelvinMetreSquared` | `10`, `0.5`, `0.2` |

### 3.5 Battery

| Key | Default | Range | Effect |
|---|---|---|---|
| `Battery.MaxLength` / `MaxWidth` / `MaxHeight` | `32` | `[3,)` | Largest size. Every cell is a block entity, so very large batteries assemble more slowly |
| `Battery.CellCapacity` | `3200000000` | `(0,)` | FE stored by each cell |
| `Battery.CellTransferRate` | `300000` | `(0,)` | FE/t each cell adds to the input limit, and the same again to the output limit |

Lowering `CellCapacity` never deletes stored energy. A battery above its new limit refuses input and can still output until its charge falls below the limit.

### 3.6 Industrial Processor

These keys keep the `CyaniteReprocessor` name for compatibility with existing files.

| Key | Default | Range | Effect |
|---|---|---|---|
| `CyaniteReprocessor.TransferRate` | `500` | `(0,)` | Largest fluid and energy transfer rate |
| `CyaniteReprocessor.EnergyTankCapacity` | `5000` | `(0,)` | Energy capacity |
| `CyaniteReprocessor.WaterTankCapacity` | `5000` | `(0,)` | Input fluid capacity |
| `CyaniteReprocessor.EnergyConsumptionPerTick` | `1` | `(0,)` | FE/t for recipes that do not set their own |
| `CyaniteReprocessor.WaterConsumptionPerTick` | `1` | `(0,)` | Fluid per tick for recipes that do not set their own |
| `CyaniteReprocessor.TotalWorkTime` | `200` | `(0,)` | Ticks per job for recipes that do not set their own |

## 4. Client options

| Key | Default | Effect |
|---|---|---|
| `AlwaysShowTooltips` | `true` | Always show moderator and coil tooltips |
| `EnableJEIIntegration` | `true` | Enables the material pages in JEI, the recipe viewer supported on this version |
| `Reactor.GUI.HeatDisplayMax`, `HeatExchanger.gui.HeatDisplayMax` | `2000` | Top of the heat gauge |
| `Turbine.GUI.DeltaMB`, `DeltaMBShift`, `DeltaMBCtrl`, `DeltaMBHCtrlShift` | `10`, `100`, `500`, `1000` | Flow-rate step of the turbine buttons with no modifier, Shift, Ctrl and Ctrl + Shift |

## 5. Datapack basics

A datapack is a folder (or `.zip`) with a `pack.mcmeta` file and a `data` folder, placed in `<world>/datapacks/`. This version declares its format with `min_format` and `max_format`:

```json
{
  "pack": {
    "description": "My BiggerReactors-Rework tweaks",
    "min_format": 121,
    "max_format": 121
  }
}
```

```text
My_Pack/
├── pack.mcmeta
└── data/
    └── biggerreactorsrework/
        ├── data_maps/
        │   ├── block/reactor_moderator.json
        │   ├── block/turbine_coil.json
        │   ├── fluid/fluid_transition.json
        │   └── item/reactor_fuel.json
        └── recipe/
```

Materials come from two layers. The mod's defaults are JSON5 files in `ebcr/moderators`, `ebest/coils` and `transitions`. **Data maps are applied on top of them**, so use data maps in your pack: they override a default for a block or fluid without touching the mod's files. JSON files cannot hold comments.

## 6. Reactor fuels

A fuel value is **mB of the existing generic reactor fuel per whole item**. A higher value means more fuel per item. It does not change the power of each mB, the heat, or the waste. Items and tags must already exist; this creates no item or recipe.

**In the server file:**

```toml
[Reactor]
    CustomFuelEntries = "example:enriched_pellet=2500;#c:ingots/thorium=1500;example:disabled_pellet=0"
```

**As a data map**, `data/biggerreactorsrework/data_maps/item/reactor_fuel.json`:

```json
{
  "values": {
    "example:enriched_pellet": { "amount": 2500 },
    "#c:ingots/thorium": { "amount": 1500 }
  }
}
```

A value of `0` disables a match. Amounts are whole numbers from `0` to `4294967298`. The file entry allows at most 1,024 definitions and rejects duplicates. An invalid edited entry is logged and the whole text is ignored.

The first match wins:

1. An exact item in `CustomFuelEntries`.
2. The first matching tag in `CustomFuelEntries`.
3. The `reactor_fuel` data map.
4. The defaults: `c:storage_blocks/uranium` and the blutonium block give 9 × `FuelMBPerIngot`; `c:ingots/uranium` and the blutonium ingot give `FuelMBPerIngot`.

Only whole items enter the reactor: if a tank has room for two items, a third stays in the stack. Fuel already stored keeps its amount when a yield changes. Manual extraction still returns uranium ingots, and waste is still cyanite.

## 7. Solid moderators

Blocks inside the reactor interior that change its behavior in solid (direct FE) mode. Blue Ice, for example, has the same values as a diamond block.

`data/biggerreactorsrework/data_maps/block/reactor_moderator.json`:

```json
{
  "values": {
    "minecraft:sponge": { "absorption": 0.15, "efficiency": 0.9, "moderation": 2.0, "conductivity": 1.0 },
    "#c:storage_blocks/tin": { "absorption": 0.3, "efficiency": 0.7, "moderation": 1.2, "conductivity": 2.0 }
  }
}
```

| Field | Range | Meaning |
|---|---|---|
| `absorption` | `0` to `1` | How much radiation the block absorbs |
| `efficiency` | `0` to `1` | How well absorbed radiation becomes heat |
| `moderation` | `1` or more | How much the block slows radiation |
| `conductivity` | `0` or more | How well the block conducts heat |

| Default example | Absorption | Efficiency | Moderation | Conductivity |
|---|---|---|---|---|
| Diamond block (`c:storage_blocks/diamond`) | `0.55` | `0.85` | `1.5` | `3` |
| Blue Ice (`minecraft:blue_ice`) | `0.55` | `0.85` | `1.5` | `3` |

Changing a diamond value does not change Blue Ice: they are separate entries. The guide's table and the recipe viewers read the loaded values, so they follow your changes.

> [!IMPORTANT]
> Liquids placed in the reactor interior are not moderators. In liquid mode, spare solid blocks count as air. Tune coolants with [section 9](#9-coolants-and-fluid-transitions).

## 8. Turbine coils

`data/biggerreactorsrework/data_maps/block/turbine_coil.json`:

```json
{
  "values": {
    "minecraft:honey_block": { "efficiency": 1.25, "extractionRate": 1.1, "bonus": 1.0 }
  }
}
```

| Field | Range | Meaning |
|---|---|---|
| `efficiency` | Above `0` | Energy conversion efficiency of the coil |
| `extractionRate` | Above `0` | How much energy the coil extracts from the rotor |
| `bonus` | Above `0`, below `2` | Bonus multiplier of the coil |

## 9. Coolants and fluid transitions

A transition pairs a cold fluid with the fluid it becomes when heated.

`data/biggerreactorsrework/data_maps/fluid/fluid_transition.json`, keyed by the cold fluid:

```json
{
  "values": {
    "example:cold_coolant": {
      "gas": "biggerreactorsrework:heated_coolant",
      "latentHeat": 30,
      "boilingPoint": 800,
      "liquidThermalConductivity": 2,
      "gasThermalConductivity": 1.5,
      "turbineMultiplier": 0,
      "recovery": 1
    }
  }
}
```

| Field | Range | Meaning |
|---|---|---|
| `gas` | Fluid ID or `#tag` | The hot product. A missing fluid logs an error and skips the entry |
| `latentHeat` | Above `0` | Heat per mB converted. This is heat, not electrical FE |
| `boilingPoint` | Above `0` | Conversion temperature, in kelvin |
| `liquidThermalConductivity`, `gasThermalConductivity` | `0` or more | Heat transfer of each phase |
| `turbineMultiplier` | `0` or more | Turbine value of the product. `0` means it must go to a heat exchanger instead |
| `recovery` | Above `0`, up to `1`; default `1` | Share of the fluid recovered when it returns to its cold form |

| Default pair | Latent heat | Boiling point | Conductivity (cold / hot) | Turbine | Recovery |
|---|---|---|---|---|---|
| Water → Steam | `4` | `373.15` | `0.6` / `0.025` | `2.5` | `0.6` |
| Liquid Obsidian → heated coolant | `36` | `773.15` | `2.2` / `1.8` | `0` | `0.71765` |
| Cryo Fuel → heated coolant (when installed) | `30` | `773.15` | `2.2` / `1.8` | `0` | `0.59` |
| Sodium → superheated sodium (when installed) | `40` | `1273.15` | `4` / `4` | `0` | `0.95` |

`biggerreactorsrework:heated_coolant` is a generic hot fluid that remembers its origin through fluid components. Plain buckets cannot keep those components, so they refuse to collect it from a port; pipes and containers that preserve components work normally. A tank holding a fluid whose profile changed stops as invalid until it is emptied manually.

## 10. Industrial Processor recipes

Type: `biggerreactorsrework:cyanite_reprocessing`. Files go in `data/<namespace>/recipe/`.

| Field | Required | Meaning | When omitted |
|---|---|---|---|
| `ingredient` | Yes | Item input: `"minecraft:obsidian"`, `"#c:ingots/uranium"` or a list | Recipe rejected |
| `input_count` | No | Items per job, `1` to `64` | `1` |
| `result` | One of the two | Item output, `{ "id": ..., "count": ... }` | |
| `fluid_result` | One of the two | Fluid output, `{ "id": ..., "amount": ... }` | |
| `fluid` | No | Input fluid | `minecraft:water` |
| `ticks` | No | Ticks per job | `CyaniteReprocessor.TotalWorkTime` |
| `energy_per_tick` | No | FE/t | `CyaniteReprocessor.EnergyConsumptionPerTick` |
| `fluid_per_tick` | No | Fluid per tick | `CyaniteReprocessor.WaterConsumptionPerTick` |
| `fluid_per_operation` | No | Fluid per whole job, used instead of the per-tick cost | Per-tick cost |

The default cyanite recipe is `biggerreactorsrework:cyanite_reprocessing`, and Liquid Obsidian is `biggerreactorsrework:liquid_obsidian_processing`. To replace one, use the same path in the `biggerreactorsrework` namespace.

**Example:** `data/my_pack/recipe/netherrack_to_lava.json`

```json
{
  "type": "biggerreactorsrework:cyanite_reprocessing",
  "ingredient": "minecraft:netherrack",
  "input_count": 4,
  "fluid": "minecraft:water",
  "fluid_result": { "id": "minecraft:lava", "amount": 250 },
  "fluid_per_operation": 100,
  "ticks": 100
}
```

The guide and recipe viewers show recipes from the server, so they follow your changes.

## 11. Uranium generation

- To stop it, set `Worldgen.GenerateUranium = false`. Keep in mind that uranium must then come from somewhere else, because reactors need fuel.
- To change vein count or height, replace `data/biggerreactorsrework/worldgen/placed_feature/uranium_ore.json` from a datapack. Keep its `biggerreactorsrework:uranium_generation` placement entry, or the option stops working. The vein itself is `worldgen/feature/uranium_ore.json`; on this version that file has no `config` wrapper.

World generation only affects new chunks, and nothing removes ore that already exists.

## 12. KubeJS scripts

[KubeJS](https://kubejs.com) is optional. Without it, the mod works the same.

| | |
|---|---|
| KubeJS | 26.3-8.1.0-port.1 or newer (NeoForge 26.3). This is a community port; the official 26.3 release is still pending |
| Where | `kubejs/server_scripts/<any name>.js` |
| Errors | `logs/kubejs/server.log` |

```js
BiggerReactorsEvents.config(event => {
  event.set('Worldgen.GenerateUranium', false)
  event.set('Battery.CellCapacity', 6400000000)
  event.set('Battery.CellTransferRate', 600000)
  event.set('Reactor.OutputMultiplier', 1.2)
  event.fuel('example:enriched_pellet', 2500)
  event.fuel('#c:ingots/thorium', 1500)
  console.info(event.getKeys()) // every available key
})

BiggerReactorsEvents.materials(event => {
  event.moderator('example:shielding_block', 0.5, 0.9, 2.0, 1.0)
  event.coil('example:coil_block', 1.25, 1.1, 1.0)
  event.fuel('example:pellet', 1200)
  event.transition('example:cold_coolant',
    'biggerreactorsrework:heated_coolant', 30, 800, 2, 1.5, 0, 1)
})
```

| Call | Arguments, with the ranges of the sections above |
|---|---|
| `config.set` | Key from [section 3](#3-server-options), value |
| `config.fuel` | Item or `#tag`, mB per item. Merges with `CustomFuelEntries` and replaces matching entries |
| `materials.moderator` | Block or `#tag`, absorption, efficiency, moderation, conductivity |
| `materials.coil` | Block or `#tag`, efficiency, extraction rate, bonus |
| `materials.fuel` | Item or `#tag`, mB per item. Ranks with the data map, below `CustomFuelEntries` |
| `materials.transition` | Cold fluid or `#tag`, hot fluid or `#tag`, latent heat, boiling point, cold conductivity, hot conductivity, turbine multiplier, recovery |

**Rules:**

- `config` values live in memory above your file and are never written to it. They are sent to connected players and cleared when the server stops.
- Changing `config` values needs a world or server restart. A `/reload` with changed values reports an error and keeps the live values; with unchanged values it is safe.
- `materials` become data maps above other packs and are regenerated on `/reload`.
- An unknown key or invalid value is an error in the script log.

## 13. What cannot be changed

These are fixed in the code:

- The rotor limit of 2,200 RPM.
- Rotor contact damage: 1 to 10 hearts per second depending on RPM, for players in the blades' swept area.
- Automatic reactor and heat-exchanger slowdown when output stays full.
- Fluids as reactor moderators.
- The heated-coolant identity rule, and its refusal in plain buckets.
- Block and item IDs, the guide's chapters, and the machines' screens.
- Reward-free advancements: their rewards and triggers are not options, though a datapack can replace an advancement file like any other.
- Separate fuel physics. Custom fuels change the yield only.

## 14. Applying and checking changes

| Change | Apply with | How to check |
|---|---|---|
| Server options | Restart | The screen or file shows the value; machine screens and the guide show the result |
| Client options | Restart when unsure | The screen shows the value |
| Moderators, coils | `/reload` | Guide tables, recipe viewers, block tooltips |
| Coolants | `/reload` | Guide tables, recipe viewers, the port and the reactor's status |
| Fuels | `/reload`, or restart for the file | Insert the item through a reactor access port and read the fuel gauge |
| Recipes | `/reload` | The guide's processor cards and recipe viewers |
| KubeJS `config` | World or server restart | `logs/kubejs/server.log`, then the machine screens |
| KubeJS `materials` | `/reload` | As for each material above |

Logs are at `logs/latest.log`. Search for `biggerreactorsrework` and the file's name.

Changes do not rewrite existing worlds. Reassemble a machine (break and replace one casing block) if it keeps old values, and test any change on a copy of your world first.

## 15. Troubleshooting

| Problem | Likely cause | What to check |
|---|---|---|
| A setting has no effect | The game was not restarted; the machine was not reassembled; the server's value applies | Restart, reassemble, check the server's file |
| A value returned to default | Wrong type or too large a number | The file after startup, and its valid range in the comment |
| An option is missing from the file | It is advanced | Set `EnableAdvancedConfig = true` and start again |
| Gameplay rules are greyed out | A world is open | Leave the world |
| A fuel is refused | Entry set to `0`; a higher-priority rule matches; no room for a whole item | The order in [section 6](#6-reactor-fuels) and the log |
| All custom fuels stopped working | One invalid definition made the text invalid | `Ignoring invalid Reactor.CustomFuelEntries` in the log |
| A moderator has no effect | The reactor is in liquid mode, or the block is a fluid | [Section 7](#7-solid-moderators) |
| A coolant stops the reactor | Its profile changed while stored | Empty both tanks with the port's manual clear |
| A plain bucket will not collect heated coolant | Intended: it would lose the coolant's identity | Use pipes or a container that preserves fluid components |
| A bucket does nothing at a port | Port not assembled, wrong direction, wrong fluid, or under 1,000 mB of room | The port's screen |
| Ore still appears | The chunk was generated before the change | Explore new terrain |
| A script value was refused on `/reload` | Options require a restart | Restart the world or server |
| Missing IDs at load | An optional mod's ID without that mod | Remove the entry, or use a load condition |
