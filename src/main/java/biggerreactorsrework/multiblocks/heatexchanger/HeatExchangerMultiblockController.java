package biggerreactorsrework.multiblocks.heatexchanger;

// Modified 2026-10-26: protect inventories when joining controllers.

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.Config;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerBaseBlock;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerBaseTile;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerChannelTile;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerFluidPortTile;
import biggerreactorsrework.util.FluidTransitionTank;
import biggerreactorsrework.core.CoreEvents;
import biggerreactorsrework.core.multiblock.MultiblockController;
import biggerreactorsrework.core.multiblock.MultiblockTileModule;
import biggerreactorsrework.core.multiblock.ValidationException;
import biggerreactorsrework.core.multiblock.common.IEventMultiblock;
import biggerreactorsrework.core.multiblock.common.IPersistentMultiblock;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblock;
import biggerreactorsrework.core.multiblock.touching.ITouchingMultiblock;
import biggerreactorsrework.core.util.HeatBody;
import biggerreactorsrework.core.util.Util;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static biggerreactorsrework.core.modular.block.IConnectedTexture.Module.*;

@ParametersAreNonnullByDefault
public class HeatExchangerMultiblockController extends MultiblockController<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController> implements
        IRectangularMultiblock<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController>,
        IPersistentMultiblock<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController>,
        ITouchingMultiblock<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController>,
        IEventMultiblock<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController> {
    
    public HeatExchangerMultiblockController(Level level) {
        super(level, HeatExchangerBaseTile.class, HeatExchangerBaseBlock.class);
    }
    
    public final Set<HeatExchangerChannelTile> condenserChannels = new LinkedHashSet<>();
    public final Set<HeatExchangerChannelTile> evaporatorChannels = new LinkedHashSet<>();
    private final Set<HeatExchangerFluidPortTile> fluidPorts = new LinkedHashSet<>();
    private final Set<biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerTerminalTile> renderTerminals=new LinkedHashSet<>();
    // Modified 2026-10-30: match the canonical terminal selection used by other multiblocks.
    public net.minecraft.core.BlockPos appearancePos(net.minecraft.core.BlockPos fallback) {
        return renderTerminals.stream().map(BlockEntity::getBlockPos).min(net.minecraft.core.BlockPos::compareTo)
                .map(net.minecraft.core.BlockPos::immutable).orElse(fallback);
    }
    private long soundActiveUntil;
    
    @Nullable
    @Override
    public Vector3ic minSize() {
        return new Vector3i(4, 3, 3);
    }
    
    @Nullable
    @Override
    public Vector3ic maxSize() {
        return new Vector3i(Config.CONFIG.HeatExchanger.MaxLength, Config.CONFIG.HeatExchanger.MaxHeight, Config.CONFIG.HeatExchanger.MaxWidth);
    }
    
    @Override
    public boolean allowedInteriorBlock(Block block) {
        return block instanceof AirBlock;
    }
    
    @Override
    public void validateStage1() throws ValidationException {
        if (condenserChannels.isEmpty() || evaporatorChannels.isEmpty()) {
            throw new ValidationException(Component.translatable("multiblock.error.biggerreactorsrework.heat_exchanger.missing_channel_type"));
        }
        
        if (fluidPorts.size() < 4) {
            throw new ValidationException(Component.translatable("multiblock.error.biggerreactorsrework.heat_exchanger.invalid_port_count"));
        }
    }
    
    @Override
    public void validateStage2() throws ValidationException {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        
        int condenserPorts = 0;
        int evaporatorPorts = 0;
        
        for (HeatExchangerFluidPortTile fluidPort : fluidPorts) {
            BlockPos portPos = fluidPort.getBlockPos();
            boolean channelFound = false;
            for (Direction value : Direction.values()) {
                mutableBlockPos.set(portPos);
                mutableBlockPos.move(value);
                BlockEntity tile = blocks.getTile(mutableBlockPos);
                if (tile instanceof HeatExchangerChannelTile channelTile) {
                    if (channelTile.CONDENSER) {
                        condenserPorts++;
                    } else {
                        evaporatorPorts++;
                    }
                    channelFound = true;
                    break;
                }
            }
            if (!channelFound) {
                throw new ValidationException(Component.translatable("multiblock.error.biggerreactorsrework.heat_exchanger.fluid_port_unconnected", portPos.getX(), portPos.getY(), portPos.getZ()));
            }
        }
        if (condenserPorts < 2 || evaporatorPorts < 2) {
            // technically this isn't the problem im checking, but this is a secondary check that happens, without having to march the channels
            throw new ValidationException(Component.translatable("multiblock.error.biggerreactorsrework.heat_exchanger.duplicate_port_types"));
        }
        
        verifyFluidChannels(condenserChannels);
        verifyFluidChannels(evaporatorChannels);
        
        long tick = CoreEvents.tickNumber();
        
        // *not that i dont have to march them anyway*
        for (HeatExchangerFluidPortTile fluidPort : fluidPorts) {
            if (fluidPort.lastCheckedTick == tick) {
                continue;
            }
            Direction nextDirection = null;
            for (Direction value : Direction.values()) {
                mutableBlockPos.set(fluidPort.getBlockPos());
                mutableBlockPos.move(value);
                HeatExchangerBaseTile tile = blocks.getTile(mutableBlockPos);
                if (tile instanceof HeatExchangerChannelTile) {
                    nextDirection = value;
                    break;
                }
            }
            mutableBlockPos.set(fluidPort.getBlockPos());
            if (nextDirection == null) {
                throw new ValidationException("Unknown channel verification error, this shouldn't be possible " + mutableBlockPos);
            }
            MultiblockTileModule<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController> currentModule = fluidPort.multiblockModule();
            while (true) {
                mutableBlockPos.move(nextDirection);
                currentModule = currentModule.getNeighbor(nextDirection);
                if (currentModule == null) {
                    throw new ValidationException("Unknown channel verification error, this shouldn't be possible " + mutableBlockPos);
                }
                HeatExchangerBaseTile channelTile = currentModule.iface;
                if (channelTile instanceof HeatExchangerFluidPortTile) {
                    break;
                }
                if (!(channelTile instanceof HeatExchangerChannelTile)) {
                    throw new ValidationException("Unknown channel verification error, this shouldn't be possible " + mutableBlockPos);
                }
                if (channelTile instanceof HeatExchangerChannelTile) {
                    ((HeatExchangerChannelTile) channelTile).lastCheckedTick = tick;
                }
                BlockState channelState = channelTile.getBlockState();
                if (nextDirection != Direction.DOWN && channelState.getValue(TOP_CONNECTED_PROPERTY)) {
                    nextDirection = Direction.UP;
                    continue;
                }
                if (nextDirection != Direction.UP && channelState.getValue(BOTTOM_CONNECTED_PROPERTY)) {
                    nextDirection = Direction.DOWN;
                    continue;
                }
                
                if (nextDirection != Direction.SOUTH && channelState.getValue(NORTH_CONNECTED_PROPERTY)) {
                    nextDirection = Direction.NORTH;
                    continue;
                }
                if (nextDirection != Direction.NORTH && channelState.getValue(SOUTH_CONNECTED_PROPERTY)) {
                    nextDirection = Direction.SOUTH;
                    continue;
                }
                
                if (nextDirection != Direction.WEST && channelState.getValue(EAST_CONNECTED_PROPERTY)) {
                    nextDirection = Direction.EAST;
                    continue;
                }
                if (nextDirection != Direction.EAST && channelState.getValue(WEST_CONNECTED_PROPERTY)) {
                    nextDirection = Direction.WEST;
                    continue;
                }
                throw new ValidationException("Unknown channel verification error, this shouldn't be possible " + mutableBlockPos);
            }
        }
        
        for (HeatExchangerChannelTile condenserChannel : condenserChannels) {
            if (condenserChannel.lastCheckedTick != tick) {
                BlockPos channelPos = condenserChannel.getBlockPos();
                throw new ValidationException(Component.translatable("multiblock.error.biggerreactorsrework.heat_exchanger.dangling_channel", channelPos.getX(), channelPos.getY(), channelPos.getZ()));
            }
        }
        
        for (HeatExchangerChannelTile evaporatorChannel : evaporatorChannels) {
            if (evaporatorChannel.lastCheckedTick != tick) {
                BlockPos channelPos = evaporatorChannel.getBlockPos();
                throw new ValidationException(Component.translatable("multiblock.error.biggerreactorsrework.heat_exchanger.dangling_channel", channelPos.getX(), channelPos.getY(), channelPos.getZ()));
            }
        }
    }
    
    @Override
    public void validateStage3() throws ValidationException {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        Util.chunkCachedBlockStateIteration(min(), max(), level, (block, pos) -> {
            if (block.getBlock() instanceof HeatExchangerBaseBlock) {
                mutableBlockPos.set(pos.x, pos.y, pos.z);
                if (!blocks.containsPos(mutableBlockPos)) {
                    throw new ValidationException(Component.translatable("multiblock.error.biggerreactorsrework.heat_exchanger.dangling_internal_part", pos.x, pos.y, pos.z));
                }
            }
        });
    }
    
    private void verifyFluidChannels(Set<? extends HeatExchangerBaseTile> channels) throws ValidationException {
        for (HeatExchangerBaseTile tile : channels) {
            BlockState state = tile.getBlockState();
            int connectedSides = 0;
            connectedSides += state.getValue(TOP_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(BOTTOM_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(NORTH_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(SOUTH_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(EAST_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(WEST_CONNECTED_PROPERTY) ? 1 : 0;
            
            if (connectedSides != 2) {
                throw new ValidationException("all fluid channels must have exactly two connections " + tile.getBlockPos());
            }
        }
    }

    @Override
    protected void onPartAdded(HeatExchangerBaseTile toAttach) {
        if(toAttach instanceof biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerTerminalTile terminal)renderTerminals.add(terminal);
        if (toAttach instanceof HeatExchangerChannelTile channelTile) {
            if (channelTile.CONDENSER) {
                condenserChannels.add(channelTile);
            } else {
                evaporatorChannels.add(channelTile);
            }
        }
        if (toAttach instanceof HeatExchangerFluidPortTile) {
            fluidPorts.add((HeatExchangerFluidPortTile) toAttach);
        }
    }
    
    @Override
    protected void onPartRemoved(HeatExchangerBaseTile toDetach) {
        renderTerminals.remove(toDetach);
        if (toDetach instanceof HeatExchangerChannelTile) {
            condenserChannels.remove(toDetach);
        }
        if (toDetach instanceof HeatExchangerChannelTile channelTile) {
            if (channelTile.CONDENSER) {
                condenserChannels.remove(channelTile);
            } else {
                evaporatorChannels.remove(channelTile);
            }
        }
        if (toDetach instanceof HeatExchangerFluidPortTile) {
            fluidPorts.remove((HeatExchangerFluidPortTile) toDetach);
        }
    }

    public final ReadWriteLock locks = new ReentrantReadWriteLock();

    public final FluidTransitionTank evaporatorTank = new FluidTransitionTank(false);
    public final FluidTransitionTank condenserTank = new FluidTransitionTank(true);
    // Modified 2026-10-26: coupled conversion limits from persistent steam output only.
    public final HeatExchangerBackpressure outputPressure=new HeatExchangerBackpressure(condenserTank,evaporatorTank);

    public final HeatBody ambientHeatBody = new HeatBody();
    public final HeatBody airHeatBody = new HeatBody();
    public final HeatBody condenserHeatBody = new HeatBody();
    public final HeatBody evaporatorHeatBody = new HeatBody();

    public double channelRFKT;
    public boolean balancedOutputs;
    private int primaryCursor,secondaryCursor,primaryMaintenanceCursor,secondaryMaintenanceCursor;
    public double thermalLinks;
    public final biggerreactorsrework.thermal.ThermalExchanger thermalExchange = new biggerreactorsrework.thermal.ThermalExchanger();
    private boolean thermalInitialized;
    private double thermalStorageChange;
    private HeatExchangerCapacity.Geometry statisticsGeometry;
    private String statisticsProfile="";
    private java.util.concurrent.CompletableFuture<biggerreactorsrework.core.util.Statistics.Potential> capacityEstimate;

    public java.util.List<java.util.Map<String,Object>> statistics() {
        var rows=new java.util.ArrayList<java.util.Map<String,Object>>();
        var primary=condenserTank.activeTransition();
        var secondary=evaporatorTank.activeTransition();
        if(secondary==null)secondary=biggerreactorsrework.registries.FluidTransitionRegistry.liquidTransition(net.minecraft.world.level.material.Fluids.WATER);
        if(statisticsGeometry==null)return rows;
        String key=primary==null || secondary==null?"empty":biggerreactorsrework.registries.CoolantComponents.fingerprint(primary)+"/"+biggerreactorsrework.registries.CoolantComponents.fingerprint(secondary);
        if(!key.equals(statisticsProfile)){if(capacityEstimate!=null)capacityEstimate.cancel(true);capacityEstimate=null;statisticsProfile=key;}
        if(capacityEstimate==null && primary!=null && secondary!=null){var g=statisticsGeometry;var cold=secondary;capacityEstimate=biggerreactorsrework.core.util.Statistics.estimate(()->HeatExchangerCapacity.measure(g,primary,cold));}
        var potential=capacityEstimate!=null && capacityEstimate.isDone() && !capacityEstimate.isCompletedExceptionally()?capacityEstimate.getNow(null):null;
        rows.add(biggerreactorsrework.core.util.Statistics.row("dimensions",(max().x()-min().x()+1)+" \u00D7 "+(max().y()-min().y()+1)+" \u00D7 "+(max().z()-min().z()+1)));
        rows.add(biggerreactorsrework.core.util.Statistics.row("condenser_channels",condenserChannels.size()));
        rows.add(biggerreactorsrework.core.util.Statistics.row("evaporator_channels",evaporatorChannels.size()));
        rows.add(biggerreactorsrework.core.util.Statistics.row("contacts",Math.round(channelRFKT/Config.CONFIG.HeatExchanger.ChannelFEPerKelvinMetreSquared)));
        long hotCapacity=(long)Math.floor(thermalLinks*Config.CONFIG.Reactor.Thermal.HotMBPerLink);
        rows.add(biggerreactorsrework.core.util.Statistics.rate("hot_capacity",hotCapacity));
        rows.add(primary==null?biggerreactorsrework.core.util.Statistics.row("water_steam_capacity","@status:awaiting_fluid"):biggerreactorsrework.core.util.Statistics.rate("water_steam_capacity",hotCapacity*primary.steamPerBatch/10));
        rows.add(biggerreactorsrework.core.util.Statistics.pair(condenserTank));
        rows.add(biggerreactorsrework.core.util.Statistics.quantity("primary_capacity",condenserTank.perSideCapacity));
        rows.add(biggerreactorsrework.core.util.Statistics.quantity("hot_stored",condenserTank.fluidAmountInTank(0)));
        rows.add(biggerreactorsrework.core.util.Statistics.quantity("cold_stored",condenserTank.fluidAmountInTank(1)));
        rows.add(potential==null?biggerreactorsrework.core.util.Statistics.row("conversion_potential",primary==null?"@status:awaiting_fluid":"@status:calculating"):biggerreactorsrework.core.util.Statistics.rate("conversion_potential",potential.converted()));
        rows.add(biggerreactorsrework.core.util.Statistics.rate("conversion_current",condenserTank.transitionedLastTick()));
        rows.add(biggerreactorsrework.core.util.Statistics.rate("input_current",condenserTank.inputRate));
        rows.add(biggerreactorsrework.core.util.Statistics.rate("output_current",condenserTank.outputRate));
        if(potential!=null){rows.add(biggerreactorsrework.core.util.Statistics.rate("water_potential",potential.burn()));rows.add(biggerreactorsrework.core.util.Statistics.rate("steam_potential",potential.burn()));}
        rows.add(biggerreactorsrework.core.util.Statistics.rate("steam_current",evaporatorTank.transitionedLastTick()));
        rows.add(biggerreactorsrework.core.util.Statistics.quantity("water_capacity",evaporatorTank.perSideCapacity));
        rows.add(biggerreactorsrework.core.util.Statistics.quantity("water_stored",evaporatorTank.fluidAmountInTank(0)));
        rows.add(biggerreactorsrework.core.util.Statistics.quantity("steam_stored",evaporatorTank.fluidAmountInTank(1)));
        rows.add(biggerreactorsrework.core.util.Statistics.row("output_occupancy",String.format(java.util.Locale.ROOT,"%.1f%%",evaporatorTank.perSideCapacity<=0?0:100d*evaporatorTank.fluidAmountInTank(1)/evaporatorTank.perSideCapacity)));
        String reason=condenserTank.hasUnresolvedProfile() || evaporatorTank.hasUnresolvedProfile()?"invalid_coolant":condenserTank.fluidAmountInTank(0)==0?"no_heat":evaporatorTank.fluidAmountInTank(0)==0?"no_water":
                evaporatorTank.fluidAmountInTank(1)>=evaporatorTank.perSideCapacity?"steam_blocked":
                condenserTank.fluidAmountInTank(1)>=condenserTank.perSideCapacity?"cold_blocked":"normal";
        if(reason.equals("normal") && potential!=null && condenserTank.transitionedLastTick()<potential.converted()*.99) {
            reason="partial_supply";
        }
        rows.add(biggerreactorsrework.core.util.Statistics.row("status","@status:"+reason));
        rows.add(biggerreactorsrework.core.util.Statistics.row("reference","@status:full_inlet_tanks"));
        rows.add(biggerreactorsrework.core.util.Statistics.row("thermal_links",String.format(java.util.Locale.ROOT,"%.1f",thermalLinks)));
        rows.add(biggerreactorsrework.core.util.Statistics.row("thermal_generated",String.format(java.util.Locale.ROOT,"%.2f kJ/t",thermalExchange.inputKJ)));
        rows.add(biggerreactorsrework.core.util.Statistics.row("thermal_useful",String.format(java.util.Locale.ROOT,"%.2f kJ/t",thermalExchange.usefulKJ)));
        rows.add(biggerreactorsrework.core.util.Statistics.row("thermal_processLoss",String.format(java.util.Locale.ROOT,"%.2f kJ/t",thermalExchange.processLossKJ)));
        if(thermalLinks==0)rows.add(biggerreactorsrework.core.util.Statistics.row("heat_path","@status:indirect_heat"));
        return rows;
    }
    public double evaporatorAirRFKT;
    public double condenserAirRFKT;
    public double airAmbientRFKT;
    // Added 2026-11-01: milestones follow actual conversion, not stored heat or a GUI setting.
    private final biggerreactorsrework.advancement.ReworkAdvancements.Reporter advancements = new biggerreactorsrework.advancement.ReworkAdvancements.Reporter();

    @Override
    public void onAssembled() {
        validationPassed();
    }
    
    @Override
    public void onReassembled() {
        validationPassed();
    }
    
    private void validationPassed() {
        
        // its in kelvin, 150C and 20C
        double ambientTemperature = level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, net.minecraft.world.phys.Vec3.ZERO, null) ? 423.15 : 293.15; // TODO config these, also the end
        ambientHeatBody.setTemperature(ambientTemperature);
        // Modified 2026-11-02: deprecated temperatures are neutral and never a productive reserve.
        airHeatBody.setTemperature(ambientTemperature);condenserHeatBody.setTemperature(ambientTemperature);evaporatorHeatBody.setTemperature(ambientTemperature);

        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        condenserHeatBody.setRfPerKelvin(condenserChannels.size() * Config.CONFIG.HeatExchanger.ChannelFEPerKelvinUnitVolume);
        evaporatorHeatBody.setRfPerKelvin(evaporatorChannels.size() * Config.CONFIG.HeatExchanger.ChannelFEPerKelvinUnitVolume);

        condenserTank.perSideCapacity = condenserChannels.size() * Config.CONFIG.HeatExchanger.ChannelTankVolumePerBlock;
        evaporatorTank.perSideCapacity = evaporatorChannels.size() * Config.CONFIG.HeatExchanger.evaporatorTankVolumePerBlock();

        Vector3i vec = new Vector3i(max()).sub(min()).add(1, 1, 1);
        int airVolume = vec.x * vec.y * vec.z;
        airVolume -= condenserChannels.size();
        airVolume -= evaporatorChannels.size();
        airHeatBody.setRfPerKelvin(airVolume * Config.CONFIG.HeatExchanger.AirFEPerKelvinUnitVolume);

        int channelContactArea = 0;
        int evaporatorAirContactArea = 0;
        int condenserAirContactArea = 0;
        int airAmbientContactArea = 0;
        
        for (HeatExchangerChannelTile condenserChannel : condenserChannels) {
            for (Direction value : Direction.values()) {
                mutableBlockPos.set(condenserChannel.getBlockPos());
                mutableBlockPos.move(value);
                HeatExchangerBaseTile tile = blocks.getTile(mutableBlockPos);
                if (tile instanceof HeatExchangerChannelTile channelTile && !channelTile.CONDENSER) {
                    channelContactArea++;
                } else if (!(tile instanceof HeatExchangerChannelTile)) {
                    condenserAirContactArea++;
                }
            }
        }

        for (HeatExchangerChannelTile condenserChannel : evaporatorChannels) {
            for (Direction value : Direction.values()) {
                mutableBlockPos.set(condenserChannel.getBlockPos());
                mutableBlockPos.move(value);
                HeatExchangerBaseTile tile = blocks.getTile(mutableBlockPos);
                if (!(tile instanceof HeatExchangerChannelTile)) {
                    evaporatorAirContactArea++;
                }
            }
        }

        airAmbientContactArea = vec.x * vec.y + vec.x * vec.z + vec.y * vec.z;
        airAmbientContactArea *= 2;

        // Modified 2026-11-02: cache straight links through at most three air cells, scanning from primary only.
        thermalLinks=biggerreactorsrework.thermal.ExchangerLinks.scan(condenserChannels.stream().map(c->
            new biggerreactorsrework.thermal.ExchangerLinks.Position(c.getBlockPos().getX(),c.getBlockPos().getY(),c.getBlockPos().getZ())).toList(),(x,y,z)-> {
                if(x<=min().x() || x>=max().x() || y<=min().y() || y>=max().y() || z<=min().z() || z>=max().z())return 3;
                var state=level.getBlockState(new BlockPos(x,y,z));
                if(state.getBlock() instanceof biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerCondenserChannelBlock)return 1;
                if(state.getBlock() instanceof biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerEvaporatorChannelBlock)return 2;
                return state.isAir()?0:3;
            }).weighted();
        channelRFKT = channelContactArea * Config.CONFIG.HeatExchanger.ChannelFEPerKelvinMetreSquared;
        condenserAirRFKT = condenserAirContactArea * Config.CONFIG.HeatExchanger.AirFEPerKelvinMetreSquared;
        evaporatorAirRFKT = evaporatorAirContactArea * Config.CONFIG.HeatExchanger.AirFEPerKelvinMetreSquared;
        airAmbientRFKT = airAmbientContactArea * Config.CONFIG.HeatExchanger.AmbientFEPerKelvinMetreSquared;
        statisticsGeometry=new HeatExchangerCapacity.Geometry(condenserChannels.size(),evaporatorChannels.size(),channelRFKT,condenserAirRFKT,evaporatorAirRFKT,airAmbientRFKT,airHeatBody.rfPerKelvin(),ambientTemperature,thermalLinks);
        if(capacityEstimate!=null)capacityEstimate.cancel(true);
        capacityEstimate=null;

        for (HeatExchangerFluidPortTile coolantPort : fluidPorts) {
            BlockPos portPos = coolantPort.getBlockPos();
            for (Direction value : Direction.values()) {
                mutableBlockPos.set(portPos);
                mutableBlockPos.move(value);
                BlockEntity tile = blocks.getTile(mutableBlockPos);
                if (tile instanceof HeatExchangerChannelTile channelTile) {
                    coolantPort.setHETank(channelTile.CONDENSER ? condenserTank : evaporatorTank);
                    coolantPort.setCondenser(channelTile.CONDENSER);
                    break;
                }
            }
        }

        // Preserve all configured directions; initialize a missing direction only once for each logical circuit.
        for(boolean circuit:new boolean[]{true,false}) {
            var ports=fluidPorts.stream().filter(p->p.isCondenser()==circuit).sorted(java.util.Comparator.comparing(HeatExchangerFluidPortTile::getBlockPos)).toList();
            if(ports.stream().noneMatch(HeatExchangerFluidPortTile::isInlet) && !ports.isEmpty())ports.getFirst().setInlet(true);
            if(ports.stream().allMatch(HeatExchangerFluidPortTile::isInlet) && ports.size()>1)ports.getLast().setInlet(false);
        }

        ambientHeatBody.setInfinite(true);
        refreshRenderPaths();updateFluidRendering();
    }

    private record RenderStep(HeatExchangerChannelTile tile,Direction entry,Direction exit) { }
    private void refreshRenderPaths() {
        if(renderTerminals.isEmpty())return;var anchor=renderTerminals.iterator().next().getBlockPos();
        assignRenderPath(condenserChannels,anchor,true);assignRenderPath(evaporatorChannels,anchor,false);
    }
    private static boolean channelConnected(BlockState state,Direction direction) {
        var property=switch(direction){case DOWN->BOTTOM_CONNECTED_PROPERTY;case UP->TOP_CONNECTED_PROPERTY;case NORTH->NORTH_CONNECTED_PROPERTY;case SOUTH->SOUTH_CONNECTED_PROPERTY;case EAST->EAST_CONNECTED_PROPERTY;case WEST->WEST_CONNECTED_PROPERTY;};
        return state.hasProperty(property) && state.getValue(property);
    }
    private void assignRenderPath(Set<HeatExchangerChannelTile> channels,BlockPos anchor,boolean condenser) {
        var parts=new java.util.HashMap<BlockPos,HeatExchangerChannelTile>();for(var c:channels)parts.put(c.getBlockPos(),c);
        var path=new java.util.ArrayList<RenderStep>();var visited=new java.util.HashSet<BlockPos>();
        var starts=fluidPorts.stream().filter(p->p.isCondenser()==condenser)
            .sorted(java.util.Comparator.comparing(HeatExchangerFluidPortTile::isInlet).reversed().thenComparing(HeatExchangerFluidPortTile::getBlockPos)).toList();
        for(var inlet:starts) {
            HeatExchangerChannelTile current=null;Direction entry=null;
            for(var d:Direction.values()){current=parts.get(inlet.getBlockPos().relative(d));if(current!=null){entry=d.getOpposite();break;}}
            while(current!=null && visited.add(current.getBlockPos())) {
                Direction exit=null;for(var d:Direction.values())if(d!=entry && channelConnected(current.getBlockState(),d)){exit=d;break;}
                if(exit==null)break;path.add(new RenderStep(current,entry,exit));
                current=parts.get(current.getBlockPos().relative(exit));entry=exit.getOpposite();
            }
        }
        if(path.size()!=channels.size())return;
        for(int i=0;i<path.size();i++){
            var step=path.get(i);var c=step.tile;c.coolantAnchor=anchor;c.coolantPathIndex=i;c.coolantPathLength=path.size();c.coolantEntry=step.entry;c.coolantExit=step.exit;
            c.setChanged();level.sendBlockUpdated(c.getBlockPos(),c.getBlockState(),c.getBlockState(),2);
        }
    }
    private void updateFluidRendering() {
        if(renderTerminals.isEmpty())return;var terminal=renderTerminals.iterator().next();
        var primary=biggerreactorsrework.core.fluids.FluidRenderSnapshot.capture(condenserTank);
        var secondary=biggerreactorsrework.core.fluids.FluidRenderSnapshot.capture(evaporatorTank);
        // Modified 2026-10-27: light follows the visible hot-phase span, never steam or the cold return.
        boolean hot=!condenserTank.hasUnresolvedProfile() && biggerreactorsrework.multiblocks.MachineLighting.hotFluid(condenserTank.fluidTypeInTank(0));
        for(var channel:condenserChannels) {
            var span=biggerreactorsrework.core.util.FluidPath.span(primary.inputFill(),primary.outputFill(),channel.coolantPathIndex,channel.coolantPathLength,false);
            setChannelLight(channel,hot && channel.coolantPathLength>0 && span.end()>span.start());
        }
        if(primary.equals(terminal.primaryView) && secondary.equals(terminal.secondaryView))return;
        terminal.primaryView=primary;terminal.secondaryView=secondary;
        level.sendBlockUpdated(terminal.getBlockPos(),terminal.getBlockState(),terminal.getBlockState(),2);
    }

    private void setChannelLight(HeatExchangerChannelTile channel,boolean lit) {
        var state=channel.getBlockState();var property=net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
        if(state.hasProperty(property) && state.getValue(property)!=lit)
            level.setBlock(channel.getBlockPos(),state.setValue(property,lit),3);
    }

    @Override
    public void onDisassembled() {
        biggerreactorsrework.audio.MachineAudio.publish(1, min(), max(), renderTerminals, false, 0);
        condenserChannels.forEach(channel->setChannelLight(channel,false));
    }

    @Override
    public void tick() {
        // Modified 2026-11-02: shared atomic recipe; legacy temperatures never produce additional output.
        condenserTank.sampleFlows();evaporatorTank.sampleFlows();
        var preset=Config.CONFIG.Reactor.Thermal.snapshot(Config.CONFIG.Reactor.PerFuelRodCapacity,Config.CONFIG.Reactor.CoolantTankAmountPerFuelRod);
        thermalExchange.tick(condenserTank,evaporatorTank,thermalLinks*preset.exchangerRate(),preset);
        if (evaporatorTank.transitionedLastTick() > 0) advancements.report(level, min(), max(), "heat_exchange");
        // Modified 2026-10-28: real conversion drives the boiler loop, not stored heat.
        if (condenserTank.transitionedLastTick() > 0 || evaporatorTank.transitionedLastTick() > 0)
            soundActiveUntil = CoreEvents.tickNumber() + 10;
        biggerreactorsrework.audio.MachineAudio.publish(1, min(), max(), renderTerminals, CoreEvents.tickNumber() < soundActiveUntil, 0);
        thermalStorageChange=0;
        if(balancedOutputs) {
            for(boolean circuit:new boolean[]{true,false}) for(boolean maintenance:new boolean[]{false,true}) {
                int phase=maintenance?0:1;
                var tank=circuit?condenserTank:evaporatorTank;
                var targets=fluidPorts.stream().filter(p->p.isCondenser()==circuit && p.isRecoveringInput()==maintenance).sorted(java.util.Comparator.comparing(HeatExchangerFluidPortTile::getBlockPos))
                    .<java.util.function.LongUnaryOperator>map(p->p::offerFluid).toList();
                var result=biggerreactorsrework.thermal.BalancedOutput.distribute(tank.fluidAmountInTank(phase),targets,maintenance?(circuit?primaryMaintenanceCursor:secondaryMaintenanceCursor):(circuit?primaryCursor:secondaryCursor));
                tank.maintenanceDrain(phase,tank.maintenanceFluid(phase),tank.maintenanceComponents(phase),result.accepted(),false);
                if(maintenance) { if(circuit)primaryMaintenanceCursor=result.nextCursor();else secondaryMaintenanceCursor=result.nextCursor(); }
                else if(circuit)primaryCursor=result.nextCursor();else secondaryCursor=result.nextCursor();
            }
        } else fluidPorts.forEach(HeatExchangerFluidPortTile::pushFluid);
        if(CoreEvents.tickNumber()%10==0)updateFluidRendering();
        if (CoreEvents.tickNumber() % 2 == 0) {
            dirty();
        }
    }
    
    @Override
    protected boolean hasStoredContents() { return condenserTank.hasStoredContents() || evaporatorTank.hasStoredContents(); }

    @Override
    public CompoundTag mergeNBTs(CompoundTag nbtA, CompoundTag nbtB) {
        // TODO: this, as a whole, this
        return nbtA;
    }
    
    @Override
    public void read(CompoundTag nbt) {
        balancedOutputs=nbt.getBooleanOr("balancedOutputs", false);primaryCursor=nbt.getIntOr("primaryCursor", 0);secondaryCursor=nbt.getIntOr("secondaryCursor", 0);primaryMaintenanceCursor=nbt.getIntOr("primaryMaintenanceCursor", 0);secondaryMaintenanceCursor=nbt.getIntOr("secondaryMaintenanceCursor", 0);
        condenserTank.deserializeNBT(nbt.getCompoundOrEmpty("condenserTank"));
        evaporatorTank.deserializeNBT(nbt.getCompoundOrEmpty("evaporatorTank"));
        outputPressure.load(nbt.getCompoundOrEmpty("outputPressure"));
        if(nbt.getIntOr("thermalModelVersion", 0)==biggerreactorsrework.thermal.ThermalModel.VERSION){ thermalExchange.restoreCredit(nbt.getDoubleOr("flowCredit", 0D));thermalExchange.restoreRecipe(nbt.getStringOr("flowRecipe", "")); }
        double ambient=level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, net.minecraft.world.phys.Vec3.ZERO, null)?423.15:293.15;
        for(var body:new HeatBody[]{ambientHeatBody,airHeatBody,condenserHeatBody,evaporatorHeatBody})body.setTemperature(ambient);

    }

    @Nonnull
    @Override
    public CompoundTag write() {
        final var nbt = new CompoundTag();
        nbt.putBoolean("balancedOutputs",balancedOutputs);nbt.putInt("primaryCursor",primaryCursor);nbt.putInt("secondaryCursor",secondaryCursor);nbt.putInt("primaryMaintenanceCursor",primaryMaintenanceCursor);nbt.putInt("secondaryMaintenanceCursor",secondaryMaintenanceCursor);
        nbt.putInt("thermalModelVersion",biggerreactorsrework.thermal.ThermalModel.VERSION);
        nbt.putDouble("flowCredit",thermalExchange.credit());nbt.putString("flowRecipe",thermalExchange.recipe());
        nbt.put("condenserTank", condenserTank.serializeNBT());
        nbt.put("evaporatorTank", evaporatorTank.serializeNBT());
        nbt.put("outputPressure",outputPressure.save());
        nbt.putDouble("ambientHeatBody", ambientHeatBody.temperature());
        nbt.putDouble("airHeatBody", airHeatBody.temperature());
        nbt.putDouble("condenserHeatBody", condenserHeatBody.temperature());
        nbt.putDouble("evaporatorHeatBody", evaporatorHeatBody.temperature());
        return nbt;
    }

    public java.util.List<HeatExchangerFluidPortTile> fluidPorts() { return java.util.List.copyOf(fluidPorts); }

    public void setInletPort(HeatExchangerFluidPortTile port, boolean inlet) {
        port.setInlet(inlet);
        refreshRenderPaths();
    }

    public void runRequest(String requestName, @Nullable Object requestData) {
        switch (requestName) {
            case "balancedOutputs" -> { if(requestData instanceof Boolean value) { balancedOutputs=value;dirty(); } }
            // Manually dump tanks.
            case "dumpTanks" -> {
                if (!(requestData instanceof Boolean)) {
                    return;
                }
                // Condenser true, evaporator false
                if((Boolean) requestData) {
                    condenserTank.dumpTank(FluidTransitionTank.IN_TANK);
                    condenserTank.dumpTank(FluidTransitionTank.OUT_TANK);
                } else {
                    evaporatorTank.dumpTank(FluidTransitionTank.IN_TANK);
                    evaporatorTank.dumpTank(FluidTransitionTank.OUT_TANK);
                }
            }
        }
    }
}
