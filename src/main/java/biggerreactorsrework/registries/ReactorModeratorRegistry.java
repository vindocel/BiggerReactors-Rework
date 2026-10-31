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
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
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
import biggerreactorsrework.core.robn.ROBNObject;
import biggerreactorsrework.core.serialization.DataCompound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;


public class ReactorModeratorRegistry {
    
    public interface IModeratorProperties extends ROBNObject {
        double absorption();
        
        double heatEfficiency();
        
        double moderation();
        
        double heatConductivity();
        
        @Override
        default Map<String, Object> toROBNMap() {
            final Map<String, Object> map = new HashMap<>();
            map.put("absorption", absorption());
            map.put("heatEfficiency", heatEfficiency());
            map.put("moderation", moderation());
            map.put("heatConductivity", heatConductivity());
            return map;
        }
        
        @Override
        default void fromROBNMap(Map<String, Object> map) {
            throw new IllegalArgumentException("");
        }
    }
    
    public static class ModeratorProperties implements IModeratorProperties, ROBNObject {
        
        public static final ModeratorProperties EMPTY_MODERATOR = new ModeratorProperties(0, 0, 1, 0);
        
        public final double absorption;
        public final double heatEfficiency;
        public final double moderation;
        public final double heatConductivity;
        
        public ModeratorProperties(double absorption, double heatEfficiency, double moderation, double heatConductivity) {
            this.absorption = absorption;
            this.heatEfficiency = heatEfficiency;
            this.moderation = moderation;
            this.heatConductivity = heatConductivity;
        }
        
        public ModeratorProperties(IModeratorProperties properties) {
            this(properties.absorption(), properties.heatEfficiency(), properties.moderation(), properties.heatConductivity());
        }
        
        @Override
        public double absorption() {
            return absorption;
        }
        
        @Override
        public double heatEfficiency() {
            return heatEfficiency;
        }
        
        @Override
        public double moderation() {
            return moderation;
        }
        
        @Override
        public double heatConductivity() {
            return heatConductivity;
        }
        
        
    }
    
    private final static HashMap<Block, ModeratorProperties> registry = new HashMap<>();
    
    public static boolean isBlockAllowed(Block block) {
        return registry.containsKey(block);
    }
    
    public static ModeratorProperties blockModeratorProperties(Block block) {
        return registry.get(block);
    }
    public static Map<Block,ModeratorProperties> snapshot(){return Map.copyOf(registry);}
    
    // TODO: unify these names across all registries
    private enum RegistryType {
        tag,
        registry,
        fluidtag,
        fluid
    }
    
    private static class ReactorModeratorJsonData {
    
        @ConfigValue
        RegistryType type = RegistryType.tag;
    
        @ConfigValue
        Identifier location = Identifier.parse("dirt");
    
        @ConfigValue(range = "[0, 1]")
        double absorption;
    
        @ConfigValue(range = "[0, 1]")
        double efficiency;
    
        @ConfigValue(range = "[1,)")
        double moderation;
    
        @ConfigValue(range = "[0,)")
        double conductivity;
    }
    
    private static final DatapackLoader<ReactorModeratorJsonData> dataLoader = new DatapackLoader<>(ReactorModeratorJsonData::new);
    
