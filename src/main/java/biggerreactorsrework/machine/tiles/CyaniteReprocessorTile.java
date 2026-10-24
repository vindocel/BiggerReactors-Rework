package biggerreactorsrework.machine.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.Config;
import biggerreactorsrework.api.WorkHandler;
import biggerreactorsrework.machine.blocks.CyaniteReprocessor;
import biggerreactorsrework.machine.containers.CyaniteReprocessorContainer;
import biggerreactorsrework.machine.side.RelativeFace;
import biggerreactorsrework.machine.side.SideConfiguration;
import biggerreactorsrework.machine.side.SideRoute;
import biggerreactorsrework.machine.side.SideRules;
import biggerreactorsrework.machine.side.Transmission;
import biggerreactorsrework.machine.state.CyaniteReprocessorState;
import biggerreactorsrework.machine.tiles.impl.CyaniteReprocessorItemHandler;
import biggerreactorsrework.items.ingots.BlutoniumIngot;
import biggerreactorsrework.core.client.gui.api.IHasUpdatableState;
import biggerreactorsrework.core.debug.DebugTool;
import biggerreactorsrework.core.registry.CapabilityRegistration;
import biggerreactorsrework.core.registry.RegisterCapability;
import biggerreactorsrework.core.registry.RegisterTile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Turns 2 cyanite ingots into 1 blutonium ingot, using water and energy while it works.
 * Each face is configured per material (items, water, energy) and can pull and push items and water by itself.
 */
public class CyaniteReprocessorTile extends BlockEntity implements MenuProvider, IHasUpdatableState<CyaniteReprocessorState> {

    @RegisterTile("cyanite_reprocessor")
    public static final BlockEntityType.BlockEntitySupplier<CyaniteReprocessorTile> SUPPLIER = new RegisterTile.Producer<>(CyaniteReprocessorTile::new);

    @RegisterCapability
    private static final CapabilityRegistration ITEM_HANDLER_CAP_REGISTRATION = CapabilityRegistration.tileCap(Capabilities.ItemHandler.BLOCK, CyaniteReprocessorTile.class, (CyaniteReprocessorTile tile, @Nullable Direction side) -> tile.itemHandlerFor(side));
    @RegisterCapability
    private static final CapabilityRegistration ENERGY_CAP_REGISTRATION = CapabilityRegistration.tileCap(Capabilities.EnergyStorage.BLOCK, CyaniteReprocessorTile.class, (CyaniteReprocessorTile tile, @Nullable Direction side) -> tile.energyFor(side));
    @RegisterCapability
    private static final CapabilityRegistration FLUID_HANDLER_CAP_REGISTRATION = CapabilityRegistration.tileCap(Capabilities.FluidHandler.BLOCK, CyaniteReprocessorTile.class, (CyaniteReprocessorTile tile, @Nullable Direction side) -> tile.fluidFor(side));

    /**
     * Cyanite ingots consumed per blutonium ingot produced.
     */
    private static final int CYANITE_PER_INGOT = 2;

    /**
     * The (mostly) current state of the machine.
     */
    public final CyaniteReprocessorState cyaniteReprocessorState = new CyaniteReprocessorState(this);
    /**
     * The work handler.
     */
    private WorkHandler workHandler = new WorkHandler(Config.CONFIG.CyaniteReprocessor.TotalWorkTime);
    /**
     * The item handler.
     */
    private final CyaniteReprocessorItemHandler itemHandler = new CyaniteReprocessorItemHandler(this::setChanged);
    /**
     * The energy storage. Other blocks can only insert energy; the machine consumes it while working.
     */
    private final Battery energyStorage = new Battery();
    /**
     * The water tank.
     */
    private final FluidTank fluidTank = new FluidTank(Config.CONFIG.CyaniteReprocessor.WaterTankCapacity, fluid -> fluid.getFluid() == Fluids.WATER) {
        @Override
        protected void onContentsChanged() {
            CyaniteReprocessorTile.this.setChanged();
        }
    };
    /**
     * View of the water tank for water input sides: it only fills.
     */
    private final IFluidHandler waterInput = new IFluidHandler() {
        @Override
        public int getTanks() {
            return fluidTank.getTanks();
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            return fluidTank.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return fluidTank.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return fluidTank.isFluidValid(tank, stack);
        }

        @Override
        public int fill(@Nonnull FluidStack resource, @Nonnull FluidAction action) {
            return fluidTank.fill(resource, action);
        }

        @Nonnull
        @Override
        public FluidStack drain(@Nonnull FluidStack resource, @Nonnull FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, @Nonnull FluidAction action) {
            return FluidStack.EMPTY;
        }
    };
    /**
     * Which faces take or give each material.
     */
    private final SideConfiguration sides = new SideConfiguration(SideRules.CYANITE_REPROCESSOR);
    /**
     * Automation: pull cyanite from item input sides, push blutonium through item output sides, pull water from water
     * input sides.
     */
    private boolean autoInput = false;
    private boolean autoEject = false;
    private boolean autoInputFluid = false;
    /**
     * "Anti-cheat" item stack, to ensure players don't swap items mid-process.
     *
     * @see CyaniteReprocessorTile#tick()
     */
    private ItemStack itemPresentLastTick = ItemStack.EMPTY;

