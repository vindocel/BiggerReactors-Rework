package biggerreactorsrework.core.energy;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.energy.IEnergyStorage;
import biggerreactorsrework.core.registry.OnModLoad;
import biggerreactorsrework.core.util.NonnullDefault;
import biggerreactorsrework.core.util.Pair;
import biggerreactorsrework.core.util.Util;

import javax.annotation.Nullable;
import java.lang.ref.Cleaner;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@NonnullDefault
public class EnergyHandlerWrappers {
    
    private record WrapperRegistration<T>(BlockCapability<T, Direction> capability,
                                          Function<T, IEnergyHandler> toWrapperConstructor,
                                          Function<IEnergyHandler, T> fromWrapperConstructor,
                                          int priority) {
        IEnergyHandler wrapTo(T optional) {
            return toWrapperConstructor.apply(optional);
        }
        
        T wrapFrom(IEnergyHandler optional) {
            //noinspection ConstantConditions
            return fromWrapperConstructor.apply(optional);
        }
    }
    
    private static final ObjectArrayList<WrapperRegistration<?>> supportedCapabilitiesList = new ObjectArrayList<>();
    private static final Object2ObjectMap<BlockCapability<?, Direction>, WrapperRegistration<?>> supportedCapabilitiesMap = new Object2ObjectOpenHashMap<>();
    
    public static synchronized <T> void registerWrapper(BlockCapability<T, Direction> capability, Function<T, IEnergyHandler> toWrapperConstructor, Function<IEnergyHandler, T> fromWrapperConstructor, int priority) {
        registerWrapper(new WrapperRegistration<>(capability, toWrapperConstructor, fromWrapperConstructor, priority));
    }
    
    public static synchronized <T> void registerWrapper(WrapperRegistration<T> registration) {
        supportedCapabilitiesList.add(registration);
        final Comparator<WrapperRegistration<?>> comparator = Comparator.comparingInt(WrapperRegistration::priority);
        supportedCapabilitiesList.sort(comparator.reversed());
        supportedCapabilitiesMap.put(registration.capability, registration);
    }
    
    static {
        // ensure that identity always registered and is max priority
//        registerWrapper(IEnergyHandler.CAPABILITY, Function.identity(), Function.identity(), Integer.MAX_VALUE);
    }
    
    @Nullable
    public static IEnergyHandler findCapability(ServerLevel level, BlockPos pos, Direction direction) {
        for (int i = 0; i < supportedCapabilitiesList.size(); i++) {
            //noinspection unchecked
            @Nullable
            final var registration = (WrapperRegistration<Object>) supportedCapabilitiesList.get(i);
            if (registration == null) {
                continue;
            }
            @Nullable
            final var capabilityOptional = level.getCapability(registration.capability, pos, direction);
            if (capabilityOptional == null) {
                continue;
            }
            return registration.wrapTo(capabilityOptional);
        }
        return null;
    }
    
    @Nullable
    public static <T> T attemptWrap(BlockCapability<T, Direction> capability, @Nullable IEnergyHandler handler) {
        //noinspection unchecked
        @Nullable
        final var registration = (WrapperRegistration<T>) supportedCapabilitiesMap.get(capability);
        if (registration == null || handler == null) {
            return null;
        }
        return registration.wrapFrom(handler);
    }
    
    public static <O> List<Pair<BlockCapability<?, ?>, ICapabilityProvider<O, ?, ?>>> wrap(ICapabilityProvider<O, Direction, IEnergyHandler> baseProducer) {
        var capList = new ReferenceArrayList<Pair<BlockCapability<?, ?>, ICapabilityProvider<O, ?, ?>>>();
        for (var wrapperRegistration : supportedCapabilitiesList) {
            //noinspection unchecked
            var wrapperRegistrationGeneric = (WrapperRegistration<Object>)wrapperRegistration;
            ICapabilityProvider<O, Direction, ?> provider = (object, direction) -> {
                @Nullable
                var baseCap = baseProducer.getCapability(object, direction);
                if(baseCap == null){
                    return null;
                }
                return wrapperRegistrationGeneric.fromWrapperConstructor().apply(baseCap);
            };
            capList.add(new Pair<>(wrapperRegistration.capability, provider));
        }
        return capList;
    }
    
    private static class Neo {
        
        public static final BlockCapability<IEnergyStorage, Direction> FORGE_ENERGY_CAPABILITY = Capabilities.EnergyStorage.BLOCK;

        @OnModLoad
        public static void onModLoad() {
            registerWrapper(FORGE_ENERGY_CAPABILITY, ToWrapper::new, FromWrapper::new, Integer.MIN_VALUE);
        }

        private static class ToWrapper implements IEnergyHandler {
            
            private final IEnergyStorage forgeStorage;
            
            ToWrapper(IEnergyStorage forgeStorage) {
                this.forgeStorage = forgeStorage;
            }
            
            @Override
            public long insertEnergy(long maxInsert, boolean simulate) {
                return forgeStorage.receiveEnergy(Util.clampToInt(maxInsert), simulate);
            }
            
            @Override
            public long extractEnergy(long maxExtract, boolean simulate) {
                return forgeStorage.extractEnergy(Util.clampToInt(maxExtract), simulate);
            }
            
            @Override
            public long energyStored() {
                return forgeStorage.getEnergyStored();
            }
            
            @Override
            public long maxEnergyStored() {
                return forgeStorage.getMaxEnergyStored();
            }
        }
        
        private static class FromWrapper implements IEnergyStorage {
            
            private final IEnergyHandler phosHandler;
            
            private FromWrapper(IEnergyHandler phosHandler) {
                this.phosHandler = phosHandler;
            }
            
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                // toIntExact is fine here because it is an exceptional case if its not within the int range,
                // as you cant ask for anything outside the int range
                return (int) PowerDistribution.bounded(maxReceive, phosHandler.insertEnergy(Math.max(0, maxReceive), simulate));
            }
            
            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return (int) PowerDistribution.bounded(maxExtract, phosHandler.extractEnergy(Math.max(0, maxExtract), simulate));
            }
            
            @Override
            public int getEnergyStored() {
                return Util.clampToInt(phosHandler.energyStored());
            }
            
            @Override
            public int getMaxEnergyStored() {
                return Util.clampToInt(phosHandler.maxEnergyStored());
            }
            
            @Override
            public boolean canExtract() {
                return true;
            }
            
            @Override
            public boolean canReceive() {
                return true;
            }
        }
    }

}
