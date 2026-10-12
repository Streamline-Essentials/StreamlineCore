package host.plas.collections.slayer;

import lombok.Getter;

import java.util.Locale;

/** The mob groups shown in {@code /slayer}, in menu order. Every mob belongs to exactly one. */
@Getter
public enum SlayerCategory {
    MONSTERS("Monsters", "minecraft:zombie_head", "#FF5555"),
    NETHER("Nether", "minecraft:netherrack", "#FF8C3A"),
    AQUATIC("Aquatic", "minecraft:heart_of_the_sea", "#55FFFF"),
    PASSIVE("Passive", "minecraft:wheat", "#55FF55"),
    PETS("Pets", "minecraft:bone", "#FFCC00"),
    BOSSES("Bosses", "minecraft:dragon_head", "#D17BFF"),
    ;

    private final String displayName;
    private final String icon;
    private final String accent;

    SlayerCategory(String displayName, String icon, String accent) {
        this.displayName = displayName;
        this.icon = icon;
        this.accent = accent;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Matches the enum name or the display name, any case; null when neither matches. */
    public static SlayerCategory byName(String raw) {
        if (raw == null) return null;
        for (SlayerCategory category : values()) {
            if (category.name().equalsIgnoreCase(raw) || category.displayName.equalsIgnoreCase(raw)) return category;
        }
        return null;
    }
}
