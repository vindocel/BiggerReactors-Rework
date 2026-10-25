package biggerreactorsrework.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local appearance choices; independent of Minecraft so persistence and scope can be tested. */
final class GuiThemePreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private boolean applyToAll = true;
    private boolean allDark;
    private final Map<String, Boolean> machines = new LinkedHashMap<>();

    boolean applyToAll() { return applyToAll; }

    boolean dark(String machine) {
        return applyToAll ? allDark : machines.getOrDefault(machine, allDark);
    }

    void choose(String machine, boolean dark) {
        if (applyToAll) allDark = dark;
        else machines.put(machine, dark);
    }

    void setApplyToAll(String machine, boolean value) {
        boolean current = dark(machine);
        applyToAll = value;
        if (value) allDark = current;
        else machines.put(machine, current);
    }

    static GuiThemePreferences read(Reader reader) {
        var result = new GuiThemePreferences();
        JsonObject json = GSON.fromJson(reader, JsonObject.class);
        if (json == null) return result;
        if (json.has("applyToAll")) result.applyToAll = json.get("applyToAll").getAsBoolean();
        if (json.has("allDark")) result.allDark = json.get("allDark").getAsBoolean();
        if (json.has("machines")) {
            json.getAsJsonObject("machines").entrySet().forEach(entry -> {
                String value = entry.getValue().getAsString();
                if (!value.equals("light") && !value.equals("dark")) {
                    throw new IllegalArgumentException("Unknown machine theme: " + value);
                }
                result.machines.put(entry.getKey(), value.equals("dark"));
            });
        }
        return result;
    }

    void write(Writer writer) {
        JsonObject json = new JsonObject();
        json.addProperty("applyToAll", applyToAll);
        json.addProperty("allDark", allDark);
        JsonObject choices = new JsonObject();
        machines.forEach((machine, dark) -> choices.addProperty(machine, dark ? "dark" : "light"));
        json.add("machines", choices);
        GSON.toJson(json, writer);
    }
}