    public CyaniteReprocessorTile(BlockEntityType<CyaniteReprocessorTile> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        this.updateState();
    }

    /**
     * Energy storage that only accepts energy from outside, with the configured transfer rate.
     */
    private static final class Battery extends EnergyStorage {
        Battery() {
            super(Config.CONFIG.CyaniteReprocessor.EnergyTankCapacity, Config.CONFIG.CyaniteReprocessor.TransferRate, 0);
        }

        boolean consume(int amount) {
            if (this.energy < amount) {
                return false;
            }
            this.energy -= amount;
            return true;
        }

        void set(int energy) {
            this.energy = Mth.clamp(energy, 0, this.capacity);
        }
    }

    // ---------------------------------------------------------------- sides

    private Direction facing() {
        BlockState state = this.getBlockState();
        return state.hasProperty(CyaniteReprocessor.FACING) ? state.getValue(CyaniteReprocessor.FACING) : Direction.NORTH;
    }

    public SideRoute route(RelativeFace face, Transmission transmission) {
        return this.sides.get(face, transmission);
    }

    private SideRoute route(Direction side, Transmission transmission) {
        return this.route(RelativeFace.of(this.facing(), side), transmission);
    }

    /**
     * Item handler of a world side, following its item route. The {@code null} side sees both slots (information tools).
     */
    @Nullable
    public IItemHandler itemHandlerFor(@Nullable Direction side) {
        if (side == null) {
            return this.itemHandler.pipeHandler();
        }
        SideRoute route = this.route(side, Transmission.ITEM);
        return this.itemHandler.sideHandler(route.insertsItems(), route.extractsItems());
    }

    /**
     * Water input on the configured faces; the {@code null} side sees the tank.
     */
    @Nullable
    public IFluidHandler fluidFor(@Nullable Direction side) {
        if (side == null) {
            return this.fluidTank;
        }
        return this.route(side, Transmission.FLUID) == SideRoute.FLUID_INPUT ? this.waterInput : null;
    }

    /**
     * Energy input on the configured faces; the {@code null} side sees the storage, which never gives energy back.
     */
    @Nullable
    public IEnergyStorage energyFor(@Nullable Direction side) {
        if (side == null || this.route(side, Transmission.ENERGY) == SideRoute.ENERGY_INPUT) {
            return this.energyStorage;
        }
        return null;
    }