    public static void loadRegistry() {
        BiggerReactors.LOGGER.info("Loading reactor moderators");
        registry.clear();
        
        List<ReactorModeratorJsonData> data = dataLoader.loadAll(Identifier.parse("biggerreactorsrework:ebcr/moderators"));
        BiggerReactors.LOGGER.info("Loaded " + data.size() + " moderator data entries");
        
        for (ReactorModeratorJsonData moderatorData : data) {
            
            ModeratorProperties properties = new ModeratorProperties(moderatorData.absorption, moderatorData.efficiency, moderatorData.moderation, moderatorData.conductivity);
    
            switch (moderatorData.type) {
                case tag -> {
                    var blockTagOptional = BuiltInRegistries.BLOCK.get(TagKey.create(BuiltInRegistries.BLOCK.key(), moderatorData.location));
                    blockTagOptional.ifPresent(holders -> holders.forEach(blockHolder -> {
                        var element = blockHolder.value();
                        registry.put(element, properties);
                        BiggerReactors.LOGGER.debug("Loaded moderator " + BuiltInRegistries.BLOCK.getKey(element));
                    }));
                }
                case registry -> {
                    // cant check against air, because air is a valid thing to load
                    if (BuiltInRegistries.BLOCK.containsKey(moderatorData.location)) {
                        registry.put(BuiltInRegistries.BLOCK.getValue(moderatorData.location), properties);
                        BiggerReactors.LOGGER.debug("Loaded moderator " + moderatorData.location);
                    }
                }
                case fluidtag -> {
                    var fluidTagOptional = BuiltInRegistries.FLUID.get(TagKey.create(BuiltInRegistries.FLUID.key(), moderatorData.location));
                    fluidTagOptional.ifPresent(holders -> holders.forEach(fluidHolder -> {
                        var element = fluidHolder.value();
                        Block elementBlock = element.defaultFluidState().createLegacyBlock().getBlock();
                        registry.put(elementBlock, properties);
                        BiggerReactors.LOGGER.debug("Loaded moderator " + BuiltInRegistries.FLUID.getKey(element));
                    }));
                }
                case fluid -> {
                    // cant check against air, because air is a valid thing to load
                    if (BuiltInRegistries.FLUID.containsKey(moderatorData.location)) {
                        Fluid fluid = BuiltInRegistries.FLUID.getValue(moderatorData.location);
                        assert fluid != null;
                        Block block = fluid.defaultFluidState().createLegacyBlock().getBlock();
                        registry.put(block, properties);
                        BiggerReactors.LOGGER.debug("Loaded moderator " + moderatorData.location);
                    }
                }
            }
        }
        MaterialDataMaps.moderators(registry);
        // Rework 2026-10-25: fluids are transported inside manifolds, never world-space moderators.
        registry.keySet().removeIf(block -> block instanceof net.minecraft.world.level.block.LiquidBlock ||
                !block.defaultBlockState().getFluidState().isEmpty());
        BiggerReactors.LOGGER.info("Loaded " + registry.size() + " moderator entries");
    }
    
    public static class Client {
        
        private static final SimpleChannel CHANNEL = new SimpleChannel(Identifier.fromNamespaceAndPath(BiggerReactors.modid, "moderator_sync_channel"), Client::readSync, null);
        private static final ObjectOpenHashSet<Block> moderatorBlocks = new ObjectOpenHashSet<>();
        private static final Object2ObjectOpenHashMap<Block, ModeratorProperties> moderatorProperties = new Object2ObjectOpenHashMap<>();
        
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
                BiggerReactors.LOGGER.debug("Sending moderator list to player: " + player);
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
                properties.add(value.getValue().absorption);
                properties.add(value.getValue().heatEfficiency);
                properties.add(value.getValue().moderation);
                properties.add(value.getValue().heatConductivity);
                propertiesList.add(properties);
            }
            final var compound = new DataCompound();
            compound.put("list", list);
            compound.put("propertiesList", propertiesList);
            return compound;
        }
        
        private static void readSync(DataCompound compound, IPayloadContext context) {
            moderatorBlocks.clear();
            moderatorProperties.clear();
            //noinspection unchecked
            final var list = (List<String>) compound.getList("list");
            //noinspection unchecked
            final var propertiesList = (List<DoubleArrayList>) compound.getList("propertiesList");
            if (BiggerReactors.LOG_DEBUG) {
                BiggerReactors.LOGGER.debug("Received moderator list from server with length of " + list.size());
            }
            for (int i = 0; i < list.size(); i++) {
                var blockLocation = list.get(i);
                var properties = propertiesList.get(i);
                var id=Identifier.tryParse(blockLocation);
                if(id==null||!BuiltInRegistries.BLOCK.containsKey(id))continue;
                final var block = BuiltInRegistries.BLOCK.getValue(id);
                if (block == null) {
                    return;
                }
                if (BiggerReactors.LOG_DEBUG) {
                    BiggerReactors.LOGGER.debug("Block " + block + " added as moderator on client");
                }
                moderatorBlocks.add(block);
                moderatorProperties.put(block, new ModeratorProperties(properties.getDouble(0), properties.getDouble(1), properties.getDouble(2), properties.getDouble(3)));
            }
        }
        
        public static void toolTipEvent(ItemTooltipEvent event) {
            final var item = event.getItemStack().getItem();
            if (item instanceof BlockItem blockItem) {
                if (!moderatorBlocks.contains(blockItem.getBlock())) {
                    return;
                }
            } else if (item instanceof BucketItem bucketItem) {
                final var fluidBlock = bucketItem.content.defaultFluidState().createLegacyBlock().getBlock();
                if (fluidBlock.defaultBlockState().isAir() || !moderatorBlocks.contains(fluidBlock)) {
                    return;
                }
            } else {
                return;
            }
            if (Minecraft.getInstance().options.advancedItemTooltips || Config.CONFIG.AlwaysShowTooltips) {
                event.getToolTip().add(Component.translatable("tooltip.biggerreactorsrework.is_a_moderator"));
            }
        }
        
        public static void forEach(BiConsumer<Block, IModeratorProperties> consumer) {
            moderatorProperties.forEach(consumer);
        }
    }
}
