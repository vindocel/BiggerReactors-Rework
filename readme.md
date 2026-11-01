# BiggerReactors-Rework

BiggerReactors-Rework is an independent fork and rework of BiggerReactors for **Minecraft 26.1.2 on NeoForge**.
It runs with only Minecraft, NeoForge and this mod installed.

- Mod id and namespace: `biggerreactorsrework`
- Soft block lighting for operating terminals, reactor fuel, hot exchanger fluids and the running Industrial Processor.
- Reactor, heat-exchanger and RPM-dependent turbine sounds, with individual volume/mute controls in each multiblock's Settings window.
- Repository: https://github.com/vindocel/BiggerReactors-Rework
- Meant for new worlds. Worlds and machines from the original BiggerReactors are not migrated.
- Optional JEI and Roughly Enough Items integration: Industrial Processor recipes, moderator and coolant properties, turbine coils and heat-exchanger conversions. Machine shortcuts open the installed viewer. EMI and OpenComputers remain inactive on this line until compatible builds are available.
- Optional CC:Tweaked integration: the reactor, turbine and heat-exchanger computer ports expose live telemetry and controls through native peripherals, including wired modems. OpenComputers support is retained separately for its compatible builds.
- Requires Java 25 and NeoForge 26.1.2.126 or newer for Minecraft 26.1.2. The other lines live on `mc/1.21.1` and `mc/26.3`.
- Optional official KubeJS 26.1.2 integration for configuration and material events. Native configuration files and the in-game editor work without scripting mods.
- `runClient` uses `run/`, including the user's 26.1.2 mods. Automated tests and verification worlds use isolated subfolders in `run/mc-26.1.2/`.

## Origin and credits

This project is a fork. It is not affiliated with, or endorsed by, the authors of the original mods.

- **RogueLogix**, BiggerReactors: https://github.com/BiggerSeries/BiggerReactors
  (profile: https://www.curseforge.com/members/roguelogix), with contributions from gizmocodes and Ridanisaurus.
- **ErogenousBeef**, the original Big Reactors: https://github.com/erogenousbeef-zz/BigReactors

Report problems with this fork at https://github.com/vindocel/BiggerReactors-Rework/issues, not to the original projects.

## License

GNU Lesser General Public License v2.1, see [LICENSE.txt](LICENSE.txt).
