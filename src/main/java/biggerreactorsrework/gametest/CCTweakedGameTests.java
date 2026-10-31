package biggerreactorsrework.gametest;

import biggerreactorsrework.compat.ComputerPortState;
import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.turbine.blocks.*;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerComputerPortBlock;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;

/** Added 2026-10-31: native CC discovery, real Lua execution, server-thread callbacks and port lifetime. */
public final class CCTweakedGameTests {
    private static boolean installed() { return ModList.get().isLoaded("computercraft"); }
    private static void reactor(GameTestHelper h) {
        for(int x=1;x<=3;x++)for(int y=1;y<=3;y++)for(int z=1;z<=3;z++)
            if(x==1||x==3||y==1||y==3||z==1||z==3)h.setBlock(x,y,z,ReactorCasing.INSTANCE);
        h.setBlock(2,2,2,ReactorFuelRod.INSTANCE);h.setBlock(2,3,2,ReactorControlRod.INSTANCE);
        h.setBlock(1,2,2,ReactorTerminal.INSTANCE);h.setBlock(3,2,2,ReactorComputerPort.INSTANCE);
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=400)
    public static void nativeLuaReactorControlsAndSnapshots(GameTestHelper h) {
        if(!installed()){h.succeed();return;}
        reactor(h);
        var pos=new BlockPos(3,2,2);var computer=new BlockPos(4,2,2);
        h.startSequence().thenWaitUntil(()->Probe.assembled(h,pos)).thenExecute(()->Probe.boot(h,computer,"""
            local p=assert(peripheral.find('biggerreactorsrework_reactor'))
            assert(p.connected() and p.apiVersion()==1 and type(p.modVersion())=='string')
            p.setActive(true); assert(p.active()); p.setActive(false)
            assert(p.controlRodCount()==1); p.setControlRodLevel(0,42.5)
            assert(p.getControlRod(0).level==42.5)
            p.setControlRodName(0,'Rod Lua'); assert(p.getControlRod(0).name=='Rod Lua')
            p.setAllControlRodLevels(33); assert(p.getControlRod(0).level==33)
            assert(type(p.battery())=='table' and p.coolantTank()==nil)
            local fuel=p.fuelTank(); fuel.fuel=123456; assert(p.fuelTank().fuel~=123456)
            for _,name in ipairs({'fuelTemperature','stackTemperature','ambientTemperature'}) do assert(type(p[name]())=='number') end
            p.ejectWaste()
            assert(not pcall(p.setControlRodLevel,-1,20)); assert(not pcall(p.setControlRodLevel,0,101))
            assert(not pcall(p.setControlRodLevel,0,0/0)); assert(not pcall(p.setActive,'yes'))
            assert(not pcall(p.setControlRodName,0,string.rep('x',129))); assert(not pcall(p.dumpCoolant))
            """)).thenWaitUntil(()->Probe.passed(h,computer)).thenSucceed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=400)
    public static void nativeLuaTurbineControlsAndTelemetry(GameTestHelper h) {
        if(!installed()){h.succeed();return;}
        for(int x=1;x<=5;x++)for(int y=1;y<=5;y++)for(int z=1;z<=5;z++)
            if(x==1||x==5||y==1||y==5||z==1||z==5)h.setBlock(x,y,z,TurbineCasing.INSTANCE);
        h.setBlock(1,3,3,TurbineTerminal.INSTANCE);h.setBlock(5,3,3,TurbineComputerPort.INSTANCE);
        h.setBlock(3,1,3,TurbineRotorBearing.INSTANCE);h.setBlock(3,5,3,TurbineRotorBearing.INSTANCE);
        for(int y=2;y<=4;y++)h.setBlock(3,y,3,TurbineRotorShaft.INSTANCE);
        for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})h.setBlock(3+d[0],2,3+d[1],TurbineRotorBlade.INSTANCE);
        for(int x=2;x<=4;x++)for(int z=2;z<=4;z++)if(x!=3||z!=3)h.setBlock(x,4,z,Blocks.IRON_BLOCK);
        var pos=new BlockPos(5,3,3);var computer=new BlockPos(6,3,3);
        h.startSequence().thenWaitUntil(()->Probe.assembled(h,pos)).thenExecute(()->Probe.boot(h,computer,"""
            local p=assert(peripheral.find('biggerreactorsrework_turbine'))
            assert(p.connected()); p.setActive(true); assert(p.active()); p.setActive(false)
            p.setCoilEngaged(false); assert(not p.coilEngaged()); p.setCoilEngaged(true)
            p.setNominalFlowRate(1); assert(p.fluidTank().nominalFlowRate==1)
            for _,state in ipairs({'closed','overflow','all'}) do p.setVentState(state); assert(p.ventState()==state) end
            assert(type(p.rotor().RPM)=='number' and type(p.battery().capacity)=='number')
            assert(type(p.fluidTank().input)=='table' and type(p.fluidTank().output)=='table')
            for _,rate in ipairs({-1,0.5,math.huge,0/0,p.fluidTank().flowRateLimit+1}) do assert(not pcall(p.setNominalFlowRate,rate)) end
            assert(not pcall(p.setVentState,'invalid')); assert(not pcall(p.setCoilEngaged,1))
            """)).thenWaitUntil(()->Probe.passed(h,computer)).thenSucceed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=400)
    public static void nativeLuaLiquidReactorCoolantAndAssemblyLoss(GameTestHelper h) {
        if(!installed()){h.succeed();return;}
        GuideStructures.reactor(5,true).parts().forEach(h::setBlock);
        var pos=new BlockPos(4,1,2);h.setBlock(pos,ReactorComputerPort.INSTANCE);
        var computer=new BlockPos(5,1,2);
        h.startSequence().thenWaitUntil(()->Probe.assembled(h,pos)).thenExecute(()->Probe.boot(h,computer,"""
            local p=assert(peripheral.find('biggerreactorsrework_reactor'))
            assert(p.battery()==nil and type(p.coolantTank())=='table')
            assert(type(p.coolantTank().capacity)=='number'); p.dumpCoolant()
            os.setComputerLabel('cc-wait')
            while p.connected() do sleep(.05) end
            assert(not pcall(p.active)); assert(not pcall(p.setActive,true))
            """)).thenWaitUntil(()->Probe.label(h,computer,"cc-wait"))
            .thenExecute(()->h.setBlock(0,0,0,Blocks.AIR))
            .thenWaitUntil(()->Probe.passed(h,computer)).thenSucceed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=400)
    public static void nativeLuaWiredExchangerDiscoveryAndChannels(GameTestHelper h) {
        if(!installed()){h.succeed();return;}
        GuideStructures.exchanger().parts().forEach(h::setBlock);
        var pos=new BlockPos(2,1,0);h.setBlock(pos,HeatExchangerComputerPortBlock.INSTANCE);
        var modem=new BlockPos(2,1,-1);var computer=new BlockPos(2,1,-2);
        h.startSequence().thenWaitUntil(()->Probe.assembled(h,pos)).thenExecute(()->{
            Probe.modem(h,modem);
            Probe.boot(h,computer,"""
                local p
                for i=1,30 do p=peripheral.find('biggerreactorsrework_heat_exchanger'); if p then break end; sleep(.1) end
                assert(p and p.connected(),'wired exchanger not discovered')
                for _,name in ipairs({'condenser','evaporator'}) do
                  local t=p[name](); assert(type(t.temperature)=='number' and type(t.input)=='table' and type(t.output)=='table')
                  t.input.amount=123456; assert(p[name]().input.amount~=123456)
                end
                p.dumpCondenser(); p.dumpEvaporator(); assert(type(p.internalEnvironment().temperature)=='number')
                for _,name in ipairs({'ambientTemperature','ambientInternalRFKT','condenserInternalRFKT','evaporatorInternalRFKT','condenserEvaporatorRFKT'}) do assert(type(p[name]())=='number') end
                """);
        }).thenWaitUntil(()->Probe.passed(h,computer)).thenSucceed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=160)
    public static void peripheralLifetimeAndOptionalDataRetention(GameTestHelper h) {
        reactor(h);var pos=new BlockPos(3,2,2);
        h.startSequence().thenWaitUntil(()->{
            var tile=GameTestSupport.be(h,pos);
            h.assertTrue(tile instanceof biggerreactorsrework.multiblocks.reactor.tiles.ReactorComputerPortTile port &&
                    port.nullableController()!=null && port.controller().simulation()!=null,"reactor not assembled");
        }).thenExecute(()->{
            if(installed())Probe.lifetime(h,pos);
            else h.assertTrue(((ComputerPortState.Port)GameTestSupport.be(h,pos)).computerPortState().component()==null,"optional OC loaded without its mod");
        }).thenSucceed();
    }
    private static final class Probe {
        static dan200.computercraft.api.peripheral.IPeripheral peripheral(GameTestHelper h,BlockPos pos) {
            var p=h.getLevel().getCapability(dan200.computercraft.api.peripheral.PeripheralCapability.get(),h.absolutePos(pos),Direction.EAST);
            h.assertTrue(p!=null,"native peripheral capability missing");return p;
        }
        static Object call(Object p,String name,Object... values) throws Exception {
            try{return ((Object[])p.getClass().getMethod(name,dan200.computercraft.api.lua.IArguments.class)
                    .invoke(p,new dan200.computercraft.api.lua.ObjectArguments(values)))[0];}
            catch(java.lang.reflect.InvocationTargetException error){if(error.getCause() instanceof Exception cause)throw cause;throw error;}
        }
        static void assembled(GameTestHelper h,BlockPos pos) {
            Object connected;
            try{connected=call(peripheral(h,pos),"connected");}
            catch(Exception error){throw new AssertionError(error);}
            h.assertTrue(Boolean.TRUE.equals(connected),"machine not assembled");
        }
        static void boot(GameTestHelper h,BlockPos pos,String script) {
            h.setBlock(pos,BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("computercraft","computer_advanced")));
            try {
                var server=h.getLevel().getServer();
                int id=dan200.computercraft.api.ComputerCraftAPI.createUniqueNumberedSaveDir(server,"computer");
                var tile=GameTestSupport.be(h,pos);
                tile.getClass().getMethod("setComputerID",int.class).invoke(tile,id);
                // Install files before CC creates its cached root mount and starts the BIOS.
                var mount=dan200.computercraft.api.ComputerCraftAPI.createSaveDirMount(server,"computer/"+id,1000000);
                var wrapped="local ok,err=pcall(function()\n"+script+"\nend)\nos.setComputerLabel(ok and 'cc-pass' or ('FAIL:'..tostring(err)))\n";
                try(var channel=mount.openFile("startup.lua",java.util.Set.of(java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.WRITE,java.nio.file.StandardOpenOption.TRUNCATE_EXISTING))) {
                    var bytes=java.nio.ByteBuffer.wrap(wrapped.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    while(bytes.hasRemaining())channel.write(bytes);
                }
                Object computer=tile.getClass().getMethod("createServerComputer").invoke(tile);
                computer.getClass().getMethod("turnOn").invoke(computer);
            }catch(Exception error){throw new AssertionError("Could not boot native Lua computer",error);}
        }
        static void passed(GameTestHelper h,BlockPos pos) {
            label(h,pos,"cc-pass");
        }
        static void label(GameTestHelper h,BlockPos pos,String expected) {
            try {
                // The test server advances ticks without real-time pacing; let native Lua workers run too.
                java.util.concurrent.locks.LockSupport.parkNanos(5_000_000);
                var tile=GameTestSupport.be(h,pos);
                // GameTest structures may not have entity-ticking chunk status: tick the real CC block normally.
                tickBlock(tile);
                var computer=tile.getClass().getMethod("getServerComputer").invoke(tile);
                String label=computer==null?null:(String)computer.getClass().getMethod("getLabel").invoke(computer);
                h.assertTrue(expected.equals(label),"Lua computer status: "+label+", "+screen(computer));
            }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        }
        static String screen(Object computer) throws ReflectiveOperationException {
            if(computer==null)return "computer missing";
            var state=computer.getClass().getMethod("getTerminalState").invoke(computer);
            var terminal=state.getClass().getMethod("create").invoke(state);
            int height=(int)terminal.getClass().getMethod("getHeight").invoke(terminal);
            var text=new StringBuilder("state="+computer.getClass().getMethod("getState").invoke(computer)+"; ");
            for(int y=0;y<height;y++)text.append(terminal.getClass().getMethod("getLine",int.class).invoke(terminal,y)).append(" | ");
            return text.toString();
        }
        @SuppressWarnings({"rawtypes","unchecked"})
        static void tickBlock(net.minecraft.world.level.block.entity.BlockEntity tile) {
            var state=tile.getBlockState();
            var ticker=((net.minecraft.world.level.block.EntityBlock)state.getBlock()).getTicker(tile.getLevel(),state,tile.getType());
            if(ticker!=null)((net.minecraft.world.level.block.entity.BlockEntityTicker)ticker).tick(tile.getLevel(),tile.getBlockPos(),state,tile);
        }
        static void modem(GameTestHelper h,BlockPos pos) {
            h.setBlock(pos,BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("computercraft","wired_modem_full")));
            try{var tile=GameTestSupport.be(h,pos);tile.getClass().getMethod("use",net.minecraft.world.entity.player.Player.class).invoke(tile,h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE));}
            catch(Exception error){throw new AssertionError(error);}
        }
        static void lifetime(GameTestHelper h,BlockPos pos) {
            var p=peripheral(h,pos);var tile=GameTestSupport.be(h,pos);
            h.assertTrue(p.equals(peripheral(h,pos)),"same tile is not an equivalent peripheral");
            for(var method:p.getClass().getMethods())if(method.isAnnotationPresent(dan200.computercraft.api.lua.LuaFunction.class))
                h.assertTrue(method.getAnnotation(dan200.computercraft.api.lua.LuaFunction.class).mainThread(),"live callback is not scheduled on the main thread");
            var rejected=java.util.concurrent.CompletableFuture.supplyAsync(()->{
                try{call(p,"active");return false;}catch(Exception error){return error instanceof dan200.computercraft.api.lua.LuaException;}
            }).join();
            h.assertTrue(rejected,"off-thread callback was not rejected");
            var tag=tile.saveWithFullMetadata(h.getLevel().registryAccess());
            var opaque=new net.minecraft.nbt.CompoundTag();opaque.putString("address","retained-with-CC-only");
            var local=tag.getCompoundOrEmpty("local");local.put("computerComponent",opaque);tag.put("local",local);
            GameTestSupport.load(tile,tag,h);
            h.assertTrue(tile.saveWithFullMetadata(h.getLevel().registryAccess()).getCompoundOrEmpty("local").getCompoundOrEmpty("computerComponent").equals(opaque),"CC discarded dormant OpenComputers data");
            h.setBlock(pos,Blocks.AIR);
            try{h.assertTrue(Boolean.FALSE.equals(call(p,"connected")),"removed peripheral still connected");call(p,"active");throw new AssertionError("removed port accepted call");}
            catch(dan200.computercraft.api.lua.LuaException expected){}
            catch(Exception error){throw new AssertionError(error);}
        }
    }
}
