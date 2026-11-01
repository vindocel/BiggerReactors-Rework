package biggerreactorsrework.multiblocks.reactor.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import biggerreactorsrework.core.transfer.ItemSlots;
import biggerreactorsrework.Config;
import biggerreactorsrework.blocks.materials.MaterialBlock;
import biggerreactorsrework.items.ingots.BlutoniumIngot;
import biggerreactorsrework.items.ingots.CyaniteIngot;
import biggerreactorsrework.items.ingots.UraniumIngot;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorAccessPort;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorAccessPortContainer;
import biggerreactorsrework.multiblocks.reactor.state.ReactorAccessPortState;
import biggerreactorsrework.core.client.gui.api.IHasUpdatableState;
import biggerreactorsrework.core.debug.DebugInfo;
import biggerreactorsrework.core.multiblock.common.IEventMultiblock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.core.registry.CapabilityRegistration;
import biggerreactorsrework.core.registry.RegisterCapability;
import biggerreactorsrework.core.registry.RegisterTile;
import biggerreactorsrework.core.util.BlockStates;
import biggerreactorsrework.core.util.NonnullDefault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static biggerreactorsrework.multiblocks.reactor.blocks.ReactorAccessPort.PortDirection.*;

@NonnullDefault
public class ReactorAccessPortTile extends ReactorBaseTile implements ItemSlots, biggerreactorsrework.core.transfer.TransferSnapshots, MenuProvider, IHasUpdatableState<ReactorAccessPortState>, IEventMultiblock.AssemblyStateTransition {
    // Modified 2026-10-30: refresh cached neighbors when their capability is invalidated.
    private final biggerreactorsrework.core.capability.NeighborCapability<ItemSlots, ?> outputCapability =
            biggerreactorsrework.core.capability.NeighborCapability.items(this);
    
    @RegisterTile("reactor_access_port")
    public static final BlockEntityType.BlockEntitySupplier<ReactorAccessPortTile> SUPPLIER = new RegisterTile.Producer<>(ReactorAccessPortTile::new);
    
    @RegisterCapability
    private static final CapabilityRegistration ITEM_HANDLER_CAP_REGISTRATION = CapabilityRegistration.tileItems(ReactorAccessPortTile.class);
    
    private static final TagKey<Item> uraniumIngotTag = TagKey.create(BuiltInRegistries.ITEM.key(), Identifier.parse("c:ingots/uranium"));
    private static final TagKey<Item> uraniumBlockTag = TagKey.create(BuiltInRegistries.ITEM.key(), Identifier.parse("c:storage_blocks/uranium"));
    
    public static final int FUEL_SLOT = 0;
    public static final int WASTE_SLOT = 1;
    public static final int FUEL_INSERT_SLOT = 2;
    
    public ReactorAccessPortTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    private ReactorAccessPort.PortDirection direction = INLET;
    private boolean fuelMode = false;
    
    public boolean isInlet() {
        return direction == INLET;
    }
    
    public void setDirection(ReactorAccessPort.PortDirection direction) {
        this.direction = direction;
        this.setChanged();
    }
    
    @Override
    protected void readNBT(CompoundTag compound) {
        if (compound.contains("direction")) {
            direction = biggerreactorsrework.core.util.SavedValues.enumValue(ReactorAccessPort.PortDirection.class, compound.getStringOr("direction", ""), ReactorAccessPort.PortDirection.INLET);
        }
        if (compound.contains("fuelMode")) {
            fuelMode = compound.getBooleanOr("fuelMode", false);
        }
    }
    
    @Override
    protected CompoundTag writeNBT() {
        CompoundTag NBT = new CompoundTag();
        NBT.putString("direction", String.valueOf(direction));
        NBT.putBoolean("fuelMode", fuelMode);
        return NBT;
    }
    
    @Nonnull
    @Override
    public DebugInfo getDebugInfo() {
        return super.getDebugInfo().add("PortDirection: " + direction);
    }
    
