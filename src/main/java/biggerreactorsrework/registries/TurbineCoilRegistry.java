package biggerreactorsrework.registries;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
//? if =26.1.2 || =26.3 {
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
//?} else if =1.21.1 {
/*import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
*/
//?}
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

        // Added 2026-11-03: the turbine's rounded generation rule derived from the material data.
        public double fePerMB() { return Math.round(efficiency * 32) / 4.0; }
        public long steamPerTick() { return Math.round(extractionRate * 5000); }
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

//? if =26.1.2 || =26.3 {
        @ConfigValue
        Identifier location = Identifier.parse("dirt");

        @ConfigValue(range = "(0,)")
//?} else if =1.21.1 {
/*        @ConfigValue
        ResourceLocation location = ResourceLocation.parse("dirt");

        @ConfigValue(range = "(0,)")
*/
//?}
        double efficiency;

        @ConfigValue(range = "(0,)")
        double extractionRate;

        @ConfigValue(range = "(0,2)")
        double bonus;
    }

    private static final DatapackLoader<TurbineCoilJsonData> dataLoader = new DatapackLoader<>(TurbineCoilJsonData::new);

    public static void loadRegistry() {
        BiggerReactors.LOGGER.info("Loading turbine coils");
//? if =26.1.2 || =26.3 {
        registry.clear();

        List<TurbineCoilJsonData> data = dataLoader.loadAll(Identifier.parse("biggerreactorsrework:ebest/coils"));
        BiggerReactors.LOGGER.info("Loaded " + data.size() + " coil data entries");
//?} else if =1.21.1 {
/*        registry.clear();

        List<TurbineCoilJsonData> data = dataLoader.loadAll(ResourceLocation.parse("biggerreactorsrework:ebest/coils"));
        BiggerReactors.LOGGER.info("Loaded " + data.size() + " coil data entries");
*/
//?}

        for (TurbineCoilJsonData coilData : data) {

            CoilData properties = new CoilData(coilData.efficiency, coilData.bonus, coilData.extractionRate);

//? if =26.1.2 || =26.3 {
            if (coilData.type == RegistryType.tag) {
                var blockTagOptional = BuiltInRegistries.BLOCK.get(TagKey.create(BuiltInRegistries.BLOCK.key(), coilData.location));
                blockTagOptional.ifPresent(holders -> holders.forEach(blockHolder -> {
//?} else if =1.21.1 {
/*            if (coilData.type == RegistryType.tag) {
                var blockTagOptional = BuiltInRegistries.BLOCK.getTag(TagKey.create(BuiltInRegistries.BLOCK.key(), coilData.location));
                blockTagOptional.ifPresent(holders -> holders.forEach(blockHolder -> {
*/
//?}
                    var element = blockHolder.value();
                    registry.put(element, properties);
                    BiggerReactors.LOGGER.debug("Loaded coil " + BuiltInRegistries.BLOCK.getKey(element));
                }));
            } else {
                // cant check against air, because air is a valid thing to load
//? if =26.1.2 || =26.3 {
                if (BuiltInRegistries.BLOCK.containsKey(coilData.location)) {
                    registry.put(BuiltInRegistries.BLOCK.getValue(coilData.location), properties);
                    BiggerReactors.LOGGER.debug("Loaded coil " + coilData.location);
//?} else if =1.21.1 {
/*                if (BuiltInRegistries.BLOCK.containsKey(coilData.location)) {
                    registry.put(BuiltInRegistries.BLOCK.get(coilData.location), properties);
                    BiggerReactors.LOGGER.debug("Loaded coil " + coilData.location);
*/
//?}
                }
            }
        }
        MaterialDataMaps.coils(registry);
        BiggerReactors.LOGGER.info("Loaded " + registry.size() + " coil entries");
    }

//? if =26.1.2 || =26.3 {
    public static class Client {

        private static final SimpleChannel CHANNEL = new SimpleChannel(Identifier.fromNamespaceAndPath(BiggerReactors.modid, "coil_sync_channel"), Client::readSync, null);
        private static final ObjectOpenHashSet<Block> coilBlocks = new ObjectOpenHashSet<>();
//?} else if =1.21.1 {
/*    public static class Client {

        private static final SimpleChannel CHANNEL = new SimpleChannel(ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "coil_sync_channel"), Client::readSync, null);
        private static final ObjectOpenHashSet<Block> coilBlocks = new ObjectOpenHashSet<>();
*/
//?}
        private static final Object2ObjectOpenHashMap<Block, CoilData> coilProperties = new Object2ObjectOpenHashMap<>();

        @OnModLoad
        private static void onModLoad() {
//? if =26.1.2 || =26.3 {
            NeoForge.EVENT_BUS.addListener(Client::datapackEvent);
            if (FMLEnvironment.getDist().isClient()) {
                NeoForge.EVENT_BUS.addListener(Client::toolTipEvent);
//?} else if =1.21.1 {
/*            NeoForge.EVENT_BUS.addListener(Client::datapackEvent);
            if (FMLEnvironment.dist.isClient()) {
                NeoForge.EVENT_BUS.addListener(Client::toolTipEvent);
*/
//?}
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
//? if =26.1.2 || =26.3 {
                var properties = propertiesList.get(i);
                var id=Identifier.tryParse(value);
                if(id==null||!BuiltInRegistries.BLOCK.containsKey(id))continue;
                final var block = BuiltInRegistries.BLOCK.getValue(id);
                if (block == null) {
//?} else if =1.21.1 {
/*                var properties = propertiesList.get(i);
                var id=ResourceLocation.tryParse(value);
                if(id==null||!BuiltInRegistries.BLOCK.containsKey(id))continue;
                final var block = BuiltInRegistries.BLOCK.get(id);
                if (block == null) {
*/
//?}
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
                var data = coilProperties.get(((BlockItem) item).getBlock());
                if (data == null) return;
                int percent = relativeEfficiency(data);
                if (percent > 0) event.getToolTip().add(Component.translatable("tooltip.biggerreactorsrework.turbine_efficiency", percent)
                        .withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
        }

        // Modified 2026-11-18: approximate share of the best registered coil's FE per steam mB, in steps of 5%.
        private static int relativeEfficiency(CoilData data) {
            double best = 0;
            for (var other : coilProperties.values()) best = Math.max(best, other.fePerMB());
            if (best <= 0 || data.fePerMB() <= 0) return 0;
            return (int) Math.max(5, Math.round(data.fePerMB() / best * 20) * 5);
        }

        public static Map<Block, CoilData> getImmutableRegistry() {
            return Collections.unmodifiableMap(coilProperties);
        }
    }
}
