package biggerreactorsrework.gametest;

// Modified 2026-10-30: validate command boundaries, persisted state and invalidated neighbor capabilities.

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.Config;
import biggerreactorsrework.core.capability.NeighborCapability;
import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.core.config.ConfigManager;
import biggerreactorsrework.core.serialization.DataCompound;
import biggerreactorsrework.machine.blocks.CyaniteReprocessor;
import biggerreactorsrework.machine.tiles.CyaniteReprocessorTile;
import biggerreactorsrework.machine.side.*;
import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.reactor.tiles.*;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorAccessPortContainer;
import biggerreactorsrework.multiblocks.reactor.simulation.base.FuelTank;
import biggerreactorsrework.multiblocks.turbine.simulation.modern.ModernTurbineSimulation;
import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

public final class StabilizationGameTests {
    @GameTest(template = "empty_7x7x7")
    public static void markingMachineDataDirtyDoesNotLoadRemoteChunks(GameTestHelper h) {
        var pos=h.absolutePos(BlockPos.ZERO).offset(32768,0,32768);
        var chunks=h.getLevel().getChunkSource();
        h.assertTrue(chunks.getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null,"chosen remote chunk is already loaded");
        var bounds=new org.joml.Vector3i(pos.getX(),pos.getY(),pos.getZ());
        biggerreactorsrework.core.util.Util.markRangeDirty(h.getLevel(),bounds,bounds);
        h.assertTrue(chunks.getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null,"dirty marking loaded an unrelated chunk");
        h.succeed();
    }

