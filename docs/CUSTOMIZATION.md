# Customization Guide

Configuration files and the native Mods menu editor work without another mod. All targets share the same gameplay settings, tabbed draft editor and server authority rules. Datapack and optional scripting APIs differ; follow the guide for your installed Minecraft version.

| Minecraft | NeoForge baseline | Java | Datapack format | Guide |
| --- | --- | --- | --- | --- |
| 1.21.1 | 21.1.264 | 21 | 48 | [1.21.1](customization/1.21.1.md) |
| 26.1.2 | 26.1.2.126 | 25 | 101 | [26.1.2](customization/26.1.2.md) |
| 26.3 | 26.3.0.99 | 25 | 121 | [26.3](customization/26.3.md) |

Open **Mods → BiggerReactors-Rework → Config** for Appearance, Reactor, Turbine, and Machines and world. Changes remain drafts until Done; Escape cancels. Server rules can be saved only in a loaded local world, on its server thread. Main-menu and remote-server rules are read-only. KubeJS-owned fields remain locked, and script overrides never overwrite the file-backed values.

Use native formats from the selected guide for recipes, loot, world generation and scripts. A 26.3 datapack is not interchangeable with a 1.21.1 datapack. No additional mod is required by the base project.
