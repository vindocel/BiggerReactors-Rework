package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.calculator.design.*;
import biggerreactorsrework.calculator.simulation.PlantRun;
import biggerreactorsrework.calculator.simulation.PlantSpec;
import biggerreactorsrework.calculator.simulation.VirtualPlant;
import biggerreactorsrework.fluids.LiquidObsidian;
import biggerreactorsrework.multiblocks.heatexchanger.HeatExchangerMultiblockController;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerBaseTile;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerFluidPortTile;
import biggerreactorsrework.multiblocks.reactor.ReactorMultiblockController;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorAccessPort;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorAccessPortTile;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorBaseTile;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorCoolantPortTile;
import biggerreactorsrework.multiblocks.turbine.TurbineMultiblockController;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineFluidPort;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineBaseTile;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineFluidPortTile;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.registries.ReactorModeratorRegistry;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;

/** Added 2026-10-28: plants emitted by the calculator assemble with the real validators and match the virtual plant. */
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class CalculatorGameTests {
    private static final BlockPos ORIGIN = new BlockPos(1, 1, 1);

    static void place(GameTestHelper h, PlantLayout plant) {
        for (var placed : plant.machines())
            placed.machine().blocks().forEach((pos, state) -> h.setBlock(pos.offset(placed.offset()).offset(ORIGIN), state));
    }

    static boolean assembled(GameTestHelper h, PlantLayout plant) {
        for (var placed : plant.machines())
            for (var entry : placed.machine().blocks().entrySet())
                if (entry.getValue().hasProperty(ASSEMBLED) && !h.getBlockState(entry.getKey().offset(placed.offset()).offset(ORIGIN)).getValue(ASSEMBLED)) return false;
        return true;
    }

    private static BlockPos at(PlantLayout plant, int machine, BlockPos local) {
        return local.offset(plant.machines().get(machine).offset()).offset(ORIGIN);
    }

    /** Port directions are tile settings the player chooses in each port's screen. */
    static void configurePorts(GameTestHelper h, PlantLayout plant) {
        for (int m = 0; m < plant.machines().size(); m++) {
            var machine = plant.machines().get(m).machine();
            for (var port : machine.ports()) {
                var tile = h.getBlockEntity(at(plant, m, port.pos()));
                if (tile instanceof ReactorCoolantPortTile reactorPort)
                    reactorPort.setDirection(port.role().inlet() ? ReactorAccessPort.PortDirection.INLET : ReactorAccessPort.PortDirection.OUTLET);
                else if (tile instanceof TurbineFluidPortTile turbinePort)
                    turbinePort.setDirection(port.role().inlet() ? TurbineFluidPort.PortDirection.INLET : TurbineFluidPort.PortDirection.OUTLET);
                else if (tile instanceof HeatExchangerFluidPortTile exchangerPort)
                    exchangerPort.controller().setInletPort(exchangerPort, port.role().inlet());
            }
        }
    }

    private static void assertConnected(GameTestHelper h, PlantLayout plant) {
        h.assertTrue(PlantAssembler.connected(plant), "emitted ports do not touch");
    }

    static ReactorDesign liquidReactor() { return new ReactorDesign(4, 5, 4, ReactorDesign.Pattern.CHECKER, null, 40); }

    static TurbineDesign ironTurbine(long flow) { return new TurbineDesign(2, 3, 1, 45, Blocks.IRON_BLOCK, flow); }

    @GameTest(template = "empty_64x48x40", timeoutTicks = 200)
    public static void emittedSolidReactorAssembles(GameTestHelper h) {
        var plant = PlantAssembler.solid(new ReactorDesign(5, 4, 5, ReactorDesign.Pattern.CHECKER, net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.parse("biggerreactorsrework:graphite_block")), 0));
        place(h, plant);
        h.succeedWhen(() -> h.assertTrue(assembled(h, plant), "solid reactor did not assemble"));
    }

    @GameTest(template = "empty_64x48x40", timeoutTicks = 200)
    public static void emittedWaterPlantAssembles(GameTestHelper h) {
        var plant = PlantAssembler.water(liquidReactor(), ironTurbine(1000));
        h.assertTrue(plant != null, "water plant could not be assembled");
        assertConnected(h, plant);
        place(h, plant);
        h.succeedWhen(() -> h.assertTrue(assembled(h, plant), "water plant did not assemble"));
    }

    @GameTest(template = "empty_64x48x40", timeoutTicks = 200)
    public static void emittedAdvancedPlantAssembles(GameTestHelper h) {
        var plant = PlantAssembler.advanced(new ReactorDesign(4, 6, 4, ReactorDesign.Pattern.CHECKER, null, 40), new ExchangerDesign(6, 2), ironTurbine(1000));
        h.assertTrue(plant != null, "advanced plant could not be assembled");
        assertConnected(h, plant);
        place(h, plant);
        h.succeedWhen(() -> h.assertTrue(assembled(h, plant), "advanced plant did not assemble"));
    }

    /** The exchanger geometry the calculator counts must equal what the controller counted on assembly. */
    @GameTest(template = "empty_64x48x40", timeoutTicks = 200)
    public static void exchangerGeometryMatchesController(GameTestHelper h) {
        var design = new ExchangerDesign(7, 4);
        var plant = new PlantLayout(List.of(new PlantLayout.Placed(design.layout(), BlockPos.ZERO)), List.of());
        place(h, plant);
        h.succeedWhen(() -> {
            h.assertTrue(assembled(h, plant), "exchanger did not assemble");
            var c = ((HeatExchangerBaseTile) h.getBlockEntity(at(plant, 0, design.hotIn()))).controller();
            var g = design.geometry();
            h.assertTrue(c.condenserChannels.size() == g.condenser() && c.evaporatorChannels.size() == g.evaporator(), "channel count differs");
            h.assertTrue(c.channelRFKT == g.channelRFKT(), "contact differs: " + c.channelRFKT + " vs " + g.channelRFKT());
            h.assertTrue(c.condenserAirRFKT == g.condenserAirRFKT() && c.evaporatorAirRFKT == g.evaporatorAirRFKT(), "air contact differs");
            h.assertTrue(c.airAmbientRFKT == g.ambientRFKT() && c.airHeatBody.rfPerKelvin() == g.airRFK(), "ambient path differs");
        });
    }

    /** Folded paths must stay continuous, agree with the controller and connect without padding other machines. */
    @GameTest(template = "empty_64x48x40", timeoutTicks = 200)
    public static void foldedExchangerPlantAssembles(GameTestHelper h) {
        var exchanger = new ExchangerDesign(6, 2, 2);
        var plant = PlantAssembler.advanced(new ReactorDesign(4, 3, 4, ReactorDesign.Pattern.CHECKER, null, 40),
                exchanger, ironTurbine(1000));
        h.assertTrue(plant != null, "folded exchanger forced unnecessary reactor/turbine dimensions");
        assertConnected(h, plant);
        place(h, plant);
        assertExchangerGeometry(h, plant, 1, exchanger);
    }

    @GameTest(template = "empty_64x48x40", timeoutTicks = 200)
    public static void threePlaneExchangerGeometryMatchesController(GameTestHelper h) {
        var exchanger = new ExchangerDesign(5, 4, 3);
        var plant = new PlantLayout(List.of(new PlantLayout.Placed(exchanger.layout(), BlockPos.ZERO)), List.of());
        place(h, plant);
        assertExchangerGeometry(h, plant, 0, exchanger);
    }

    private static void assertExchangerGeometry(GameTestHelper h, PlantLayout plant, int index, ExchangerDesign design) {
        h.succeedWhen(() -> {
            h.assertTrue(assembled(h, plant), "folded heat paths did not assemble");
            var controller = ((HeatExchangerBaseTile) h.getBlockEntity(at(plant, index, design.hotIn()))).controller();
            var g = design.geometry();
            h.assertTrue(controller.condenserChannels.size() == g.condenser() && controller.evaporatorChannels.size() == g.evaporator(), "folded channel count differs");
            h.assertTrue(controller.channelRFKT == g.channelRFKT(), "folded channel contacts differ");
            h.assertTrue(controller.condenserAirRFKT == g.condenserAirRFKT() && controller.evaporatorAirRFKT == g.evaporatorAirRFKT(), "folded air contacts differ");
            h.assertTrue(controller.airAmbientRFKT == g.ambientRFKT() && controller.airHeatBody.rfPerKelvin() == g.airRFK(), "folded heat loss geometry differs");
            h.assertTrue(controller.condenserTank.perSideCapacity == design.channelVolume()
                    && controller.evaporatorTank.perSideCapacity == design.channelVolume(), "folded tank capacities differ");
        });
    }

    /**
     * The same plant assembled in the level and run through its real controllers and ports, in the same order and
     * with the same charges, against the virtual plant.
     */
    @GameTest(template = "empty_64x48x40", timeoutTicks = 400)
    public static void virtualAdvancedPlantMatchesAssembledPlant(GameTestHelper h) {
        var reactor = new ReactorDesign(4, 6, 4, ReactorDesign.Pattern.CHECKER, null, 30);
        var exchanger = new ExchangerDesign(6, 2, 2);
        var turbine = ironTurbine(400);
        var plant = PlantAssembler.advanced(reactor, exchanger, turbine);
        h.assertTrue(plant != null, "plant could not be assembled");
        place(h, plant);
        var coolant = FluidTransitionRegistry.liquidTransition(LiquidObsidian.INSTANCE);
        var water = FluidTransitionRegistry.liquidTransition(Fluids.WATER);
        long primary = reactor.manifoldBlocks() * biggerreactorsrework.Config.CONFIG.Reactor.CoolantTankAmountPerFuelRod;
        long secondary = exchanger.channelVolume() / 2;
        var spec = new PlantSpec(reactor, null, coolant, exchanger, water, turbine, TurbineCoilRegistry.getCoilData(Blocks.IRON_BLOCK), primary, secondary);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(assembled(h, plant), "plant did not assemble")).thenExecute(() -> {
            configurePorts(h, plant);
            var r = ((ReactorBaseTile) h.getBlockEntity(at(plant, 0, plant.machines().get(0).machine().ports().getFirst().pos()))).controller();
            var x = ((HeatExchangerBaseTile) h.getBlockEntity(at(plant, 1, exchanger.hotIn()))).controller();
            var t = ((TurbineBaseTile) h.getBlockEntity(at(plant, 2, plant.machines().get(2).machine().ports().getFirst().pos()))).controller();
            r.setAllControlRodLevels(reactor.insertion());
            r.setActive(biggerreactorsrework.multiblocks.reactor.state.ReactorActivity.ACTIVE);
            t.simulation().setNominalFlowRate(turbine.flow());
            t.simulation().setVentState(biggerreactorsrework.multiblocks.turbine.state.VentState.CLOSED);
            t.setActive(true);
            var coldIn = (ReactorCoolantPortTile) h.getBlockEntity(at(plant, 0, plant.machines().get(0).machine().port(MachineLayout.Role.REACTOR_COLD_IN).pos()));
            var waterIn = (HeatExchangerFluidPortTile) h.getBlockEntity(at(plant, 1, exchanger.waterIn()));
            var virtual = new VirtualPlant(spec, false);
            long primaryLeft = primary, waterLeft = secondary;
            int ticks = 30000, window = 5000;
            double realPower = 0, virtualPower = 0, realFuel = 0, virtualFuel = 0;
            for (int tick = 0; tick < ticks; tick++) {
                if (primaryLeft > 0) primaryLeft -= coldIn.fill(LiquidObsidian.INSTANCE, DataComponentPatch.EMPTY, primaryLeft, false);
                if (waterLeft > 0) waterLeft -= waterIn.fill(Fluids.WATER, DataComponentPatch.EMPTY, waterLeft, false);
                r.simulation().fuelTank().extractWaste(Long.MAX_VALUE, false);
                r.simulation().fuelTank().insertFuel(Long.MAX_VALUE, false);
                r.tick();
                x.tick();
                t.tick();
                long exported = t.simulation().battery().extract(t.simulation().battery().stored());
                virtual.tick();
                if (tick >= ticks - window) {
                    realPower += exported;
                    virtualPower += virtual.exportedLastTick;
                    realFuel += r.simulation().fuelTank().burnedLastTick();
                    virtualFuel += virtual.burnedLastTick;
                }
            }
            realPower /= window;
            virtualPower /= window;
            realFuel /= window;
            virtualFuel /= window;
            BiggerReactors.LOGGER.info("Calculator equivalence: real {} FE/t {} mB/t, virtual {} FE/t {} mB/t", realPower, realFuel, virtualPower, virtualFuel);
            h.assertTrue(realPower > 0, "assembled plant produced nothing");
            h.assertTrue(Math.abs(realPower - virtualPower) <= .01 * realPower, "virtual power " + virtualPower + " differs from assembled " + realPower);
            h.assertTrue(Math.abs(realFuel - virtualFuel) <= .01 * Math.max(realFuel, 1e-9), "virtual fuel " + virtualFuel + " differs from assembled " + realFuel);
        }).thenSucceed();
    }

    private static biggerreactorsrework.calculator.search.PlantSearch.Outcome search(long target, boolean liquid, String material, String coil) {
        var catalog = biggerreactorsrework.calculator.CalculatorCatalog.capture();
        var request = new biggerreactorsrework.calculator.CalculatorRequest(target, "biggerreactorsrework:uranium_ingot", liquid, material, coil, catalog.fingerprint());
        var route = liquid ? catalog.coolantRoute(material) : biggerreactorsrework.calculator.Route.SOLID;
        return biggerreactorsrework.calculator.search.PlantSearch.run(request, route, catalog, (phase, done, total) -> { }, () -> false);
    }

    /** Every route reaches a modest target, conserves its fluids, and the stored record reads back unchanged. */
    @GameTest(template = "empty_7x7x7", timeoutTicks = 4000)
    public static void searchMeetsTargetsOnEveryRoute(GameTestHelper h) throws Exception {
        var rows = new ArrayList<Map<String, Object>>();
        Object[][] cases = {{"solid", 20000L, false, "biggerreactorsrework:graphite_block", ""},
                {"direct", 5000L, true, "minecraft:water", "minecraft:iron_block"},
                {"advanced", 5000L, true, "biggerreactorsrework:liquid_obsidian", "minecraft:gold_block"}};
        for (var c : cases) {
            long start = System.currentTimeMillis();
            var outcome = search((long) c[1], (boolean) c[2], (String) c[3], (String) c[4]);
            var row = new LinkedHashMap<String, Object>();
            row.put("case", c[0]);
            row.put("target", c[1]);
            row.put("status", outcome.status().name());
            row.put("ms", System.currentTimeMillis() - start);
            row.put("evaluated", outcome.evaluated());
            row.put("limited", outcome.searchLimited());
            if (outcome.validation() != null) {
                var v = outcome.validation();
                row.put("power", v.power());
                row.put("power_alt", outcome.alternate() == null ? null : outcome.alternate().power());
                row.put("fuel", v.fuel());
                row.put("ticks", v.ticks());
                row.put("rpm", v.rpm());
                row.put("insertion", outcome.spec().reactor().insertion());
                row.put("reactor", outcome.spec().reactor().x() + "x" + outcome.spec().reactor().y() + "x" + outcome.spec().reactor().z() + " " + outcome.spec().reactor().pattern());
                if (outcome.spec().turbine() != null) {
                    var t = outcome.spec().turbine();
                    row.put("turbine", t.toString());
                    var steam = outcome.spec().exchanger() != null ? outcome.spec().water() : outcome.spec().coolant();
                    var bench = biggerreactorsrework.calculator.simulation.Bench.turbine(t, outcome.spec().coil(), steam, () -> false);
                    row.put("turbine_bench", bench.output() + " FE/t at " + bench.rpm() + " RPM converged=" + bench.converged());
                }
                if (outcome.spec().exchanger() != null) row.put("exchanger", outcome.spec().exchanger().toString());
                row.put("primary_error", v.primaryError());
                row.put("water_error", v.waterError());
            }
            rows.add(row);
            BiggerReactors.LOGGER.info("Calculator search: {}", row);
            h.assertTrue(outcome.status() == biggerreactorsrework.calculator.CalculationStatus.TARGET_MET, c[0] + " search did not meet its target: " + row);
            h.assertTrue(outcome.validation().power() >= (long) c[1], c[0] + " validated power is below the target");
            h.assertTrue(outcome.validation().primaryError() == 0 && outcome.validation().waterError() == 0, c[0] + " circuit created or lost fluid");
            h.assertTrue(PlantAssembler.connected(outcome.plant()), c[0] + " ports do not touch");
            var catalog = biggerreactorsrework.calculator.CalculatorCatalog.capture();
            var request = new biggerreactorsrework.calculator.CalculatorRequest((long) c[1], "biggerreactorsrework:uranium_ingot", (boolean) c[2], (String) c[3], (String) c[4], catalog.fingerprint());
            var route = (boolean) c[2] ? catalog.coolantRoute((String) c[3]) : biggerreactorsrework.calculator.Route.SOLID;
            var record = biggerreactorsrework.calculator.service.CalculatorService.build(request, route, catalog, outcome);
            var copy = biggerreactorsrework.calculator.CalculationResult.fromTag(record.toTag());
            h.assertTrue(copy != null && copy.values().equals(record.values()), c[0] + " record did not read back");
            for (int m = 0; m < record.plant().machines().size(); m++)
                h.assertTrue(copy.plant().machines().get(m).machine().blocks().equals(record.plant().machines().get(m).machine().blocks()), c[0] + " layout changed on save");
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("calculator-search.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(rows));
        h.succeed();
    }

    /**
     * The decisive check: the plant the search recommends, built block by block in the level with its charges and
     * settings, run through the real controllers and ports, against the validated prediction.
     */
    @GameTest(template = "empty_64x48x40", timeoutTicks = 1200)
    public static void searchedDirectPlantMatchesAssembledPlant(GameTestHelper h) {
        var outcome = search(5000, true, "minecraft:water", "minecraft:iron_block");
        h.assertTrue(outcome.status() == biggerreactorsrework.calculator.CalculationStatus.TARGET_MET, "search failed: " + outcome.status());
        var spec = outcome.spec();
        var plant = outcome.plant();
        place(h, plant);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(assembled(h, plant), "searched plant did not assemble")).thenExecute(() -> {
            configurePorts(h, plant);
            var reactorPort = plant.machines().get(0).machine().port(MachineLayout.Role.REACTOR_COLD_IN).pos();
            var r = ((ReactorBaseTile) h.getBlockEntity(at(plant, 0, reactorPort))).controller();
            var t = ((TurbineBaseTile) h.getBlockEntity(at(plant, 1, plant.machines().get(1).machine().ports().getFirst().pos()))).controller();
            r.setAllControlRodLevels(spec.reactor().insertion());
            r.setActive(biggerreactorsrework.multiblocks.reactor.state.ReactorActivity.ACTIVE);
            t.simulation().setNominalFlowRate(spec.turbine().flow());
            t.simulation().setVentState(biggerreactorsrework.multiblocks.turbine.state.VentState.CLOSED);
            t.setActive(true);
            var coldIn = (ReactorCoolantPortTile) h.getBlockEntity(at(plant, 0, reactorPort));
            long left = spec.primaryCharge();
            int ticks = (int) outcome.validation().ticks() + 8000, window = 8000;
            double power = 0, fuel = 0;
            for (int tick = 0; tick < ticks; tick++) {
                if (left > 0) left -= coldIn.fill(Fluids.WATER, DataComponentPatch.EMPTY, left, false);
                r.simulation().fuelTank().extractWaste(Long.MAX_VALUE, false);
                r.simulation().fuelTank().insertFuel(Long.MAX_VALUE, false);
                r.tick();
                t.tick();
                long exported = t.simulation().battery().extract(t.simulation().battery().stored());
                if (tick >= ticks - window) {
                    power += exported;
                    fuel += r.simulation().fuelTank().burnedLastTick();
                }
            }
            power /= window;
            fuel /= window;
            double predicted = outcome.validation().power();
            BiggerReactors.LOGGER.info("Searched plant in the level: {} FE/t {} mB/t; predicted {} FE/t {} mB/t", power, fuel, predicted, outcome.validation().fuel());
            h.assertTrue(power >= 5000, "assembled plant missed the target: " + power);
            // The prediction may be conservative (slow rise after the validation windows), never optimistic.
            h.assertTrue(power >= .99 * predicted && power <= 1.03 * predicted, "assembled " + power + " differs from predicted " + predicted);
            h.assertTrue(Math.abs(fuel - outcome.validation().fuel()) <= .02 * outcome.validation().fuel(), "fuel " + fuel + " differs from predicted " + outcome.validation().fuel());
        }).thenSucceed();
    }

    /** The author's sodium setup reaches about 468 kRF/t with a 6x5x6 reactor: the search must stay in that range of size. */
    @GameTest(template = "empty_7x7x7", timeoutTicks = 12000)
    public static void sodiumPlantForHalfAMillionStaysCompact(GameTestHelper h) throws Exception {
        if (!net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(net.minecraft.resources.ResourceLocation.parse("mekanism:sodium"))) {
            h.succeed();
            return;
        }
        var rows = new ArrayList<Map<String, Object>>();
        for (long target : new long[]{300000, 500000, 3000000, 15000000}) {
        long start = System.currentTimeMillis();
        var outcome = search(target, true, "mekanism:sodium", "biggerreactorsrework:ludicrite_block");
        var row = new LinkedHashMap<String, Object>();
        row.put("target", target);
        row.put("status", outcome.status().name());
        row.put("ms", System.currentTimeMillis() - start);
        row.put("limited", outcome.searchLimited());
        row.put("evaluated", outcome.evaluated());
        if (outcome.plant() != null) {
            for (var placed : outcome.plant().machines())
                row.put(placed.machine().kind().name(), placed.machine().width() + "x" + placed.machine().height() + "x" + placed.machine().depth());
            row.put("power", outcome.validation().power());
            row.put("fuel", outcome.validation().fuel());
            row.put("insertion", outcome.spec().reactor().insertion());
            row.put("turbine", outcome.spec().turbine().toString());
            row.put("exchanger", outcome.spec().exchanger().toString());
        }
        BiggerReactors.LOGGER.info("Sodium plant: {}", row);
        rows.add(row);
        h.assertTrue(outcome.status() == biggerreactorsrework.calculator.CalculationStatus.TARGET_MET, "sodium search did not meet its target: " + row);
        if (target == 3000000) {
            var x = outcome.spec().exchanger();
            h.assertTrue(x.layers() > 1, "large target still uses a flat exchanger: " + row);
            long volume = outcome.plant().machines().stream().mapToLong(p -> (long) p.machine().width() * p.machine().height() * p.machine().depth()).sum();
            h.assertTrue(volume < 10L * 6 * 9 + 48L * 5 * 4 + 11L * 32 * 11, "compact search regressed from the long baseline: " + row);
        }
        if (target == 15000000) {
            var t = outcome.spec().turbine();
            h.assertTrue(t.width() > 17 && t.height() < 59, "saved high-target regression stayed narrow and tall: " + row);
            h.assertTrue(!outcome.searchLimited(), "preparation exhausted the high-target search again: " + row);
            h.assertTrue(outcome.validation().primaryError() == 0 && outcome.validation().waterError() == 0, "high-target circuit lost fluid");
        }
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("calculator-sodium.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));
        h.succeed();
    }
}
