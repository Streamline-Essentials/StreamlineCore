package host.plas.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.StreamQuests;
import host.plas.data.shop.ShopCategory;
import host.plas.data.shop.ShopEntry;
import host.plas.data.shop.ShopReward;
import host.plas.data.shop.ShopRewardType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code questshop.yml}: the quest shop's settings, messages, categories and entries. Values are
 * read without writing defaults back, so the shipped file keeps its comments. Keys come back
 * sorted alphabetically, so categories and entries are ordered by their {@code order} value
 * (then price), and rewards are a YAML list given top to bottom.
 */
public class ShopConfig extends SimpleConfiguration {
    @Getter @Setter private static List<ShopCategory> categories = new ArrayList<>();

    @Getter private static String title = "&6&lQuest Shop";
    @Getter private static boolean confirmPurchases = true;
    @Getter private static boolean hideUnavailable = true;

    @Getter private static String messagePurchased = "&aYou bought &f%item% &afor &e%price% &apoints&8.";
    @Getter private static String messageNotEnoughPoints = "&cYou need &e%missing% &cmore points for &f%item%&c.";
    @Getter private static String messageNoPermission = "&cYou cannot buy &f%item%&c.";
    @Getter private static String messageUnavailable = "&f%item% &cis not available on this server.";
    @Getter private static String messageNotLoaded = "&cYour quest points are still loading. Try again in a moment.";

    public ShopConfig() {
        super("questshop.yml", StreamQuests.getInstance(), true);
    }

    @Override
    public void init() {
        reloadResource();

        title = string("settings.title", title);
        confirmPurchases = bool("settings.confirm-purchases", confirmPurchases);
        hideUnavailable = bool("settings.hide-unavailable", hideUnavailable);

        messagePurchased = string("messages.purchased", messagePurchased);
        messageNotEnoughPoints = string("messages.not-enough-points", messageNotEnoughPoints);
        messageNoPermission = string("messages.no-permission", messageNoPermission);
        messageUnavailable = string("messages.unavailable", messageUnavailable);
        messageNotLoaded = string("messages.not-loaded", messageNotLoaded);

        setCategories(readCategories());
    }

    public void reloadConfig() {
        init();
    }

    private List<ShopCategory> readCategories() {
        Map<String, ShopCategory> categories = new LinkedHashMap<>();
        for (String key : singleLayerKeySet("categories")) {
            String path = "categories." + key;
            ShopCategory category = new ShopCategory(key);
            category.setName(string(path + ".name", category.getName()));
            category.setIcon(string(path + ".icon", category.getIcon()));
            category.setDescription(lines(path + ".description"));
            category.setOrder(integer(path + ".order", 0));
            categories.put(key, category);
        }

        for (String key : singleLayerKeySet("items")) {
            try {
                ShopEntry entry = readEntry(key);
                ShopCategory category = categories.get(entry.getCategory());
                if (category == null) {
                    StreamQuests.getInstance().logWarning("Quest shop entry '" + key + "' names unknown category '"
                            + entry.getCategory() + "'; it is not listed.");
                    continue;
                }
                category.getEntries().add(entry);
            } catch (RuntimeException e) {
                StreamQuests.getInstance().logWarning("Skipping quest shop entry '" + key + "': " + e.getMessage());
            }
        }

        List<ShopCategory> sorted = new ArrayList<>(categories.values());
        sorted.sort(Comparator.comparingInt(ShopCategory::getOrder).thenComparing(ShopCategory::getIdentifier));
        for (ShopCategory category : sorted) {
            category.getEntries().sort(Comparator.comparingInt(ShopEntry::getOrder)
                    .thenComparingDouble(ShopEntry::getPrice)
                    .thenComparing(ShopEntry::getIdentifier));
        }
        return sorted;
    }