    @GameTest(template = "empty_7x7x7", timeoutTicks = 140)
    public static void menuDispatchRejectsMalformedPortCommandsAndStaleOrigins(GameTestHelper h) {
        for (int x=1;x<=3;x++) for (int y=1;y<=3;y++) for (int z=1;z<=3;z++)
            if (x==1 || x==3 || y==1 || y==3 || z==1 || z==3) h.setBlock(x,y,z,ReactorCasing.INSTANCE);
        h.setBlock(2,2,2,ReactorFuelRod.INSTANCE);
        h.setBlock(2,3,2,ReactorControlRod.INSTANCE);
        h.setBlock(1,2,2,ReactorTerminal.INSTANCE);
        h.setBlock(3,2,2,ReactorAccessPort.INSTANCE);
        h.startSequence().thenWaitUntil(() -> {
            var tile=(ReactorAccessPortTile)GameTestSupport.be(h,new BlockPos(3,2,2));
            h.assertTrue(tile.nullableController()!=null && tile.controller().simulation()!=null,"reactor not assembled");
        }).thenExecute(() -> {
            var pos=h.absolutePos(new BlockPos(3,2,2));
            var tile=(ReactorAccessPortTile)h.getLevel().getBlockEntity(pos);
            var player=FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"port_audit"));
            player.setPos(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5);
            var previous=player.containerMenu;
            try {
                player.containerMenu=new ReactorAccessPortContainer(73,pos,player);
                for (Object invalid : new Object[]{"bad",1L,List.of(1),-1,2}) {
                    request(player,tile,"setDirection",invalid,73,pos);
                    request(player,tile,"setFuelMode",invalid,73,pos);
                }
                request(player,tile,"setDirection",null,73,pos);
                h.assertTrue(tile.isInlet(),"malformed command changed port direction");
                request(player,tile,"setDirection",1,74,pos);
                request(player,tile,"setDirection",1,73,pos.above());
                h.assertTrue(tile.isInlet(),"stale menu or position reached the tile");
                request(player,tile,"setDirection",1,73,pos);
                h.assertFalse(tile.isInlet(),"valid command did not reach the tile");
                player.setPos(pos.getX()+100,pos.getY(),pos.getZ());
                request(player,tile,"setDirection",0,73,pos);
                h.assertFalse(tile.isInlet(),"out-of-range command reached the tile");
            } finally { player.containerMenu=previous; }
        }).thenSucceed();
    }

    private static void request(net.minecraft.server.level.ServerPlayer player, ReactorAccessPortTile tile,
                                String name,Object value,int menu,BlockPos pos) {
        var packet=new DataCompound();packet.put("request",name);packet.put("data",value);
        packet.put("menu",menu);packet.put("position",pos.asLong());
        GuiSync.handleServerRequest(new DataCompound(packet.toROBN()),player);
    }

    @GameTest(template = "empty_7x7x7")
    public static void idleProcessorChargingMarksChunkAndRoundTripsCurrentState(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CyaniteReprocessor.INSTANCE);
        var tile=(CyaniteReprocessorTile)GameTestSupport.be(h,pos);tile.tick();
        var chunk=h.getLevel().getChunkAt(h.absolutePos(pos));
        chunk.tryMarkSaved();
        var energy=tile.energyFor(null);
        int offered=Math.min(500,energy.getMaxEnergyStored());
        int accepted=energy.receiveEnergy(offered,true);
        h.assertTrue(accepted>0 && !chunk.isUnsaved(),"simulated charge dirtied the chunk");
        h.assertTrue(energy.getEnergyStored()==0,"simulated charge changed energy");
        energy.receiveEnergy(offered,false);tile.tick();
        h.assertTrue(chunk.isUnsaved(),"idle processor charge was not marked for disk saving");
        var saved=tile.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored=CyaniteReprocessorTile.SUPPLIER.create(tile.getBlockPos(),tile.getBlockState());
        restored.setLevel(h.getLevel());GameTestSupport.load(restored,saved,h);
        h.assertTrue(restored.energyFor(null).getEnergyStored()==accepted,"current processor charge was lost");
        h.succeed();
    }

    @GameTest(template = "empty_7x7x7")
    public static void cachedNeighborTracksSideChangesWithoutBlockReplacement(GameTestHelper h) {
        var ownerPos=new BlockPos(2,2,2);var neighborPos=ownerPos.east();
        h.setBlock(ownerPos,CyaniteReprocessor.INSTANCE);h.setBlock(neighborPos,CyaniteReprocessor.INSTANCE);
        var owner=(CyaniteReprocessorTile)GameTestSupport.be(h,ownerPos);
        var neighbor=(CyaniteReprocessorTile)GameTestSupport.be(h,neighborPos);
        var cache=NeighborCapability.fluids(owner);
        neighbor.runRequest("sideClearAll",null);
        h.assertTrue(cache.get(Direction.EAST)==null,"disabled neighbor exposed fluid capability");
        neighbor.runRequest("sideBatch",Transmission.FLUID.ordinal()*2);
        h.assertTrue(cache.get(Direction.EAST)!=null,"absent capability remained cached after enabling input");
        neighbor.runRequest("sideClearAll",null);
        h.assertTrue(cache.get(Direction.EAST)==null,"stale capability survived a side change");
        h.succeed();
    }

    @GameTest(template = "empty_7x7x7", batch = "server_configuration")
    public static void realServerSnapshotUsesStableIdsAndRestoresLocalRules(GameTestHelper h) {
        var snapshot=ConfigManager.synchronizedSnapshot(true);
        var values=snapshot.getMap("configs");
        h.assertTrue(values.keySet().stream().anyMatch(id->id.toString().contains(":server:")),"SERVER rules missing from snapshot");
        var registration=ConfigManager.getAllConfigsForMod(BiggerReactors.modid).stream()
                .filter(c->c.configType==biggerreactorsrework.core.config.ConfigType.SERVER).findFirst().orElseThrow();
        var node=(biggerreactorsrework.core.config.spec.SpecObjectNode)registration.rootConfigSpecNode.subNodes.get("Battery");
        var capacity=node.subNodes.get("CellCapacity");
        var original=capacity.generateSyncElement();long expected=Config.CONFIG.Battery.CellCapacity;
        var reactor=(biggerreactorsrework.core.config.spec.SpecObjectNode)registration.rootConfigSpecNode.subNodes.get("Reactor");
        var temperature=reactor.subNodes.get("MaxFuelTemperature");var originalTemperature=temperature.generateSyncElement();
        try {
            temperature.writeElement(new biggerreactorsrework.core.parsers.Element(biggerreactorsrework.core.parsers.Element.Type.Number,null,"MaxFuelTemperature",4000d));
            h.assertTrue(Config.CONFIG.Reactor.MaxFuelTemperature==4000,"temperature config was compiled as a constant");
            capacity.writeElement(new biggerreactorsrework.core.parsers.Element(biggerreactorsrework.core.parsers.Element.Type.Number,null,"CellCapacity",1L));
            ConfigManager.applyRemoteSnapshot(new DataCompound(snapshot.toROBN()));
            h.assertTrue(Config.CONFIG.Battery.CellCapacity==expected,"server values were not applied by stable id");
            ConfigManager.restoreLocalConfigs();
            h.assertTrue(Config.CONFIG.Battery.CellCapacity==1,"disconnect did not restore local rules");
        } finally { ConfigManager.restoreLocalConfigs();capacity.writeElement(original);temperature.writeElement(originalTemperature); }
        h.succeed();
    }

    @GameTest(template = "empty_7x7x7")
    public static void malformedSavedStateDoesNotLoseRecoverableFluidOrPoisonNumbers(GameTestHelper h) {
        var fuel=new FuelTank(1000);var tag=new DataCompound();
        tag.put("fuel",-10L);tag.put("waste",25L);tag.put("partialUsed",Double.NaN);fuel.load(tag);
        h.assertTrue(fuel.fuel()==0 && fuel.waste()==25,"invalid fuel state was not isolated");
        var bad=new CompoundTag();bad.putLong("inAmount",125);bad.putString("inFluid","INVALID FLUID!");
        var tank=new FluidTransitionTank(false);tank.deserializeNBT(bad);
        h.assertTrue(tank.hasUnresolvedProfile() && tank.serializeNBT().getLongOr("inAmount", 0L)==125,"malformed fluid identifier lost stored stock");
        var turbine=new ModernTurbineSimulation();bad=new CompoundTag();bad.putInt("ventState",999);bad.putDouble("rotorEnergy",Double.NaN);
        turbine.deserializeNBT(bad);
        h.assertTrue(turbine.serializeNBT().getDoubleOr("rotorEnergy", 0D)==0,"nonfinite rotor state was retained");
        h.setBlock(1,1,1,ReactorCoolantPort.INSTANCE);
        h.setBlock(2,1,1,biggerreactorsrework.multiblocks.turbine.blocks.TurbineFluidPort.INSTANCE);
        h.setBlock(3,1,1,biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerFluidPortBlock.INSTANCE);
        h.setBlock(4,1,1,biggerreactorsrework.multiblocks.battery.blocks.BatteryEnergyPort.INSTANCE);
        var coolant=(ReactorCoolantPortTile)GameTestSupport.be(h,new BlockPos(1,1,1));
        var turbinePort=(biggerreactorsrework.multiblocks.turbine.tiles.TurbineFluidPortTile)GameTestSupport.be(h,new BlockPos(2,1,1));
        var exchangerPort=(biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerFluidPortTile)GameTestSupport.be(h,new BlockPos(3,1,1));
        var batteryPort=(biggerreactorsrework.multiblocks.battery.tiles.BatteryEnergyPortTile)GameTestSupport.be(h,new BlockPos(4,1,1));
        for(Object invalid:new Object[]{"wrong type",1L,-1,2}) {
            coolant.runRequest("setDirection",invalid);turbinePort.runRequest("setDirection",invalid);
            exchangerPort.runRequest("setDirection",invalid);batteryPort.runRequest("setDirection",invalid);
        }
        coolant.runRequest("setDirection",null);turbinePort.runRequest("setDirection",null);
        exchangerPort.runRequest("setDirection",null);batteryPort.runRequest("setDirection",null);
        h.assertTrue(exchangerPort.isInlet(),"invalid command changed exchanger direction");
        h.succeed();
    }
}
