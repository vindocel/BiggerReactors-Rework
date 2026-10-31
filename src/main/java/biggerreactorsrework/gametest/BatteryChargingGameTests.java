package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.registry.OnModLoad;
import biggerreactorsrework.multiblocks.battery.*;
import biggerreactorsrework.multiblocks.battery.blocks.*;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryTerminalTile;
import biggerreactorsrework.multiblocks.battery.containers.BatteryTerminalContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.capabilities.*;
import biggerreactorsrework.core.transfer.EnergyStore;

/** Modified 2026-10-27: native item capabilities, real menus and the shared battery budget. Test-only registration. */
public final class BatteryChargingGameTests {
    @OnModLoad
    private static void register() {
        if(Boolean.getBoolean("biggerreactorsrework.testEnergyItems"))
            net.neoforged.fml.ModLoadingContext.get().getActiveContainer().getEventBus().addListener(BatteryChargingGameTests::capabilities);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Energy.ITEM,(stack,access)->new net.neoforged.neoforge.transfer.energy.EnergyHandler() {
            private int stored(){return access.getResource().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("testFE",0);}
            private boolean set(int value,net.neoforged.neoforge.transfer.transaction.TransactionContext transaction){
                var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("testFE",value);
                return access.exchange(access.getResource().with(DataComponents.CUSTOM_DATA,CustomData.of(tag)),1,transaction)==1;
            }
            public long getAmountAsLong(){return stored();}
            public long getCapacityAsLong(){return 10000;}
            public int insert(int max,net.neoforged.neoforge.transfer.transaction.TransactionContext transaction){int n=Math.min(max,10000-stored());return n>0&&set(stored()+n,transaction)?n:0;}
            public int extract(int max,net.neoforged.neoforge.transfer.transaction.TransactionContext transaction){int n=Math.min(max,stored());return n>0&&set(stored()-n,transaction)?n:0;}
        },Items.DIAMOND_HELMET,Items.SHIELD);
    }
    private static void set(Object target,String name,long value) {
        try {var f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.setLong(target,value);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=160)
    public static void openMenuChargesEquipmentAndSharesOutputBudget(GameTestHelper h) {
        for(int x=1;x<=3;x++)for(int y=1;y<=3;y++)for(int z=1;z<=3;z++)
            h.setBlock(x,y,z,x==2&&y==2&&z==2?BatteryCell.INSTANCE:BatteryCasing.INSTANCE);
        h.setBlock(1,2,2,BatteryTerminal.INSTANCE);
        h.setBlock(2,2,1,BatteryEnergyPort.INSTANCE);h.setBlock(2,2,3,BatteryEnergyPort.INSTANCE);
        h.startSequence().thenWaitUntil(()->h.assertTrue(((BatteryTerminalTile)GameTestSupport.be(h,new BlockPos(1,2,2))).isBatteryAssembled(),"battery not ready"))
          .thenExecute(()->{
            var tile=(BatteryTerminalTile)GameTestSupport.be(h,new BlockPos(1,2,2));var c=tile.controller();
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
            var pos=h.absolutePos(new BlockPos(2,2,2));player.setPos(pos.getX(),pos.getY(),pos.getZ());
            var helmet=new ItemStack(Items.DIAMOND_HELMET);var shield=new ItemStack(Items.SHIELD);
            player.setItemSlot(EquipmentSlot.HEAD,helmet);player.setItemSlot(EquipmentSlot.OFFHAND,shield);
            set(c,"stored",10000);set(c,"transferLimit",1000);set(c,"outputThisTick",0);
            var menu=new BatteryTerminalContainer(42,tile.getBlockPos(),player);player.containerMenu=menu;
            h.assertTrue(menu.slots.get(BatteryItemHandler.CHARGE_SLOT).y==23 && menu.slots.get(BatteryItemHandler.DISCHARGE_SLOT).y==55,"plus/minus positions incorrect");
            c.extract(400,false);menu.broadcastChanges();menu.broadcastChanges();
            h.assertTrue(BatteryItemHandler.energy(helmet).getEnergyStored()==600,"equipment exceeded shared budget or charged twice");
            h.assertTrue(BatteryItemHandler.energy(shield).getEnergyStored()==0,"offhand bypassed shared limit");
            player.containerMenu=player.inventoryMenu;set(c,"outputThisTick",0);menu.broadcastChanges();
            h.assertTrue(BatteryItemHandler.energy(helmet).getEnergyStored()==600,"closed menu charged equipment");
            var next=new BatteryTerminalContainer(43,tile.getBlockPos(),player);player.containerMenu=next;
            player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);next.broadcastChanges();
            h.assertTrue(BatteryItemHandler.energy(shield).getEnergyStored()==1000,"offhand did not charge");
            player.containerMenu=player.inventoryMenu;player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            // The logical slots keep their persisted IDs, but charging is the top plus slot.
            var charge=new ItemStack(Items.SHIELD);var discharge=new ItemStack(Items.DIAMOND_HELMET);
            BatteryItemHandler.energy(discharge).receiveEnergy(2000,false);
            c.items.setStackInSlot(BatteryItemHandler.CHARGE_SLOT,charge);
            c.items.setStackInSlot(BatteryItemHandler.DISCHARGE_SLOT,discharge);
            set(c,"inputThisTick",0);set(c,"outputThisTick",0);set(c,"stored",5000);
            try {var method=c.getClass().getDeclaredMethod("chargeItems");method.setAccessible(true);method.invoke(c);}
            catch(ReflectiveOperationException error){throw new AssertionError(error);}
            h.assertTrue(BatteryItemHandler.energy(charge).getEnergyStored()==1000,"plus slot failed to charge item");
            h.assertTrue(BatteryItemHandler.energy(discharge).getEnergyStored()==1000,"minus slot failed to drain item");
            set(c,"outputThisTick",0);set(c,"stored",0);
            h.assertTrue(c.chargeItem(new ItemStack(Items.SHIELD))==0,"empty battery created energy");
            h.assertTrue(c.chargeItem(new ItemStack(Items.STONE))==0,"non-FE item accepted energy");
            c.items.setStackInSlot(BatteryItemHandler.CHARGE_SLOT,ItemStack.EMPTY);
            c.items.setStackInSlot(BatteryItemHandler.DISCHARGE_SLOT,ItemStack.EMPTY);
          }).thenSucceed();
    }
}
