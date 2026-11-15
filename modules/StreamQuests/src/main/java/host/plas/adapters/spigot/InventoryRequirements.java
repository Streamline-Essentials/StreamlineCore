package host.plas.adapters.spigot;

import gg.drak.thebase.lib.re2j.Matcher;
import gg.drak.thebase.utils.MatcherUtils;
import host.plas.data.require.Requirement;
import host.plas.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * {@code ITEMS_IN_INVENTORY} requirements, whose items are Bukkit-serialized {@link ItemStack}s.
 * Only reached on Bukkit ({@link SpigotAdapter#isBukkit()}), since it links against Bukkit.
 */
public final class InventoryRequirements {
    private InventoryRequirements() {}

    public static boolean has(String playerUuid, Requirement requirement) {
        Player player = Bukkit.getPlayer(UUID.fromString(playerUuid));
        if (player == null) return false;

        return getItems(requirement).values().stream().allMatch(item -> {
            if (requirement.getAmount() < 0) {
                return player.getInventory().contains(item);
            } else {
                return player.getInventory().containsAtLeast(item, (int) requirement.getAmount());
            }
        });
    }

    public static ConcurrentSkipListMap<Integer, ItemStack> getItems(Requirement requirement) {
        ConcurrentSkipListMap<Integer, ItemStack> items = new ConcurrentSkipListMap<>();

        Matcher matcher = MatcherUtils.matcherBuilder("item:nbt=(.*?);!", requirement.getValue());
        List<String[]> groups = MatcherUtils.getGroups(matcher, 3);
        for (String[] group : groups) {
            try {
                ItemStack item = ItemUtils.deserialize(group[0]);
                items.put(items.size(), item);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        return items;
    }

    public static void loadItems(Requirement requirement, List<ItemStack> items) {
        StringBuilder builder = new StringBuilder();
        for (ItemStack item : items) {
            String serialized = ItemUtils.serialize(item);
            builder.append("item:nbt=").append(serialized).append(";!");
        }

        requirement.setValue(builder.toString());
    }
}
