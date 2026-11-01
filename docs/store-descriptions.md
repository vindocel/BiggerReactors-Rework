<!-- Shared store description for CurseForge and Modrinth. -->

# ⚛️ BiggerReactors Rework

### Build the reactor. Close the loop. Power your world.

![BiggerReactors Rework — reactor, turbine, battery and heat exchanger](https://cdn.modrinth.com/data/IXXgM5v4/images/160b03e41ca3f7346a31f09d2d5d7e569d212285.png)

Design your own reactor, turn its heat into steam, drive a turbine and store the power in a multiblock battery.

**BiggerReactors-Rework** brings the BiggerReactors foundation to **Minecraft 1.21.1 on NeoForge**, with **rebuilt liquid cooling, redesigned interfaces, original machine sounds and a new battery multiblock**.

> 🧬 An independent fork of **BiggerReactors**, carrying forward the legacy of **Big Reactors**. Original authors and contributors are credited below.

---

## ✨ What makes this rework different

| Feature | What you get in the rework |
| --- | --- |
| 💧 Liquid cooling | Dedicated manifolds beside fuel rods, with cold coolant entering and heated coolant leaving. |
| 🔁 Coolant circuits | Water, manufactured Liquid Obsidian and optional Cryo Fuel and liquid sodium circuits. |
| ⚙️ Industrial Processor | One machine for cyanite recycling and Liquid Obsidian production, with separate item and fluid outputs. |
| 🖥️ Redesigned interfaces | Metallic frames, clearer resource bars and consistent machine controls. |
| 🔊 Original machine sounds | Distinct reactor, heat-exchanger and turbine sounds, with turbine audio following rotor speed and individual volume/mute controls for each multiblock. |
| 💡 Soft machine lighting | Operating terminals, reactor fuel, hot exchanger fluids and the running Industrial Processor emit a modest glow. |
| 📊 Machine statistics | Current and potential rates, flow measurements and explanations of bottlenecks. |
| 🔋 Multiblock battery | Expandable FE storage, item charging and equipped-gear charging. |
| 🔴 Battery redstone sensor | Configurable charge thresholds and full/empty signals for connected machine automation. |
| 📖 In-game guide | Interactive multiblock views, recipes and operating advice. |

**The familiar foundation stays:** configurable reactors, fuel rods, control rods, turbines, coils, the heat exchanger and cyanite fuel recycling. The existing reprocessor becomes the **Industrial Processor**, adding coolant production. The battery is an added multiblock; the cooling circuit and machine interfaces are reworked systems.

---

## ⚛️ Your reactor your design

### 🧱 Start with direct power

Build the casing, arrange your fuel rods, add solid moderators and supply fuel. The reactor generates **FE directly**.

- 🎛️ Control rods let you tune operation.
- 🧩 Your layout and moderator choices affect performance.
- ⚡ A practical starting point before building a full steam plant.

### 💧 Move to liquid cooling

Place **reactor coolant manifolds directly beside the fuel rods**. Feed a supported cold coolant through an inlet and extract its heated form through an outlet.

- 🔗 More contact faces improve heat transfer.
- 🛢️ Coolant stays contained inside the manifolds, with visible fluid inside the machine.
- 🧱 Solid blocks left in spare spaces do not add moderator bonuses in liquid mode.
- ♨️ The reactor produces **steam or heated coolant**; the turbine produces the electricity.

> 💡 **Liquid cooling can generate more FE per unit of fuel than direct generation with solid moderators.** Build a properly supplied plant with a turbine and, when needed, a heat exchanger. Its output depends on the design, temperatures, controls and circulation.

![A 5×5×5 reactor with glowing fuel rods and water-filled coolant manifolds behind reactor glass](https://cdn.modrinth.com/data/IXXgM5v4/images/8e30e0bf7bb90ad26bac1faf399ed42d8b067367.png)

### 🔁 Choose your coolant and build your loop

| Coolant entering the reactor | Heated output | Next step | Availability |
| --- | --- | --- | --- |
| 💧 Water | Steam | Send directly to a turbine. | Included |
| 🟣 Liquid Obsidian | Heated Liquid Obsidian | Cool it in the heat exchanger and return Liquid Obsidian to the reactor. | Made in the Industrial Processor |
| ❄️ Cryo Fuel | Heated Cryo Fuel | Cool it in the heat exchanger and return the Cryo Fuel. | Ad Astra installed |
| 🧪 Liquid sodium | Superheated liquid sodium | Cool it in the heat exchanger and return the sodium. | Mekanism installed |

#### 💧 The simple steam route

**💧 Water → ⚛️ Reactor → ♨️ Steam → 🌀 Turbine → ⚡ FE**

#### 🟣 A reusable coolant circuit

**⚛️ Reactor → ♨️ Heated Liquid Obsidian → 🔥 Heat Exchanger → 🟣 Liquid Obsidian → ⚛️ Reactor**

The exchanger's separate water circuit supplies your turbine:

**💧 Water → 🔥 Heat Exchanger → ♨️ Steam → 🌀 Turbine → ⚡ FE**

> ♨️ Heated coolant carries **heat**. **Steam** is what the turbine uses to generate FE. Optional coolant entries appear only when their source mod is installed.

Every coolant circuit needs enough water, effective heat exchange and clear outputs. Empty both sides of a coolant circuit before changing its fluid pair.

### 🖥️ Reactor controls and statistics

Track fuel and waste, coolant reserves, fuel burn and current versus potential conversion in the reactor interface. Operating messages explain control-rod limits and output protection.

Persistent heated-coolant or steam buildup reduces conversion and fuel burn. A full output stops further production. Batches promptly extracted by pipes do not trigger the gradual penalty; operation recovers as the retained output clears.

![Reactor interface with fuel, waste and coolant bars, a "Limited by control rods" statistics message and the light/dark settings window](https://cdn.modrinth.com/data/IXXgM5v4/images/747b8050aefac5e085eb630555d1adf31b72f8ee.png)

---

## ⚙️ Industrial Processor

**The original Cyanite Reprocessor has been upgraded and renamed Industrial Processor.** It keeps its cyanite-to-blutonium fuel recycling and now also manufactures **Liquid Obsidian**. The same single-block machine handles both processes, with separate item and fluid outputs. Existing placed processors retain their stored resources.

<!-- Industrial Processor block. -->
![Industrial Processor block](https://cdn.modrinth.com/data/IXXgM5v4/images/4a099fff37a6552ae37289a26e49043ca4d71e58.png)

### ♻️ Fuel recycling and coolant production

Eject cyanite from your reactor and recycle it into blutonium, or supply obsidian to manufacture cold coolant. Configure item, fluid and energy routes for automation.

- ♻️ **2 cyanite ingots + water + FE → 1 blutonium ingot**, preserving the existing recipe and costs.
- 🟣 **1 obsidian block + 100 mB water + FE → 100 mB Liquid Obsidian**.
- 📦 Blutonium leaves through the item output; Liquid Obsidian leaves through the fluid output. Only the output needed by the active recipe can block that process.
- 📚 **JEI, EMI and REI** show both recipes, including the water, energy, time and correct item or fluid result.

With the default configuration, the obsidian recipe takes **200 ticks (10 seconds at 20 TPS)** and uses **200 FE**. The coolant is manufactured cold; the reactor supplies the heat.

Liquid Obsidian circulates as a reusable coolant. It heats up into **Heated Liquid Obsidian** in the reactor, then cools back into Liquid Obsidian in the heat exchanger. **It no longer becomes lava, and ordinary lava cannot supply this coolant circuit.**

> 🔎 Explore coolant properties and conversions through the in-game guide, machine statistics and JEI/EMI/REI. Use that information to tune your own plant.

### 🖥️ Processor interface

Input water sits beside the energy bar on the left. Item slots occupy the center, and the separate product tank on the right stores Liquid Obsidian.

<!-- Industrial Processor interface. -->
![Industrial Processor interface with energy, water, item slots and Liquid Obsidian output](https://cdn.modrinth.com/data/IXXgM5v4/images/e7d65603e924d5b53fd1266b1629ed7569a6f92b.png)

<!-- Historical interface screenshot, retained for reference rather than current publication:
https://cdn.modrinth.com/data/IXXgM5v4/images/8ab6ad762b1a419164f74d53970d6e68041fd365.png -->

---

## 🔥 Heat exchangers where the loops meet

The primary circuit cools your reactor's hot coolant back into its reusable form. For the obsidian loop, **Heated Liquid Obsidian becomes Liquid Obsidian**. The separate secondary circuit turns water into steam.

- 🔁 Return cold coolant to the reactor for a closed loop.
- 💧 Supply water to the secondary circuit.
- ♨️ Extract steam and send it to a turbine.
- 🧩 Place condenser and evaporator channels against each other for better heat transfer.

**Layout matters:** separated channels transfer heat less effectively and can take longer to stabilize. The statistics panel helps you compare what the structure could process with what it is processing now.

<!-- Heat exchanger build view. -->
![A heat exchanger multiblock with its hot and cold channels side by side](https://cdn.modrinth.com/data/IXXgM5v4/images/de8da187ef34fade34046a1cb8b0a7fcaf31aae7.png)

### 🖥️ Heat exchanger controls and statistics

Compare current and potential coolant conversion, water consumption and steam production. Tank levels, channel temperatures and operating messages help you identify a circulation or heat-transfer limit.

<!-- Heat exchanger interface. -->
![Heat exchanger interface with primary and secondary tanks, temperatures, flow rates and the statistics panel](https://cdn.modrinth.com/data/IXXgM5v4/images/95f0b5feb18653dba17f02a61fa9e181967ea85e.png)

### 🛡️ Keep your outputs moving

Persistent steam buildup slows heat exchange, reducing both steam production and the return of cold coolant. A completely full steam output stops further production.

> 🚦 Normal batches removed promptly by your pipes do not trigger this gradual penalty. Clear the bottleneck and operation recovers gradually. Full input tanks are useful reserves.

---

## 🌀 Turn steam into power

Build your turbine, choose its rotor and coils, and tune the selected steam flow.

- 🎛️ Adjust flow to suit your steam supply and turbine design.
- 📊 Monitor rotor speed, efficiency and gross generation.
- 🔁 Return condensed water to the exchanger for a closed water circuit.
- 🚰 Keep the existing excess-condensate discard options when using fresh water.

![A 5×7×5 turbine with its rotor blades and coils visible through the glass](https://cdn.modrinth.com/data/IXXgM5v4/images/311a74a8eadccb856d7ebc52d9b42dfc1213320d.png)

### 🖥️ Turbine controls and statistics

Set the steam flow, coil state and condensate handling. Follow rotor speed, current and potential steam conversion, efficiency and gross FE generation.

Turbines show a subtle, gently moving white haze behind their windows according to stored steam and actual operating flow. Steam remains visible when it is converted as quickly as it arrives; switched-off turbines show only their stored reserve. Occasional condensation droplets fall from any block with free space below after a few seconds of rotating operation. Rotor speed is limited to 2,200 RPM. Assembled reactor, turbine, heat-exchanger and battery ports show connections on the outer and inner faces, with closed chassis sides that fit neatly beside glass.

![Turbine interface showing rotor speed, FE output, steam flow, efficiency, coil and vent controls with the statistics panel](https://cdn.modrinth.com/data/IXXgM5v4/images/6cb949b6a6eb0fcb812ba5321e2b670ebecdff6d.png)

---

## 🔋 A battery that grows with your plant

Build a multiblock battery with casing, a terminal, energy ports and interior cells. **Each cell adds storage and transfer capacity.**

- ➕ **Upper slot:** charge an item using the battery's energy.
- ➖ **Lower slot:** transfer energy from an item into the battery.
- 🛡️ **Equipped gear:** FE-compatible armor and offhand items shown beside the interface charge automatically while the terminal is open.
- 🔌 **Shared transfer budget:** ports, item slots and equipped gear use the battery's available output capacity.

![A 5×5×5 multiblock battery with its charged cells visible through the glass](https://cdn.modrinth.com/data/IXXgM5v4/images/61b43cf25204fb708c5cbf50b96edeecc183a32e.png)

### 🖥️ Battery terminal

Check stored energy, input and output rates, and installed cell capacity. The terminal also brings together the charging slots and equipped-gear controls.

![Battery terminal with stored energy, input and output rates, cell count, the + and − item slots and equipped gear slots](https://cdn.modrinth.com/data/IXXgM5v4/images/7923c1037b7ccc8cd90504319d3d88e12ac32d9c.png)

### 🔴 Automate with stored charge

Add an optional **Battery Redstone Sensor** to the battery wall. Choose a charge percentage,
signal at or above or at or below that threshold, or detect a completely full or empty battery.
Each sensor keeps its own settings and sends its signal through the outer face.

Connect the output to a turbine's redstone port in **Activity / Level** mode to start and stop
generation as the battery fills and drains. The sensor's recipe and operating instructions
are available in the in-game guide.

---

## 🖥️ Consistent interfaces across your plant

Redesigned screens bring resources and controls together. Each machine shows its own operating information, so you can check a bottleneck where it occurs.

Potential values assume the conditions shown in the statistics panel. Compare them with current rates to see where supply, controls or circulation are holding the machine back.

The **settings tab** switches each screen between a light and a dark background, for one machine or for all machines at once.
Redstone sensors follow the appearance of their own reactor, turbine or battery.

---

## 🔊 Hear your power plant come to life

**Machine audio is a new addition in this rework.** Your reactor, heat exchanger and turbine now have their own original operating sounds:

- ⚛️ **Reactor:** a steady industrial hum with coolant-circulation texture.
- 🔥 **Heat exchanger:** deep boiling and bubbling as it operates.
- 🌀 **Turbine:** distinct blade passes with a restrained steam background, changing with rotor speed as it accelerates and slows down.

The sounds are positional and fade with distance, giving your plant an audible presence as you move around it.

**Set the volume for each individual multiblock in its Settings window, or mute it completely.** Turning down one reactor leaves your other reactors, heat exchangers and turbines unchanged. These are your personal listening settings; they do not change another player's volume. Sharing a light/dark appearance across machines never shares their sound settings.

---

## 📖 An engineering guide in your hands

Learn the plant inside the game:

- 🧊 Interactive multiblock views.
- 🧱 Parts, materials and recipes.
- 🔁 Coolant circuits and operating explanations.
- 🔎 Advice for finding performance bottlenecks.

---

## 🧩 Optional integrations

| Integration | What it adds |
| --- | --- |
| 📚 JEI, EMI and REI | Both Industrial Processor recipes, moderators, turbine coils and current heat-exchanger fluid conversions. |
| 💻 OpenComputers | Native cable-connected components for reactor, turbine and heat-exchanger monitoring and control. |
| 💻 CC:Tweaked | Native computer ports for reactor, turbine and heat-exchanger telemetry and controls, with direct or wired-modem access. Works alongside OpenComputers on the same ports. |
| 🔎 Jade | Supported machine information in the inspection overlay. |
| 🧪 Mekanism | The liquid sodium coolant option. |
| ❄️ Ad Astra | The Cryo Fuel coolant option. |
| 🛡️ Carry On | Protects assembled multiblocks from accidental pickup while keeping loose and disassembled parts carryable. |

> 📦 **No additional mod is required.** The base mod runs with Minecraft and NeoForge alone.

---

## 📋 Requirements

| Requirement | Version |
| --- | --- |
| 🎮 Minecraft | **1.21.1** |
| 🛠️ Loader | **NeoForge 21.1.264+** for Minecraft 1.21.1 |
| ☕ Java | **21** |
| 📦 Additional required mods | **None** |

---

## ❓ FAQ

**Can I import my original BiggerReactors machines?**  
This fork is designed for new worlds. It does not migrate worlds or machines from the original BiggerReactors.

**Do I need Mekanism or Ad Astra?**  
No. They add optional coolant choices. Water and Liquid Obsidian are included.

**How do I make Liquid Obsidian now?**  
Use the Industrial Processor: **1 obsidian block + 100 mB water + FE → 100 mB Liquid Obsidian**. The reactor heats that coolant; the exchanger cools it back. Lava is not part of this loop.

**Will my old lava and obsidian coolant loop keep working?**  
Affected reactors and heat exchangers report an invalid fluid and stop. Empty both affected tanks, then refill with a supported coolant. Stored lava is not automatically converted into Heated Liquid Obsidian.

**Can I put heated coolant into the turbine?**  
Use steam in the turbine. Send heated coolant to the heat exchanger to generate that steam.

**Can coolant choice replace good circulation?**  
No. Water supply, tank availability, channel layout and extraction still matter.

**Why is actual production below potential?**  
Check the operating message and the assumptions shown in the statistics panel. Potential assumes ideal conditions for that machine.

---

## ❤️ Built on a legacy

BiggerReactors-Rework is an **independent fork**, built on the work of the original projects and their contributors.

- ⚛️ **[RogueLogix](https://www.curseforge.com/members/roguelogix)** — creator of **[BiggerReactors](https://github.com/BiggerSeries/BiggerReactors)**, the direct foundation of this rework.
- 🧬 **[ErogenousBeef](https://github.com/erogenousbeef-zz)** — creator of the original **[Big Reactors](https://github.com/erogenousbeef-zz/BigReactors)**.
- 🛠️ **[gizmocodes](https://github.com/BiggerSeries/BiggerReactors/graphs/contributors)** and **[Ridanisaurus](https://github.com/BiggerSeries/BiggerReactors/graphs/contributors)** — contributors to BiggerReactors.
- 🔧 **[vindocel](https://github.com/vindocel)** — maintainer of **[BiggerReactors-Rework](https://github.com/vindocel/BiggerReactors-Rework)**.

This rework is not affiliated with or endorsed by the original authors. Applicable attribution and license notices are preserved under the **[GNU Lesser General Public License v2.1](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html)**.

---

🐛 **Found a problem?** Visit the **[issue tracker](https://github.com/vindocel/BiggerReactors-Rework/issues)**.  
💻 **Want to explore the code?** Browse the **[source repository](https://github.com/vindocel/BiggerReactors-Rework)**.
