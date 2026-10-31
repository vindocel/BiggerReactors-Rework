package biggerreactorsrework.multiblocks.battery.containers;

import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.core.registry.ContainerSupplier;
import biggerreactorsrework.core.registry.RegisterContainer;
import biggerreactorsrework.multiblocks.battery.BatteryItemHandler;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryBaseTile;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import biggerreactorsrework.core.transfer.ItemSlots;
import biggerreactorsrework.core.transfer.SlotsSlot;

import javax.annotation.Nonnull;

/**
 * Battery terminal menu: the battery's two item slots, the player's inventory and the armor and offhand slots.
 */
@RegisterContainer(name = "battery_terminal")
public class BatteryTerminalContainer extends AbstractContainerMenu implements GuiSync.IGUIPacketProvider {

    @RegisterContainer.Type
    public static MenuType<BatteryTerminalContainer> INSTANCE;
    @RegisterContainer.Supplier
    public static final ContainerSupplier SUPPLIER = BatteryTerminalContainer::new;

    /**
     * Slot positions, shared with the screen.
     */
    public static final int WIDTH = 200;
    public static final int DISCHARGE_X = WIDTH - 25;
    public static final int DISCHARGE_Y = 55;
    public static final int CHARGE_X = WIDTH - 25;
    public static final int CHARGE_Y = 23;
    public static final int INVENTORY_X = (WIDTH - 9 * 18) / 2 + 1;
    public static final int INVENTORY_Y = 105;
    public static final int ARMOR_X = WIDTH + 6;
    public static final int ARMOR_Y = 9;
    public static final int OFFHAND_Y = ARMOR_Y + 4 * 18 + 4;

    private static final int BATTERY_SLOTS = BatteryItemHandler.SLOTS;
    private static final int PLAYER_SLOTS = 36;
    private static final int ARMOR_SLOTS = 4;

    private static final EquipmentSlot[] ARMOR_ORDER = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Identifier[] ARMOR_ICONS = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};

    private final BatteryBaseTile tileEntity;
    private final Player owner;
    private long chargedAtTick = Long.MIN_VALUE;

    public BatteryTerminalContainer(int windowId, BlockPos blockPos, Player player) {
        super(INSTANCE, windowId);
        this.owner = player;
        this.tileEntity = (BatteryBaseTile) player.level().getBlockEntity(blockPos);
        this.getGuiPacket();

        // The client only needs the placement rules; the menu sync fills its slots.
        ItemSlots items = !player.level().isClientSide() && tileEntity.nullableController() != null
                ? tileEntity.controller().items : new BatteryItemHandler(() -> {
        });
        this.addSlot(new SlotsSlot(items, BatteryItemHandler.DISCHARGE_SLOT, DISCHARGE_X, DISCHARGE_Y));
        this.addSlot(new SlotsSlot(items, BatteryItemHandler.CHARGE_SLOT, CHARGE_X, CHARGE_Y));

        Inventory inventory = player.getInventory();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, INVENTORY_Y + 58));
        }

        for (int i = 0; i < ARMOR_ORDER.length; i++) {
            EquipmentSlot slot = ARMOR_ORDER[i];
            this.addSlot(new ArmorSlot(inventory, player, slot, 36 + slot.getIndex(), ARMOR_X, ARMOR_Y + i * 18)
                    .setBackground(ARMOR_ICONS[i]));
        }
        this.addSlot(new Slot(inventory, 40, ARMOR_X, OFFHAND_Y) {
            @Override
            public void setByPlayer(@Nonnull ItemStack newStack, @Nonnull ItemStack oldStack) {
                player.onEquipItem(EquipmentSlot.OFFHAND, oldStack, newStack);
                super.setByPlayer(newStack, oldStack);
            }
        }.setBackground(InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));
    }

    /**
     * Worn armor slot with the player inventory's rules; vanilla's armor slot is not public.
     */
    private static final class ArmorSlot extends Slot {
        private final Player owner;
        private final EquipmentSlot equipmentSlot;
        
        ArmorSlot(Inventory inventory, Player owner, EquipmentSlot equipmentSlot, int index, int x, int y) {
            super(inventory, index, x, y);
            this.owner = owner;
            this.equipmentSlot = equipmentSlot;
        }
        
        @Override
        public void setByPlayer(@Nonnull ItemStack newStack, @Nonnull ItemStack oldStack) {
            owner.onEquipItem(equipmentSlot, oldStack, newStack);
            super.setByPlayer(newStack, oldStack);
        }
        
        @Override
        public int getMaxStackSize() {
            return 1;
        }
        
        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return stack.canEquip(equipmentSlot, owner);
        }
        
        @Override
        public boolean mayPickup(@Nonnull Player player) {
            ItemStack stack = this.getItem();
            return (stack.isEmpty() || player.isCreative()
                    || !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) && super.mayPickup(player);
        }
    }
    
    /**
     * @return The current state of the machine.
     */
    @Override
    public GuiSync.IGUIPacket getGuiPacket() {
        return this.tileEntity.batteryState();
    }

    /** Modified 2026-10-27: charge only the equipment displayed by this open, valid server menu. */
    @Override
    public void broadcastChanges() {
        long tick = biggerreactorsrework.core.CoreEvents.tickNumber();
        if (!owner.level().isClientSide() && owner.containerMenu == this && stillValid(owner) && chargedAtTick != tick) {
            chargedAtTick = tick;
            var controller = tileEntity.nullableController();
            if (controller != null) {
                long charged = 0;
                for (var slot : ARMOR_ORDER) charged += controller.chargeItem(owner.getItemBySlot(slot));
                charged += controller.chargeItem(owner.getItemBySlot(EquipmentSlot.OFFHAND));
                if (charged > 0) owner.getInventory().setChanged();
            }
        }
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        return !tileEntity.isRemoved() && tileEntity.isBatteryAssembled()
                && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(tileEntity.getBlockPos())) <= 64.0;
    }

    @Override
    public void executeRequest(String requestName, Object requestData) {
        assert tileEntity.getLevel() != null;
        if (tileEntity.getLevel().isClientSide()) {
            runRequest(requestName, requestData);
            return;
        }
        tileEntity.runRequest(requestName, requestData);
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = BATTERY_SLOTS;
        int hotbarStart = playerStart + 27;
        int playerEnd = playerStart + PLAYER_SLOTS;
        int armorStart = playerEnd;

        if (index < playerStart || index >= armorStart) {
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Battery slots first, then armor, then between the main inventory and the hotbar.
            boolean moved = this.moveItemStackTo(stack, 0, BATTERY_SLOTS, false)
                    || this.moveItemStackTo(stack, armorStart, armorStart + ARMOR_SLOTS, false);
            if (!moved) {
                moved = index < hotbarStart
                        ? this.moveItemStackTo(stack, hotbarStart, playerEnd, false)
                        : this.moveItemStackTo(stack, playerStart, hotbarStart, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public BlockPos machinePos() {
        return this.tileEntity.getBlockPos();
    }
}
