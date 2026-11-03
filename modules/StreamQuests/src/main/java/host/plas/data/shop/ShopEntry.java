package host.plas.data.shop;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Something players can buy from {@code /questshop} with quest points: its look, price and rewards. */
@Getter @Setter
public class ShopEntry {
    private final String identifier;
    private String category;
    private String name;
    private String icon = "minecraft:chest";
    private int iconAmount = 1;
    private boolean glowing = false;
    private List<String> description = new ArrayList<>();
    private double price;
    /** Needed to see and buy the entry; empty for everyone. */
    private String permission = "";
    private int order = 0;
    /** Given in order, so a message can follow the item it announces. */
    private List<ShopReward> rewards = new ArrayList<>();

    /** Cached result of {@link #isAvailable()}; registries do not change while the server runs. */
    private volatile Boolean available;

    public ShopEntry(String identifier) {
        this.identifier = identifier;
        this.name = "&f" + identifier;
    }

    /** Whether every item and effect this entry gives exists on this server. */
    public boolean isAvailable() {
        Boolean cached = available;
        if (cached != null) return cached;
        boolean result = true;
        for (ShopReward reward : rewards) {
            if (! reward.isAvailable()) {
                result = false;
                break;
            }
        }
        available = result;
        return result;
    }

    public boolean hasPermission() {
        return permission != null && ! permission.isBlank();
    }
}
