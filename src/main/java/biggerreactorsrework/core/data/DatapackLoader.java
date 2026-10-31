package biggerreactorsrework.core.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import biggerreactorsrework.core.CoreEvents;
import biggerreactorsrework.core.config.ConfigType;
import biggerreactorsrework.core.config.spec.ConfigOptionsDefaults;
import biggerreactorsrework.core.config.spec.SpecObjectNode;
import biggerreactorsrework.core.parsers.JSON5;
import biggerreactorsrework.core.util.NonnullDefault;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@NonnullDefault
public class DatapackLoader<T> {
    
    private final SpecObjectNode baseSpecNode;
    private final Supplier<T> objectSupplier;
    
    public DatapackLoader(Supplier<T> objectSupplier) {
        final var templateObject = objectSupplier.get();
        baseSpecNode = new SpecObjectNode(templateObject, "", ConfigType.NULL, new ConfigOptionsDefaults(ConfigType.NULL, false, false, false));
        this.objectSupplier = objectSupplier;
    }
    
    public Map<Identifier, List<T>> loadAllMappedStack(Identifier baseResourceLocation) {
        if (CoreEvents.serverResourceManager == null) {
            return new Object2ObjectOpenHashMap<>();
        }
        
        final var map = new Object2ObjectOpenHashMap<Identifier, List<T>>();
        
        final var resourceLocations = CoreEvents.serverResourceManager.listResourceStacks(baseResourceLocation.getPath(), s -> s.getPath().contains(".json"));
        
        for (final var entry : resourceLocations.entrySet()) {
            final var resourceLocation = entry.getKey();
            // TODO: 9/8/22 add an option for enforcing this
            if (!resourceLocation.getNamespace().equals(baseResourceLocation.getNamespace())) {
                continue;
            }
            final var list = new ObjectArrayList<T>();
            for (Resource resource : entry.getValue()) {
                T t = load(resourceLocation, resource);
                if (t != null) {
                    list.add(t);
                }
            }
            if (list.isEmpty()) {
                continue;
            }
            map.put(resourceLocation, list);
        }
        
        return map;
    }
    
    public List<T> loadAll(Identifier baseResourceLocation) {
        if (CoreEvents.serverResourceManager == null) {
            return new ObjectArrayList<>();
        }
        
        final var list = new ObjectArrayList<T>();
        
        Map<Identifier, Resource> resourceLocations = CoreEvents.serverResourceManager.listResources(baseResourceLocation.getPath(), s -> s.getPath().contains(".json"));
        
        for (Map.Entry<Identifier, Resource> entry : resourceLocations.entrySet()) {
            final var resourceLocation = entry.getKey();
            // TODO: 9/8/22 add an option for enforcing this
            if (!resourceLocation.getNamespace().equals(baseResourceLocation.getNamespace())) {
                continue;
            }
            T t = load(resourceLocation, entry.getValue());
            if (t != null) {
                list.add(t);
            }
        }
        
        return list;
    }
    
    @Nullable
    public T load(Identifier location) {
        if (CoreEvents.serverResourceManager == null) {
            return null;
        }
        return load(location, CoreEvents.serverResourceManager.getResource(location).orElseThrow());
    }
    
    @Nullable
    private T load(Identifier location, Resource resource) {
        String json;
        try {
            try (BufferedReader reader = resource.openAsReader()) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                    builder.append("\n");
                }
                json = builder.toString();
            }
            
        } catch (IOException e) {
            CoreEvents.LOGGER.error("Error reading json at " + location.toString());
            e.printStackTrace();
            return null;
        }
        
        final var object = objectSupplier.get();
        
        final var elements = JSON5.parseString(json);
        
        if (!baseSpecNode.setActiveObject(object)) {
            return null;
        }
        
        if (elements == null) {
            return object;
        }
        
        final var correctedElements = baseSpecNode.correctToValidState(elements);
        
        if (correctedElements == null) {
            return object;
        }
        
        baseSpecNode.writeElement(correctedElements);
        
        return object;
    }
}