    /**
     * Tells neighbors that the capabilities of the sides changed.
     */
    private void sidesChanged() {
        this.setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
        }
    }

    /**
     * Applies a side configuration request from the screen. The data comes from the client: it is validated here.
     */
    public void runRequest(String requestName, @Nullable Object requestData) {
        int data = requestData instanceof Integer value ? value : -1;
        switch (requestName) {
            // face * 8 + transmission * 2 + (forward ? 0 : 1)
            case "sideCycle" -> {
                int face = data / 8;
                int transmission = (data % 8) / 2;
                if (data >= 0 && face < RelativeFace.VALUES.length && transmission < Transmission.VALUES.length
                        && this.sides.cycle(RelativeFace.VALUES[face], Transmission.VALUES[transmission], data % 2 == 0)) {
                    this.sidesChanged();
                }
            }
            // transmission * 2 + (forward ? 0 : 1)
            case "sideBatch" -> {
                if (data >= 0 && data / 2 < Transmission.VALUES.length) {
                    Transmission transmission = Transmission.VALUES[data / 2];
                    SideRoute target = this.sides.batchTarget(transmission, data % 2 == 0);
                    if (target == SideRoute.NONE ? this.sides.clear(transmission) : this.sides.applyAll(transmission, target)) {
                        this.sidesChanged();
                    }
                }
            }
            case "sideClearAll" -> {
                boolean changed = false;
                for (Transmission transmission : Transmission.VALUES) {
                    changed |= this.sides.clear(transmission);
                }
                if (changed) {
                    this.sidesChanged();
                }
            }
            case "autoInput" -> {
                this.autoInput = !this.autoInput;
                this.setChanged();
            }
            case "autoEject" -> {
                this.autoEject = !this.autoEject;
                this.setChanged();
            }
            case "autoInputFluid" -> {
                this.autoInputFluid = !this.autoInputFluid;
                this.setChanged();
            }
            default -> {
            }
        }
        this.updateState();
    }

    /**
     * Neighbor capability on a world side, if that block is loaded.
     */
    @Nullable
    private <T> T neighbor(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> capability, Direction side) {
        BlockPos target = this.worldPosition.relative(side);
        if (this.level == null || !this.level.isLoaded(target)) {
            return null;
        }
        return this.level.getCapability(capability, target, side.getOpposite());
    }

    /**
     * Pulls cyanite from the blocks on the item input sides.
     */
    private void autoInputCycle() {
        for (RelativeFace face : RelativeFace.VALUES) {
            if (!this.route(face, Transmission.ITEM).insertsItems()) {
                continue;
            }
            IItemHandler source = this.neighbor(Capabilities.ItemHandler.BLOCK, face.toDirection(this.facing()));
            if (source == null) {
                continue;
            }
            for (int slot = 0; slot < source.getSlots(); slot++) {
                ItemStack available = source.extractItem(slot, Integer.MAX_VALUE, true);
                if (available.isEmpty() || !this.itemHandler.isItemValid(CyaniteReprocessorItemHandler.INPUT_SLOT_INDEX, available)) {
                    continue;
                }
                ItemStack remainder = this.itemHandler.insertItem(CyaniteReprocessorItemHandler.INPUT_SLOT_INDEX, available, true);
                int moved = available.getCount() - remainder.getCount();
                if (moved <= 0) {
                    return;
                }
                this.itemHandler.insertItem(CyaniteReprocessorItemHandler.INPUT_SLOT_INDEX, source.extractItem(slot, moved, false), false);
            }
        }
    }

    /**
     * Pushes blutonium into the blocks on the item output sides.
     */
    private void autoEjectCycle() {
        for (RelativeFace face : RelativeFace.VALUES) {
            if (this.itemHandler.getStackInSlot(CyaniteReprocessorItemHandler.OUTPUT_SLOT_INDEX).isEmpty()) {
                return;
            }
            if (!this.route(face, Transmission.ITEM).extractsItems()) {
                continue;
            }
            IItemHandler destination = this.neighbor(Capabilities.ItemHandler.BLOCK, face.toDirection(this.facing()));
            if (destination == null) {
                continue;
            }
            ItemStack available = this.itemHandler.extractItem(CyaniteReprocessorItemHandler.OUTPUT_SLOT_INDEX, Integer.MAX_VALUE, true);
            int moved = available.getCount() - ItemHandlerHelper.insertItem(destination, available, true).getCount();
            if (moved > 0) {
                ItemHandlerHelper.insertItem(destination, this.itemHandler.extractItem(CyaniteReprocessorItemHandler.OUTPUT_SLOT_INDEX, moved, false), false);
            }
        }
    }

    /**
     * Pulls water from the blocks on the water input sides, at the configured transfer rate.
     */
    private void autoInputFluidCycle() {
        for (RelativeFace face : RelativeFace.VALUES) {
            int space = Math.min(this.fluidTank.getSpace(), Config.CONFIG.CyaniteReprocessor.TransferRate);
            if (space <= 0) {
                return;
            }
            if (this.route(face, Transmission.FLUID) != SideRoute.FLUID_INPUT) {
                continue;
            }
            IFluidHandler source = this.neighbor(Capabilities.FluidHandler.BLOCK, face.toDirection(this.facing()));
            if (source != null) {
                FluidUtil.tryFluidTransfer(this.fluidTank, source, new FluidStack(Fluids.WATER, space), true);
            }
        }
    }

    /**
     * Handle right-clicks holding the debug tool or a water bucket.
     *
     * @return {@link ItemInteractionResult#PASS_TO_DEFAULT_BLOCK_INTERACTION} for other items, so the screen opens.
     */
    @Nonnull
    public ItemInteractionResult onItemUse(ItemStack stack, Level level, Player player, InteractionHand hand) {
        // Print tile data.
        if (stack.is(DebugTool.INSTANCE)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.literal(String.format("[%s] Progress: %s/%s", BiggerReactors.modid, this.workHandler.getProgress(), this.workHandler.getGoal())));
                player.sendSystemMessage(Component.literal(String.format("[%s] Energy: %s/%s FE", BiggerReactors.modid, this.energyStorage.getEnergyStored(), this.energyStorage.getMaxEnergyStored())));
                player.sendSystemMessage(Component.literal(String.format("[%s] Fluid Tank: %s/%s mB", BiggerReactors.modid, this.fluidTank.getFluidAmount(), this.fluidTank.getCapacity())));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Do water bucket check.
        if (stack.is(Items.WATER_BUCKET)) {
            if (!level.isClientSide && this.fluidTank.getSpace() >= 1000) {
                this.fluidTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
                if (!player.hasInfiniteMaterials()) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Drop items on destruction.
     */
    public void dropContents(Level level, BlockPos blockPos) {
        for (int slot = 0; slot < this.itemHandler.getSlots(); slot++) {
            ItemStack stack = this.itemHandler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), stack.copy());
                this.itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Nonnull
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.biggerreactorsrework.cyanite_reprocessor");
    }

    /**
     * Create a GUI container for the tile.
     *
     * @param windowId        The window ID to use.
     * @param playerInventory The player's inventory.
     * @return A GUI container to render.
     */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @Nonnull Inventory playerInventory, @Nonnull Player player) {
        return new CyaniteReprocessorContainer(windowId, this.getBlockPos(), player);
    }

    /**
     * @return The current state of the tile.
     */
    @Override
    @Nonnull
    public CyaniteReprocessorState getState() {
        this.updateState();
        return this.cyaniteReprocessorState;
    }

    /**
     * Call for an update to the current state information.
     */
    @Override
    public void updateState() {
        this.cyaniteReprocessorState.workTime = this.workHandler.getProgress();
        this.cyaniteReprocessorState.workTimeTotal = this.workHandler.getGoal();
        this.cyaniteReprocessorState.energyStored = this.energyStorage.getEnergyStored();
        this.cyaniteReprocessorState.energyCapacity = this.energyStorage.getMaxEnergyStored();
        this.cyaniteReprocessorState.waterStored = this.fluidTank.getFluidAmount();
        this.cyaniteReprocessorState.waterCapacity = this.fluidTank.getCapacity();
        this.cyaniteReprocessorState.itemSides = this.sides.pack(Transmission.ITEM);
        this.cyaniteReprocessorState.fluidSides = this.sides.pack(Transmission.FLUID);
        this.cyaniteReprocessorState.energySides = this.sides.pack(Transmission.ENERGY);
        this.cyaniteReprocessorState.autoInput = this.autoInput;
        this.cyaniteReprocessorState.autoEject = this.autoEject;
        this.cyaniteReprocessorState.autoInputFluid = this.autoInputFluid;
    }

    /**
     * Checks if the player can currently use this tile.
     *
     * @param player The player to check.
     * @return True if usable, false otherwise.
     */
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.getBlockPos()) != this) {
            return false;
        }
        return player.distanceToSqr(
                (double) this.getBlockPos().getX() + 0.5D,
                (double) this.getBlockPos().getY() + 0.5D,
                (double) this.getBlockPos().getZ() + 0.5D) <= 64.0D;
    }

    /**
     * Read NBT data from the world.
     */
    @Override
    protected void loadAdditional(@Nonnull CompoundTag parentCompound, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(parentCompound, registries);
        CompoundTag childCompound = parentCompound.getCompound("cyaniteReprocessorState");

        // Read work.
        this.workHandler = new WorkHandler(Config.CONFIG.CyaniteReprocessor.TotalWorkTime, Math.max(0, childCompound.getInt("workTime")));
        // Read items.
        this.itemHandler.deserializeNBT(registries, childCompound.getCompound("inventory"));
        if (this.itemHandler.getSlots() != CyaniteReprocessorItemHandler.SLOTS) {
            this.itemHandler.setSize(CyaniteReprocessorItemHandler.SLOTS);
        }
        // Read energy.
        this.energyStorage.set(childCompound.getInt("energyStored"));
        // Read fluids.
        this.fluidTank.readFromNBT(registries, childCompound.getCompound("fluidTank"));
        this.itemPresentLastTick = this.itemHandler.getStackInSlot(CyaniteReprocessorItemHandler.INPUT_SLOT_INDEX).copy();
        // Read sides and automation.
        this.sides.load(childCompound.getCompound("sides"));
        this.autoInput = childCompound.getBoolean("autoInput");
        this.autoEject = childCompound.getBoolean("autoEject");
        this.autoInputFluid = childCompound.getBoolean("autoInputFluid");
        this.updateState();
    }

    /**
     * Save NBT data to the world.
     */
    @Override
    protected void saveAdditional(@Nonnull CompoundTag parentCompound, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(parentCompound, registries);
        CompoundTag childCompound = new CompoundTag();

        // Write work.
        childCompound.putInt("workTime", this.workHandler.getProgress());
        // Write items.
        childCompound.put("inventory", this.itemHandler.serializeNBT(registries));
        // Write energy.
        childCompound.putInt("energyStored", this.energyStorage.getEnergyStored());
        // Write fluids.
        childCompound.put("fluidTank", this.fluidTank.writeToNBT(registries, new CompoundTag()));
        // Write sides and automation.
        childCompound.put("sides", this.sides.save());
        childCompound.putBoolean("autoInput", this.autoInput);
        childCompound.putBoolean("autoEject", this.autoEject);
        childCompound.putBoolean("autoInputFluid", this.autoInputFluid);

        parentCompound.put("cyaniteReprocessorState", childCompound);
    }

    /**
     * @return Whether or not the machine can perform work.
     * @see CyaniteReprocessorTile#tick()
     */
    private boolean canWork() {
        // If the output slot is full, we cannot work.
        ItemStack outputStack = this.itemHandler.getStackInSlot(CyaniteReprocessorItemHandler.OUTPUT_SLOT_INDEX);
        if (outputStack.getCount() >= this.itemHandler.getSlotLimit(CyaniteReprocessorItemHandler.OUTPUT_SLOT_INDEX)
                || outputStack.getCount() >= outputStack.getMaxStackSize()) {
            return false;
        }
        return (this.energyStorage.getEnergyStored() >= Config.CONFIG.CyaniteReprocessor.EnergyConsumptionPerTick
                && this.fluidTank.getFluidAmount() >= Config.CONFIG.CyaniteReprocessor.WaterConsumptionPerTick);
    }

    /**
     * Do work (if possible). Server side only.
     */
    public void tick() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        if (this.autoInput) {
            this.autoInputCycle();
        }
        if (this.autoInputFluid) {
            this.autoInputFluidCycle();
        }

        boolean doUpdate = false;
        boolean isActive = false;
        ItemStack inputStack = this.itemHandler.getStackInSlot(CyaniteReprocessorItemHandler.INPUT_SLOT_INDEX);

        // Check to make sure the player doesn't try to pull a fast one.
        if (!ItemStack.isSameItem(this.itemPresentLastTick, inputStack)) {
            this.workHandler.clear();
        }
        this.itemPresentLastTick = inputStack.copy();

        if (inputStack.getCount() >= CYANITE_PER_INGOT) {
            // Can we continue?
            if (canWork()) {
                isActive = true;
                doUpdate = true;
                this.workHandler.increment(1);
                this.energyStorage.consume(Config.CONFIG.CyaniteReprocessor.EnergyConsumptionPerTick);
                this.fluidTank.drain(Config.CONFIG.CyaniteReprocessor.WaterConsumptionPerTick, IFluidHandler.FluidAction.EXECUTE);
            } else if (this.workHandler.getProgress() > 0) {
                // We've run out of resources, lose progress.
                this.workHandler.decrement(Math.min(2, this.workHandler.getProgress()));
                doUpdate = true;
            }

            // Item is done.
            if (this.workHandler.isFinished()) {
                this.itemHandler.extractItem(CyaniteReprocessorItemHandler.INPUT_SLOT_INDEX, CYANITE_PER_INGOT, false);
                this.itemHandler.insertItem(CyaniteReprocessorItemHandler.OUTPUT_SLOT_INDEX, new ItemStack(BlutoniumIngot.INSTANCE, 1), false);
                this.workHandler.clear();
                this.itemPresentLastTick = this.itemHandler.getStackInSlot(CyaniteReprocessorItemHandler.INPUT_SLOT_INDEX).copy();
            }
        }

        BlockState currentBlockState = this.level.getBlockState(this.getBlockPos());
        if (currentBlockState.hasProperty(CyaniteReprocessor.ENABLED) && currentBlockState.getValue(CyaniteReprocessor.ENABLED) != isActive) {
            this.level.setBlock(this.getBlockPos(), currentBlockState.setValue(CyaniteReprocessor.ENABLED, isActive), 3);
            doUpdate = true;
        }

        if (this.autoEject) {
            this.autoEjectCycle();
        }

        if (doUpdate) {
            this.setChanged();
        }

        // Update the current machine state.
        this.updateState();
    }

    public CyaniteReprocessorItemHandler getItemHandler() {
        return this.itemHandler;
    }
}