    @Override
    public void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
        if (newState != IValidatedMultiblock.AssemblyState.DISASSEMBLED) {
            assert level != null;
            level.setBlock(worldPosition, level.getBlockState(worldPosition).setValue(PORT_DIRECTION_ENUM_PROPERTY, direction), 3);
            itemOutputDirection = getBlockState().getValue(BlockStates.FACING);
        } else {
            itemOutputDirection = null;
        }
        neighborChanged();
    }
    
    @Override
    public Object transferSnapshot() {
        var reactorSim = nullableController() == null ? null : controller().simulation();
        return reactorSim == null ? null : new long[]{reactorSim.fuelTank().fuel(), reactorSim.fuelTank().waste()};
    }

    @Override
    public void restoreTransferSnapshot(Object snapshot) {
        var reactorSim = nullableController() == null ? null : controller().simulation();
        if (reactorSim != null && snapshot instanceof long[] stock) {
            reactorSim.fuelTank().restoreStored(stock[0], stock[1]);
        }
    }

    @Override
    public int getSlots() {
        return 3;
    }
    
    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (nullableController() == null) {
            return ItemStack.EMPTY;
        }
        @Nullable var reactorSim = controller().simulation();
        if(reactorSim == null){
            return ItemStack.EMPTY;
        }
        if (slot == WASTE_SLOT) {
            long availableIngots = reactorSim.fuelTank().waste() / Config.CONFIG.Reactor.FuelMBPerIngot;
            return new ItemStack(CyaniteIngot.INSTANCE, (int) availableIngots);
        } else if (slot == FUEL_SLOT) {
            long availableIngots = reactorSim.fuelTank().fuel() / Config.CONFIG.Reactor.FuelMBPerIngot;
            return new ItemStack(UraniumIngot.INSTANCE, (int) availableIngots);
        } else {
            return ItemStack.EMPTY;
        }
    }
    
    @Nonnull
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isInlet() || nullableController() == null || slot != FUEL_INSERT_SLOT) {
            return stack;
        }
        stack = stack.copy();
        // Modified 2026-11-01: whole configurable fuel items only; division before multiplication prevents overflow.
        long perItem = biggerreactorsrework.registries.ReactorFuelRegistry.amount(stack);
        if (perItem > 0) {
            long available = controller().refuel(Long.MAX_VALUE, true);
            int count = (int) Math.min(stack.getCount(), available / perItem);
            if (count > 0) {
                long accepted = controller().refuel(count * perItem, simulate);
                stack.shrink((int) (accepted / perItem));
            }
        }
        return stack;
    }
    
    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (isInlet() || nullableController() == null || slot == FUEL_INSERT_SLOT) {
            return ItemStack.EMPTY;
        }
        
        if (slot == WASTE_SLOT && !fuelMode) {
            long maxExtractable = controller().extractWaste(amount * Config.CONFIG.Reactor.FuelMBPerIngot, true);
            long toExtracted = maxExtractable - (maxExtractable % Config.CONFIG.Reactor.FuelMBPerIngot);
            long extracted = controller().extractWaste(toExtracted, simulate);
            
            return new ItemStack(CyaniteIngot.INSTANCE, (int) Math.min(amount, extracted / Config.CONFIG.Reactor.FuelMBPerIngot));
        } else if (slot == FUEL_SLOT && fuelMode) {
            long maxExtractable = controller().extractFuel(amount * Config.CONFIG.Reactor.FuelMBPerIngot, true);
            long toExtracted = maxExtractable - (maxExtractable % Config.CONFIG.Reactor.FuelMBPerIngot);
            long extracted = controller().extractFuel(toExtracted, simulate);
            
            return new ItemStack(UraniumIngot.INSTANCE, (int) Math.min(amount, extracted / Config.CONFIG.Reactor.FuelMBPerIngot));
        }
        
        return ItemStack.EMPTY;
    }
    
    @Override
    public int getSlotLimit(int slot) {
        if (nullableController() == null) {
            return 0;
        }
        var reactorSim = controller().simulation();
        if(reactorSim == null){
            return 0;
        }
        return (int) (reactorSim.fuelTank().capacity() / Config.CONFIG.Reactor.FuelMBPerIngot);
    }
    
    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == FUEL_INSERT_SLOT) {
            return biggerreactorsrework.registries.ReactorFuelRegistry.amount(stack) > 0;
        } else if (slot == FUEL_SLOT) {
            return stack.getItem() == UraniumIngot.INSTANCE;
        } else {
            return stack.getItem() == CyaniteIngot.INSTANCE;
        }
    }
    
    public int pushWaste(int waste, boolean simulated) {
        neighborChanged();
        if (itemOutput != null) {
            waste /= Config.CONFIG.Reactor.FuelMBPerIngot;
            int wasteHandled = 0;
            for (int i = 0; i < itemOutput.getSlots(); i++) {
                if (waste == 0) {
                    break;
                }
                ItemStack toInsertStack = new ItemStack(CyaniteIngot.INSTANCE, waste);
                ItemStack remainingStack = itemOutput.insertItem(i, toInsertStack, simulated);
                wasteHandled += toInsertStack.getCount() - remainingStack.getCount();
                waste -= toInsertStack.getCount() - remainingStack.getCount();
            }
            return (int) (wasteHandled * Config.CONFIG.Reactor.FuelMBPerIngot);
        }
        return 0;
    }
    
    public void ejectWaste() {
        controller().extractWaste(pushWaste((int) controller().extractWaste(Integer.MAX_VALUE, true), false), false);
    }
    
    public int pushFuel(int fuel, boolean simulated) {
        neighborChanged();
        if (itemOutput != null) {
            fuel /= Config.CONFIG.Reactor.FuelMBPerIngot;
            int fuelHandled = 0;
            for (int i = 0; i < itemOutput.getSlots(); i++) {
                if (fuel == 0) {
                    break;
                }
                ItemStack toInsertStack = new ItemStack(UraniumIngot.INSTANCE, fuel);
                ItemStack remainingStack = itemOutput.insertItem(i, toInsertStack, simulated);
                fuelHandled += toInsertStack.getCount() - remainingStack.getCount();
                fuel -= toInsertStack.getCount() - remainingStack.getCount();
            }
            return (int) (fuelHandled * Config.CONFIG.Reactor.FuelMBPerIngot);
        }
        return 0;
    }
    
    public void ejectFuel() {
        controller().extractFuel(pushFuel((int) controller().extractFuel(Integer.MAX_VALUE, true), false), false);
    }
    
    @Nullable
    Direction itemOutputDirection;
    boolean connected;
    @Nullable
    ItemSlots itemOutput = null;
    public final ReactorAccessPortState reactorAccessPortState = new ReactorAccessPortState(this);
    
    @SuppressWarnings("DuplicatedCode")
    public void neighborChanged() {
        if (itemOutputDirection == null) {
            connected = false;
            itemOutput = null;
            return;
        }
        assert level != null;
        itemOutput = outputCapability.get(itemOutputDirection);
        connected = itemOutput != null;
    }
    
    @Override
    public void runRequest(String requestName, Object requestData) {
        if (nullableController() == null) {
            return;
        }
        
        // Change IO direction.
        if (requestName.equals("setDirection")) {
            // Modified 2026-10-30: reject invalid command types and domains before mutation.
            if (!(requestData instanceof Integer value) || value < 0 || value > 1) return;
            this.setDirection(((Integer) requestData != 0) ? OUTLET : INLET);
            assert level != null;
            level.setBlockAndUpdate(this.worldPosition, this.getBlockState().setValue(PORT_DIRECTION_ENUM_PROPERTY, direction));
            return;
        }
        
        // Change fuel/waste ejection.
        if (requestName.equals("setFuelMode")) {
            if (!(requestData instanceof Integer value) || value < 0 || value > 1) return;
            this.fuelMode = ((Integer) requestData != 0);
            this.setChanged();
            return;
        }
        
        if (requestName.equals("ejectWaste")) {
            if (fuelMode) {
                ejectFuel();
            } else {
                ejectWaste();
            }
            return;
        }
        
        super.runRequest(requestName, requestData);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable(ReactorAccessPort.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new ReactorAccessPortContainer(windowId, this.worldPosition, player);
    }
    
    @Override
    public ReactorAccessPortState getState() {
        this.updateState();
        return this.reactorAccessPortState;
    }
    
    @Override
    public void updateState() {
        reactorAccessPortState.direction = (this.direction == INLET);
        reactorAccessPortState.fuelMode = this.fuelMode;
    }
}
