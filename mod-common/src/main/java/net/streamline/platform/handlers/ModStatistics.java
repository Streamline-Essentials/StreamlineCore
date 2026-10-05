package net.streamline.platform.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.StatType;
import net.minecraft.world.level.storage.LevelResource;
import net.streamline.platform.BasePlugin;
import singularity.gui.CosmicItem;
import singularity.utils.MessageUtils;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Reads vanilla statistics: from the player's live counter when online, otherwise from
 * {@code <world>/stats/<uuid>.json}. Ids are matched as strings against each registry's keys,
 * which keeps this free of the key class Minecraft renamed in 1.21.11. Must run on the server
 * thread.
 */
final class ModStatistics {
    private ModStatistics() {
    }

    static Map<String, Long> read(MinecraftServer server, String uuid, String type, Collection<String> ids) {
        Map<String, Long> values = new HashMap<>();
        String typeKey = CosmicItem.normalizeKey(type);
        Set<String> wanted = new HashSet<>();
        for (String id : ids) wanted.add(CosmicItem.normalizeKey(id));

        ServerPlayer online = BasePlugin.getPlayer(uuid);
        if (online != null) {
            for (StatType<?> statType : BuiltInRegistries.STAT_TYPE) {
                if (BuiltInRegistries.STAT_TYPE.getKey(statType).toString().equals(typeKey)) {
                    collect(online.getStats(), statType, wanted, values);
                    break;
                }
            }
            return values;
        }

        Path file = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(uuid + ".json");
        if (! Files.isRegularFile(file)) return values;

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (! root.isJsonObject()) return values;
            JsonObject stats = root.getAsJsonObject().getAsJsonObject("stats");
            if (stats == null) return values;
            JsonObject ofType = stats.getAsJsonObject(typeKey);
            if (ofType == null) return values;
            for (String key : wanted) {
                JsonElement value = ofType.get(key);
                if (value != null && value.isJsonPrimitive() && value.getAsLong() > 0) values.put(key, value.getAsLong());
            }
        } catch (Exception e) {
            MessageUtils.logWarning("Could not read the statistics file " + file + ": " + e.getMessage());
        }
        return values;
    }

    private static <T> void collect(ServerStatsCounter stats, StatType<T> type, Set<String> wanted, Map<String, Long> values) {
        Registry<T> registry = type.getRegistry();
        for (T entry : registry) {
            String key = registry.getKey(entry).toString();
            if (! wanted.contains(key)) continue;
            int value = stats.getValue(type, entry);
            if (value > 0) values.put(key, (long) value);
        }
    }
}
