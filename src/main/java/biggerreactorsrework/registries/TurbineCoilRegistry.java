package biggerreactorsrework.registries;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.Config;
import biggerreactorsrework.core.config.ConfigValue;
import biggerreactorsrework.core.data.DatapackLoader;
import biggerreactorsrework.core.networking.SimpleChannel;
import biggerreactorsrework.core.registry.OnModLoad;
import biggerreactorsrework.core.serialization.DataCompound;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TurbineCoilRegistry {
    
    
    public static class CoilData {
        public final double efficiency;
        public final double bonus;
        public final double extractionRate;
        
        public CoilData(double efficiency, double bonus, double extractionRate) {
            this.efficiency = efficiency;
            this.bonus = bonus;
            this.extractionRate = extractionRate;
        }
    }
    
    private static final HashMap<Block, CoilData> registry = new HashMap<>();
    
    public static synchronized boolean isBlockAllowed(Block block) {
        return registry.containsKey(block);
    }
    
    public static synchronized CoilData getCoilData(Block block) {
        return registry.get(block);
    }

    public static synchronized Map<Block, CoilData> snapshot() {
        return Map.copyOf(registry);
    }
    // TODO: unify these names across all registries
    private enum RegistryType {
        tag,
        registry,
    }
    
    private static class TurbineCoilJsonData {
        @ConfigValue
        RegistryType type = RegistryType.tag;
        
        @ConfigValue
        Identifier location = Identifier.parse("dirt");
    
        @ConfigValue(range = "(0,)")
        double efficiency;
    
        @ConfigValue(range = "(0,)")
        double extractionRate;
    
        @ConfigValue(range = "(0,2)")
        double bonus;
    }
    
    private static final DatapackLoader<TurbineCoilJsonData> dataLoader = new DatapackLoader<>(TurbineCoilJsonData::new);
    
    public static void loadRegistry() {
        BiggerReactors.LOGGER.info("Loading turbine coils");
        registry.clear();
        
        List<TurbineCoilJsonData> data = dataLoader.loadAll(Identifier.parse("biggerreactorsrework:ebest/coils"));
        BiggerReactors.LOGGER.info("Loaded " + data.size() + " coil data entries");
        
        for (TurbineCoilJsonData coilData : data) {
            
            CoilData properties = new CoilData(coilData.efficiency, coilData.bonus, coilData.extractionRate);
            
            if (coilData.type == RegistryType.tag) {
                var blockTagOptional = BuiltInRegistries.BLOCK.get(TagKey.create(BuiltInRegistries.BLOCK.key(), coilData.location));
                blockTagOptional.ifPresent(holders -> holders.forEach(blockHolder -> {
                    var element = blockHolder.value();
                    registry.put(element, properties);
                    BiggerReactors.LOGGER.debug("Loaded coil " + BuiltInRegistries.BLOCK.getKey(element));
                }));
            } else {
                // cant check against air, because air is a valid thing to load
                if (BuiltInRegistries.BLOCK.containsKey(coilData.location)) {
                    registry.put(BuiltInRegistries.BLOCK.getValue(coilData.location), properties);
                    BiggerReactors.LOGGER.debug("Loaded coil " + coilData.location);
                }
            }
        }
        MaterialDataMaps.coils(registry);
        BiggerReactors.LOGGER.info("Loaded " + registry.size() + " coil entries");
    }
    
    public static class Client {
        
        private static final SimpleChannel CHANNEL = new SimpleChannel(Identifier.fromNamespaceAndPath(BiggerReactors.modid, "coil_sync_channel"), Client::readSync, null);
        private static final ObjectOpenHashSet<Block> coilBlocks = new ObjectOpenHashSet<>();
        private static final Object2ObjectOpenHashMap<Block, CoilData> coilProperties = new Object2ObjectOpenHashMap<>();
        
        @OnModLoad
        private static void onModLoad() {
            NeoForge.EVENT_BUS.addListener(Client::datapackEvent);
            if (FMLEnvironment.getDist().isClient()) {
                NeoForge.EVENT_BUS.addListener(Client::toolTipEvent);
            }
        }
        
        public static void datapackEvent(OnDatapackSyncEvent e) {
            final var player = e.getPlayer();
            if (player == null) {
                return;
            }
            
            if (BiggerReactors.LOG_DEBUG) {
                BiggerReactors.LOGGER.debug("Sending coil list to player: " + player);
            }
            CHANNEL.sendToPlayer(player, writeSync());
        }
        
        private static DataCompound writeSync() {
            final var list = new ObjectArrayList<String>();
            final var propertiesList = new ObjectArrayList<DoubleArrayList>();
            for (final var value : registry.entrySet()) {
                final var location = BuiltInRegistries.BLOCK.getKey(value.getKey());
                if (location == null) {
                    continue;
                }
                list.add(location.toString());
                var properties = new DoubleArrayList();
                properties.add(value.getValue().efficiency);
                properties.add(value.getValue().extractionRate);
                properties.add(value.getValue().bonus);
                propertiesList.add(properties);
            }
            final var compound = new DataCompound();
            compound.put("list", list);
            compound.put("propertiesList", propertiesList);
            return compound;
        }
        
        private static void readSync(DataCompound compound, IPayloadContext context) {
            coilBlocks.clear();
            coilProperties.clear();
            //noinspection unchecked
            final var list = (List<String>) compound.getList("list");
            //noinspection unchecked
            final var propertiesList = (List<DoubleArrayList>) compound.getList("propertiesList");
            if (BiggerReactors.LOG_DEBUG) {
                BiggerReactors.LOGGER.debug("Received coil list from server with length of " + list.size());
            }
            for (int i = 0; i < list.size(); i++) {
                var value = list.get(i);
                var properties = propertiesList.get(i);
                var id=Identifier.tryParse(value);
                if(id==null||!BuiltInRegistries.BLOCK.containsKey(id))continue;
                final var block = BuiltInRegistries.BLOCK.getValue(id);
                if (block == null) {
                    return;
                }
                if (BiggerReactors.LOG_DEBUG) {
                    BiggerReactors.LOGGER.debug("Block " + value + " added as coil on client");
                }
                coilBlocks.add(block);
                coilProperties.put(block, new CoilData(properties.getDouble(0), properties.getDouble(1), properties.getDouble(2)));
            }
        }
        
        public static void toolTipEvent(ItemTooltipEvent event) {
            final var item = event.getItemStack().getItem();
            if (item instanceof BlockItem blockItem) {
                if (!coilBlocks.contains(blockItem.getBlock())) {
                    return;
                }
            } else {
                return;
            }
            if (Minecraft.getInstance().options.advancedItemTooltips || Config.CONFIG.AlwaysShowTooltips) {
                event.getToolTip().add(Component.translatable("tooltip.biggerreactorsrework.is_a_coil"));
            }
        }
        
        public static Map<Block, CoilData> getImmutableRegistry() {
            return Collections.unmodifiableMap(coilProperties);
        }
    }
}
