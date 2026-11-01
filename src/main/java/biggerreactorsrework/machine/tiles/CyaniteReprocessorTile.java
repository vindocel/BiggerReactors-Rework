package biggerreactorsrework.machine.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import biggerreactorsrework.core.transfer.SimpleEnergyStore;
import biggerreactorsrework.core.transfer.EnergyStore;
import biggerreactorsrework.core.transfer.ItemSlots;
import net.neoforged.neoforge.fluids.FluidStack;
import biggerreactorsrework.core.transfer.FluidTanks;
import biggerreactorsrework.core.transfer.SimpleTank;
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
// Modified 2026-10-27: industrial processing with independent item/fluid products and legacy saves.
public class CyaniteReprocessorTile extends BlockEntity implements MenuProvider, IHasUpdatableState<CyaniteReprocessorState>, biggerreactorsrework.core.transfer.TransferSnapshots {

    @RegisterTile("cyanite_reprocessor")
    public static final BlockEntityType.BlockEntitySupplier<CyaniteReprocessorTile> SUPPLIER = new RegisterTile.Producer<>(CyaniteReprocessorTile::new);

    @RegisterCapability
    private static final CapabilityRegistration ITEM_HANDLER_CAP_REGISTRATION = CapabilityRegistration.tileCap(Capabilities.Item.BLOCK, CyaniteReprocessorTile.class, (CyaniteReprocessorTile tile, @Nullable Direction side) -> biggerreactorsrework.core.transfer.Transfer.exposeItems(tile.itemHandlerFor(side), tile));
    @RegisterCapability
    private static final CapabilityRegistration ENERGY_CAP_REGISTRATION = CapabilityRegistration.tileCap(Capabilities.Energy.BLOCK, CyaniteReprocessorTile.class, (CyaniteReprocessorTile tile, @Nullable Direction side) -> biggerreactorsrework.core.transfer.Transfer.exposeEnergy(tile.energyFor(side), tile));
    @RegisterCapability
    private static final CapabilityRegistration FLUID_HANDLER_CAP_REGISTRATION = CapabilityRegistration.tileCap(Capabilities.Fluid.BLOCK, CyaniteReprocessorTile.class, (CyaniteReprocessorTile tile, @Nullable Direction side) -> biggerreactorsrework.core.transfer.Transfer.exposeFluids(tile.fluidFor(side), tile));

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
    // Added 2026-11-01: award only a completed cyanite-to-blutonium operation.
    private final biggerreactorsrework.advancement.ReworkAdvancements.Reporter advancements = new biggerreactorsrework.advancement.ReworkAdvancements.Reporter();
    /**
     * The energy storage. Other blocks can only insert energy; the machine consumes it while working.
     */
    private final Battery energyStorage = new Battery();
    /**
     * The water tank.
     */
    private final SimpleTank fluidTank = new SimpleTank(Config.CONFIG.CyaniteReprocessor.WaterTankCapacity, this::acceptsFluid) {
        @Override
        protected void onContentsChanged() {
            CyaniteReprocessorTile.this.setChanged();
        }
    };
    /**
     * View of the water tank for water input sides: it only fills.
     */
    private final FluidTanks waterInput = new FluidTanks() {
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
    private boolean autoEjectFluid = false;
    private final SimpleTank outputTank = new SimpleTank(Config.CONFIG.CyaniteReprocessor.WaterTankCapacity) {
        @Override protected void onContentsChanged() { CyaniteReprocessorTile.this.setChanged(); }
    };
    private final FluidTanks combinedFluids = fluidView(true,true);
    private FluidTanks fluidView(boolean input,boolean output) {
        return new FluidTanks() {
            public int getTanks(){return 2;}
            public FluidStack getFluidInTank(int tank){return (tank==0?fluidTank:outputTank).getFluid().copy();}
            public int getTankCapacity(int tank){return (tank==0?fluidTank:outputTank).getCapacity();}
            public boolean isFluidValid(int tank,FluidStack stack){return tank==0 && input && fluidTank.isFluidValid(stack);}
            public int fill(FluidStack stack,FluidAction action){return input?fluidTank.fill(stack,action):0;}
            public FluidStack drain(FluidStack stack,FluidAction action){return output?outputTank.drain(stack,action):FluidStack.EMPTY;}
            public FluidStack drain(int amount,FluidAction action){return output?outputTank.drain(amount,action):FluidStack.EMPTY;}
        };
    }
    private boolean autoInput = false;
    private boolean autoEject = false;
    private boolean autoInputFluid = false;
    /**
     * "Anti-cheat" item stack, to ensure players don't swap items mid-process.
     *
     * @see CyaniteReprocessorTile#tick()
     */
    private ItemStack itemPresentLastTick = ItemStack.EMPTY;

    private record TransferState(ItemStack[] items, int energy, FluidStack input, FluidStack output) {
    }

    @Override
    public Object transferSnapshot() {
        return new TransferState(itemHandler.snapshot(), energyStorage.getEnergyStored(), fluidTank.getFluid().copy(), outputTank.getFluid().copy());
    }

    @Override
    public void restoreTransferSnapshot(Object snapshot) {
        if (snapshot instanceof TransferState state) {
            itemHandler.restore(state.items);
            energyStorage.set(state.energy);
            fluidTank.setFluid(state.input);
            outputTank.setFluid(state.output);
        }
    }

    public CyaniteReprocessorTile(BlockEntityType<CyaniteReprocessorTile> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        this.updateState();
    }

    /**
     * Energy storage that only accepts energy from outside, with the configured transfer rate.
     */
    private final class Battery extends SimpleEnergyStore {
        Battery() {
            super(Config.CONFIG.CyaniteReprocessor.EnergyTankCapacity, Config.CONFIG.CyaniteReprocessor.TransferRate, 0);
        }

        // Modified 2026-10-30: persist positive external charging even while idle.
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int accepted = super.receiveEnergy(amount, simulate);
            if (!simulate && accepted > 0) CyaniteReprocessorTile.this.setChanged();
            return accepted;
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
    public ItemSlots itemHandlerFor(@Nullable Direction side) {
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
    public FluidTanks fluidFor(@Nullable Direction side) {
        if (side == null) return combinedFluids;
        SideRoute route = this.route(side, Transmission.FLUID);
        return switch(route) {
            case FLUID_INPUT -> waterInput;
            case FLUID_OUTPUT -> fluidView(false,true);
            case FLUID_INPUT_OUTPUT -> combinedFluids;
            default -> null;
        };
    }

    /**
     * Energy input on the configured faces; the {@code null} side sees the storage, which never gives energy back.
     */
    @Nullable
    public EnergyStore energyFor(@Nullable Direction side) {
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
            case "autoEjectFluid" -> { this.autoEjectFluid=!this.autoEjectFluid;this.setChanged(); }
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
    private <T> T neighbor(java.util.function.Function<net.minecraft.core.BlockPos, T> capability, Direction side) {
        BlockPos target = this.worldPosition.relative(side);
        if (this.level == null || !this.level.isLoaded(target)) {
            return null;
        }
        return capability.apply(target);
    }

    private ItemSlots neighborItems(Direction side) {
        return this.neighbor(target -> biggerreactorsrework.core.transfer.Transfer.items(this.level, target, side.getOpposite()), side);
    }

    private FluidTanks neighborFluids(Direction side) {
        return this.neighbor(target -> biggerreactorsrework.core.transfer.Transfer.fluids(this.level, target, side.getOpposite()), side);
    }

    /**
     * Pulls cyanite from the blocks on the item input sides.
     */
    private void autoInputCycle() {
        for (RelativeFace face : RelativeFace.VALUES) {
            if (!this.route(face, Transmission.ITEM).insertsItems()) {
                continue;
            }
            ItemSlots source = this.neighborItems(face.toDirection(this.facing()));
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
            ItemSlots destination = this.neighborItems(face.toDirection(this.facing()));
            if (destination == null) {
                continue;
            }
            ItemStack available = this.itemHandler.extractItem(CyaniteReprocessorItemHandler.OUTPUT_SLOT_INDEX, Integer.MAX_VALUE, true);
            int moved = available.getCount() - ItemSlots.insert(destination, available, true).getCount();
            if (moved > 0) {
                ItemSlots.insert(destination, this.itemHandler.extractItem(CyaniteReprocessorItemHandler.OUTPUT_SLOT_INDEX, moved, false), false);
            }
        }
    }

    /**
     * Pulls water from the blocks on the water input sides, at the configured transfer rate.
     */
    // Modified 2026-10-26: use the recipe's fluid instead of assuming water.
    private void autoEjectFluidCycle() {
        for(RelativeFace face:RelativeFace.VALUES) {
            var route=this.route(face,Transmission.FLUID);
            if(route!=SideRoute.FLUID_OUTPUT && route!=SideRoute.FLUID_INPUT_OUTPUT)continue;
            var target=neighborFluids(face.toDirection(facing()));
            if(target!=null)FluidTanks.move(target,outputTank,Config.CONFIG.CyaniteReprocessor.TransferRate);
        }
    }

    private int pullRecipeFluid(FluidTanks source,int space) {
        var input=itemHandler.getStackInSlot(CyaniteReprocessorItemHandler.INPUT_SLOT_INDEX);
        for(var entry:biggerreactorsrework.machine.recipe.ReprocessingRecipes.all(level)) {
            var recipe=entry.value();
            if(!recipe.matches(new net.minecraft.world.item.crafting.SingleRecipeInput(input),level) || recipe.totalFluid()==0)continue;
            int moved=FluidTanks.move(fluidTank,source,new FluidStack(recipe.fluid(),space)).getAmount();
            if(moved>0)return moved;
        }
        return 0;
    }

    private void autoInputFluidCycle() {
        for (RelativeFace face : RelativeFace.VALUES) {
            int space = Math.min(this.fluidTank.getSpace(), Config.CONFIG.CyaniteReprocessor.TransferRate);
            if (space <= 0) {
                return;
            }
            if (this.route(face, Transmission.FLUID) != SideRoute.FLUID_INPUT && this.route(face, Transmission.FLUID) != SideRoute.FLUID_INPUT_OUTPUT) {
                continue;
            }
            FluidTanks source = this.neighborFluids(face.toDirection(this.facing()));
            if (source != null) {
                pullRecipeFluid(source, space);
            }
        }
    }

    /**
     * Handle right-clicks holding the debug tool or a water bucket.
     *
     * @return {@link InteractionResult#PASS_TO_DEFAULT_BLOCK_INTERACTION} for other items, so the screen opens.
     */
    @Nonnull
    public InteractionResult onItemUse(ItemStack stack, Level level, Player player, InteractionHand hand) {
        // Print tile data.
        if (stack.is(DebugTool.INSTANCE)) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal(String.format("[%s] Progress: %s/%s", BiggerReactors.modid, this.workHandler.getProgress(), this.workHandler.getGoal())));
                player.sendSystemMessage(Component.literal(String.format("[%s] Energy: %s/%s FE", BiggerReactors.modid, this.energyStorage.getEnergyStored(), this.energyStorage.getMaxEnergyStored())));
                player.sendSystemMessage(Component.literal(String.format("[%s] Fluid Tank: %s/%s mB", BiggerReactors.modid, this.fluidTank.getFluidAmount(), this.fluidTank.getCapacity())));
            }
            return InteractionResult.SUCCESS;
        }

        // Do water bucket check.
        if (stack.is(Items.WATER_BUCKET)) {
            if (!level.isClientSide() && this.fluidTank.getSpace() >= 1000) {
                this.fluidTank.fill(new FluidStack(Fluids.WATER, 1000), FluidTanks.FluidAction.EXECUTE);
                if (!player.hasInfiniteMaterials()) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
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
        this.cyaniteReprocessorState.outputStored = outputTank.getFluidAmount();
        this.cyaniteReprocessorState.outputCapacity = outputTank.getCapacity();
        this.cyaniteReprocessorState.outputFluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(outputTank.getFluid().getFluid()).toString();
        this.cyaniteReprocessorState.inputFluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluidTank.getFluid().getFluid()).toString();
        this.cyaniteReprocessorState.autoEjectFluid = autoEjectFluid;
        var entry=biggerreactorsrework.machine.recipe.ReprocessingRecipes.all(level).stream()
            .filter(r->r.value().matches(new net.minecraft.world.item.crafting.SingleRecipeInput(itemHandler.getStackInSlot(0)),level)).findFirst().orElse(null);
        String condition="no_item";
        if(entry!=null) {
            var recipe=entry.value();int needed=recipe.fluidCostAt(Math.min(workHandler.getProgress(),recipe.duration()-1));
            boolean space=recipe.producesFluid()?outputTank.fill(recipe.fluidResult(),FluidTanks.FluidAction.SIMULATE)==recipe.fluidResult().getAmount():itemHandler.insertItem(1,recipe.result(),true).isEmpty();
            condition=!space?"output_full":energyStorage.getEnergyStored()<recipe.energy()?"no_energy":
                needed>0 && (fluidTank.getFluid().getFluid()!=recipe.fluid() || fluidTank.getFluidAmount()<needed)?"no_water":"working";
        }
        cyaniteReprocessorState.condition=condition;
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

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            this.dropContents(this.level, pos);
        }
    }

    /**
     * Read NBT data from the world.
     */
    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        HolderLookup.Provider registries = input.lookup();
        CompoundTag parentCompound = biggerreactorsrework.core.transfer.TagValues.read(input);
        CompoundTag childCompound = parentCompound.getCompoundOrEmpty("cyaniteReprocessorState");

        // Read work.
        this.workHandler = new WorkHandler(Config.CONFIG.CyaniteReprocessor.TotalWorkTime, Math.max(0, childCompound.getIntOr("workTime", 0)));
        activeRecipeId = net.minecraft.resources.Identifier.tryParse(childCompound.getStringOr("recipe", ""));
        restoredRecipeSignature = childCompound.getStringOr("recipeSignature", "");
        // Read items.
        this.itemHandler.deserializeNBT(registries, childCompound.getCompoundOrEmpty("inventory"));
        if (this.itemHandler.getSlots() != CyaniteReprocessorItemHandler.SLOTS) {
            this.itemHandler.setSize(CyaniteReprocessorItemHandler.SLOTS);
        }
        // Read energy.
        this.energyStorage.set(childCompound.getIntOr("energyStored", 0));
        // Read fluids.
        this.fluidTank.readFromNBT(registries, childCompound.getCompoundOrEmpty("fluidTank"));
        this.outputTank.readFromNBT(registries, childCompound.getCompoundOrEmpty("outputTank"));
        this.autoEjectFluid = childCompound.getBooleanOr("autoEjectFluid", false);
        this.itemPresentLastTick = this.itemHandler.getStackInSlot(CyaniteReprocessorItemHandler.INPUT_SLOT_INDEX).copy();
        // Read sides and automation.
        this.sides.load(childCompound.getCompoundOrEmpty("sides"));
        this.autoInput = childCompound.getBooleanOr("autoInput", false);
        this.autoEject = childCompound.getBooleanOr("autoEject", false);
        this.autoInputFluid = childCompound.getBooleanOr("autoInputFluid", false);
        this.updateState();
    }

