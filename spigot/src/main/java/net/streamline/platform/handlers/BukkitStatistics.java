package net.streamline.platform.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import singularity.gui.CosmicItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Reads vanilla statistics through Bukkit's {@link Statistic} API, which answers for online
 * players from memory and for offline players from their stats file. Must run on the main thread.
 */
final class BukkitStatistics {
    private BukkitStatistics() {
    }

    static Map<String, Long> read(String uuid, String type, Collection<String> ids) {
        Map<String, Long> values = new HashMap<>();
        UUID id;
        try {
            id = UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            return values;
        }

        OfflinePlayer player = Bukkit.getPlayer(id);
        if (player == null) player = Bukkit.getOfflinePlayer(id);
        if (! player.isOnline() && ! player.hasPlayedBefore()) return values;

        String kind = CosmicItem.keyPath(type);
        for (String raw : expand(kind, ids)) {
            String key = CosmicItem.normalizeKey(raw);
            try {
                long value = value(player, kind, key);
                if (value > 0) values.put(key, value);
            } catch (IllegalArgumentException | UnsupportedOperationException ignored) {
                // The id does not fit this statistic, such as a non-block for "mined".
            }
        }
        return values;
    }

    /** The ids asked for, with {@code *} replaced by every id the statistic type has. */
    private static Collection<String> expand(String kind, Collection<String> ids) {
        if (! ids.contains("*")) return ids;
        Set<String> all = new HashSet<>();
        for (String raw : ids) {
            if (! raw.equals("*")) all.add(raw);
        }
        switch (kind) {
            case "killed":
            case "killed_by":
                for (EntityType type : EntityType.values()) {
                    if (type != EntityType.UNKNOWN && type.isAlive()) all.add(type.getKey().toString());
                }
                break;
            case "custom":
                for (Statistic statistic : Statistic.values()) {
                    if (statistic.getType() == Statistic.Type.UNTYPED) all.add(statistic.getKey().toString());
                }
                break;
            default:
                for (Material material : Material.values()) {
                    if (material.isLegacy()) continue;
                    if (kind.equals("mined") ? material.isBlock() : material.isItem()) all.add(material.getKey().toString());
                }
                break;
        }
        return all;
    }

    private static long value(OfflinePlayer player, String kind, String key) {
        switch (kind) {
            case "mined": {
                Material block = Material.matchMaterial(key);
                return block == null || ! block.isBlock() ? 0 : player.getStatistic(Statistic.MINE_BLOCK, block);
            }
            case "picked_up":
                return item(player, Statistic.PICKUP, key);
            case "crafted":
                return item(player, Statistic.CRAFT_ITEM, key);
            case "used":
                return item(player, Statistic.USE_ITEM, key);
            case "broken":
                return item(player, Statistic.BREAK_ITEM, key);
            case "dropped":
                return item(player, Statistic.DROP, key);
            case "killed":
                return entity(player, Statistic.KILL_ENTITY, key);
            case "killed_by":
                return entity(player, Statistic.ENTITY_KILLED_BY, key);
            case "custom": {
                for (Statistic statistic : Statistic.values()) {
                    if (statistic.getType() == Statistic.Type.UNTYPED && statistic.getKey().toString().equals(key)) {
                        return player.getStatistic(statistic);
                    }
                }
                return 0;
            }
            default:
                return 0;
        }
    }

    private static long item(OfflinePlayer player, Statistic statistic, String key) {
        Material item = Material.matchMaterial(key);
        return item == null || ! item.isItem() ? 0 : player.getStatistic(statistic, item);
    }

    private static long entity(OfflinePlayer player, Statistic statistic, String key) {
        for (EntityType type : EntityType.values()) {
            if (type == EntityType.UNKNOWN) continue;
            if (type.getKey().toString().equals(key)) return player.getStatistic(statistic, type);
        }
        return 0;
    }
}