    private ShopEntry readEntry(String key) {
        String path = "items." + key;
        ShopEntry entry = new ShopEntry(key);
        entry.setCategory(string(path + ".category", "misc"));
        entry.setName(string(path + ".name", entry.getName()));
        entry.setIcon(string(path + ".icon", entry.getIcon()));
        entry.setIconAmount(integer(path + ".icon-amount", 1));
        entry.setGlowing(bool(path + ".glowing", false));
        entry.setDescription(lines(path + ".description"));
        entry.setPrice(number(path + ".price", 0));
        entry.setPermission(string(path + ".permission", ""));
        entry.setOrder(integer(path + ".order", 0));

        Object raw = getResource().get(path + ".rewards");
        if (! (raw instanceof Collection)) throw new IllegalArgumentException("'rewards' must be a list");
        int index = 0;
        for (Object element : (Collection<?>) raw) {
            index++;
            if (! (element instanceof Map)) {
                StreamQuests.getInstance().logWarning("Quest shop entry '" + key + "' reward #" + index + " is not a section; skipped.");
                continue;
            }
            ShopReward reward = readReward((Map<?, ?>) element);
            if (reward == null) {
                StreamQuests.getInstance().logWarning("Quest shop entry '" + key + "' reward #" + index
                        + " has a missing or unknown type (COMMAND, ITEM, EXP, MESSAGE, BROADCAST, POTION); skipped.");
                continue;
            }
            entry.getRewards().add(reward);
        }
        if (entry.getRewards().isEmpty()) throw new IllegalArgumentException("it has no usable rewards");
        return entry;
    }

    private static ShopReward readReward(Map<?, ?> map) {
        ShopRewardType type = ShopRewardType.parse(asString(map.get("type"))).orElse(null);
        if (type == null) return null;

        ShopReward reward = new ShopReward(type);
        if (map.containsKey("display")) reward.setDisplay(asString(map.get("display")));
        switch (type) {
            case COMMAND:
                reward.setLines(asLines(map.containsKey("commands") ? map.get("commands") : map.get("command")));
                break;
            case MESSAGE:
            case BROADCAST:
                reward.setLines(asLines(map.containsKey("messages") ? map.get("messages") : map.get("message")));
                break;
            case ITEM:
                reward.setItem(required(map, "item"));
                reward.setAmount(asInt(map.get("amount"), 1));
                reward.setData(map.containsKey("data") ? asString(map.get("data")) : "");
                break;
            case EXP:
                reward.setAmount(asInt(map.get("amount"), 1));
                reward.setLevels(asBool(map.get("levels"), true));
                break;
            case POTION:
                reward.setEffect(required(map, "effect"));
                reward.setDuration(asInt(map.get("duration"), 60));
                reward.setLevel(asInt(map.get("level"), 1));
                reward.setHideParticles(asBool(map.get("hide-particles"), false));
                break;
        }
        return reward;
    }

    private String string(String path, String def) {
        Object value = getResource().get(path);
        return value == null ? def : String.valueOf(value);
    }

    private boolean bool(String path, boolean def) {
        return asBool(getResource().get(path), def);
    }

    private int integer(String path, int def) {
        return asInt(getResource().get(path), def);
    }

    private double number(String path, double def) {
        Object value = getResource().get(path);
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return value == null ? def : Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private List<String> lines(String path) {
        return asLines(getResource().get(path));
    }

    private static String required(Map<?, ?> map, String key) {
        String value = asString(map.get(key));
        if (value == null || value.isBlank()) throw new IllegalArgumentException("a reward is missing '" + key + "'");
        return value.trim();
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static List<String> asLines(Object value) {
        List<String> lines = new ArrayList<>();
        if (value instanceof Collection) {
            for (Object line : (Collection<?>) value) if (line != null) lines.add(String.valueOf(line));
        } else if (value != null) {
            lines.add(String.valueOf(value));
        }
        return lines;
    }

    private static int asInt(Object value, int def) {
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return value == null ? def : Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean asBool(Object value, boolean def) {
        if (value instanceof Boolean) return (Boolean) value;
        return value == null ? def : Boolean.parseBoolean(String.valueOf(value).trim());
    }
}