    /**
     * Save NBT data to the world.
     */
    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().registryAccess();
        CompoundTag childCompound = new CompoundTag();

        // Write work.
        childCompound.putInt("workTime", this.workHandler.getProgress());
        if (activeRecipeId != null) childCompound.putString("recipe", activeRecipeId.toString());
        childCompound.putString("recipeSignature", activeRecipeSignature);
        // Write items.
        childCompound.put("inventory", this.itemHandler.serializeNBT(registries));
        // Write energy.
        childCompound.putInt("energyStored", this.energyStorage.getEnergyStored());
        // Write fluids.
        childCompound.put("fluidTank", this.fluidTank.writeToNBT(registries, new CompoundTag()));
        childCompound.put("outputTank", this.outputTank.writeToNBT(registries,new CompoundTag()));
        childCompound.putBoolean("autoEjectFluid",autoEjectFluid);
        // Write sides and automation.
        childCompound.put("sides", this.sides.save());
        childCompound.putBoolean("autoInput", this.autoInput);
        childCompound.putBoolean("autoEject", this.autoEject);
        childCompound.putBoolean("autoInputFluid", this.autoInputFluid);

        output.store("cyaniteReprocessorState", CompoundTag.CODEC, childCompound);
    }

    /**
     * @return Whether or not the machine can perform work.
     * @see CyaniteReprocessorTile#tick()
     */
    private net.minecraft.resources.Identifier activeRecipeId;
    private String activeRecipeSignature = "";
    private String restoredRecipeSignature = "";
    private biggerreactorsrework.machine.recipe.ReprocessingRecipe fingerprintedRecipe;
    private String cachedFingerprint;


    private String signature(biggerreactorsrework.machine.recipe.ReprocessingRecipe recipe) {
        if (fingerprintedRecipe == recipe) return cachedFingerprint;
        fingerprintedRecipe = recipe;
        return cachedFingerprint = biggerreactorsrework.machine.recipe.ReprocessingRecipe.CODEC.codec().encodeStart(
                net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, level.registryAccess()), recipe).getOrThrow().toString();
    }

    private boolean acceptsItem(ItemStack stack) {
        return biggerreactorsrework.machine.recipe.ReprocessingRecipes.all(level).stream().anyMatch(r -> r.value().ingredient().test(stack));
    }

    private boolean acceptsFluid(FluidStack stack) {
        return biggerreactorsrework.machine.recipe.ReprocessingRecipes.all(level).stream().anyMatch(r -> r.value().fluid() == stack.getFluid());
    }

    private boolean canWork(biggerreactorsrework.machine.recipe.ReprocessingRecipe recipe) {
        boolean room=recipe.producesFluid() ? outputTank.fill(recipe.fluidResult(),FluidTanks.FluidAction.SIMULATE)==recipe.fluidResult().getAmount() :
            itemHandler.insertItem(CyaniteReprocessorItemHandler.OUTPUT_SLOT_INDEX,recipe.result(),true).isEmpty();
        int water=recipe.fluidCostAt(workHandler.getProgress());
        return room && energyStorage.getEnergyStored()>=recipe.energy() && (water==0 ||
            fluidTank.getFluid().getFluid()==recipe.fluid() && fluidTank.getFluidAmount()>=water);
    }

    /**
     * Do work (if possible). Server side only.
     */
    public void tick() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        this.itemHandler.inputFilter(this::acceptsItem);
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
        if (restoredRecipeSignature.isEmpty() && !ItemStack.isSameItemSameComponents(this.itemPresentLastTick, inputStack)) {
            this.workHandler.clear();
        }
        this.itemPresentLastTick = inputStack.copy();

        var selected = biggerreactorsrework.machine.recipe.ReprocessingRecipes.all(level).stream()
                .filter(r -> r.value().matches(new net.minecraft.world.item.crafting.SingleRecipeInput(inputStack), level))
                .filter(r -> r.value().water() == 0 || fluidTank.isEmpty() || r.value().fluid() == fluidTank.getFluid().getFluid())
                .findFirst().orElse(null);
        if (selected == null) {
            if (workHandler.getProgress() != 0) doUpdate = true;
            workHandler.clear(); activeRecipeId = null; activeRecipeSignature = ""; restoredRecipeSignature = "";
        } else {
            var recipe = selected.value();
            String signature = signature(recipe);
            if (!selected.id().identifier().equals(activeRecipeId) || !signature.equals(activeRecipeSignature) || workHandler.getGoal() != recipe.duration()) {
                int restored = selected.id().identifier().equals(activeRecipeId) && signature.equals(restoredRecipeSignature) ? workHandler.getProgress() : 0;
                workHandler = new WorkHandler(recipe.duration(), Math.min(restored, recipe.duration() - 1));
                activeRecipeId = selected.id().identifier(); activeRecipeSignature = signature; restoredRecipeSignature = "";
                doUpdate = true;
            }
            if (canWork(recipe)) {
                isActive = true; doUpdate = true;
                workHandler.increment(1);
                energyStorage.consume(recipe.energy());
                fluidTank.drain(recipe.fluidCostAt(workHandler.getProgress()-1), FluidTanks.FluidAction.EXECUTE);
            } else if (!recipe.producesFluid() && workHandler.getProgress() > 0) {
                workHandler.decrement(Math.min(2, workHandler.getProgress())); doUpdate = true;
            }
            if (workHandler.isFinished()) {
                boolean reprocessedCyanite = !recipe.producesFluid() && recipe.result().is(BlutoniumIngot.INSTANCE)
                        && itemHandler.getStackInSlot(CyaniteReprocessorItemHandler.INPUT_SLOT_INDEX).is(biggerreactorsrework.items.ingots.CyaniteIngot.INSTANCE);
                itemHandler.extractItem(CyaniteReprocessorItemHandler.INPUT_SLOT_INDEX, recipe.count(), false);
                if(recipe.producesFluid())outputTank.fill(recipe.fluidResult(),FluidTanks.FluidAction.EXECUTE);
                else itemHandler.insertItem(CyaniteReprocessorItemHandler.OUTPUT_SLOT_INDEX, recipe.result(), false);
                if (reprocessedCyanite) advancements.report(level, new net.minecraft.world.phys.AABB(getBlockPos()), "cyanite_reprocessed");
                workHandler.clear();
                itemPresentLastTick = itemHandler.getStackInSlot(CyaniteReprocessorItemHandler.INPUT_SLOT_INDEX).copy();
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

        if(autoEjectFluid)autoEjectFluidCycle();

        // Update the current machine state.
        this.updateState();
    }

    public CyaniteReprocessorItemHandler getItemHandler() {
        return this.itemHandler;
    }
}
