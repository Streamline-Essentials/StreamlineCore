package singularity.gui;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A platform-agnostic description of an item to display in a {@link CosmicGui}.
 *
 * <p>This is a display item, not a real stack: it carries only what a menu needs to show (the
 * material, amount, name, lore, a glint and a skull owner). Each platform turns it into its own
 * stack when rendering.</p>
 *
 * <p>The material is a Minecraft item id. Both {@code "minecraft:diamond"} and the Bukkit-style
 * {@code "DIAMOND"} are accepted; {@link #getMaterialKey()} gives the normalised namespaced form
 * every renderer resolves. Names and lore use Streamline's colour syntax ({@code &} codes and
 * {@code &#RRGGBB} / {@code #RRGGBB} hex).</p>
 */
@Getter @Setter
@Accessors(chain = true)
public class CosmicItem {
    /** The item shown when a material cannot be resolved on the rendering platform. */
    public static final String FALLBACK_MATERIAL = "minecraft:barrier";

    private String material;
    private int amount;
    private String name;
    private List<String> lore;
    private boolean glowing;
    /** Hides attribute modifiers, enchantments and similar extra tooltip lines. */
    private boolean hideExtras;
    /** A player's UUID or name; only meaningful for {@code minecraft:player_head}. */
    private String skullOwner;
    /** The custom model data value, or {@code 0} for none. */
    private int customModelData;

    public CosmicItem(String material) {
        this.material = material == null || material.isBlank() ? FALLBACK_MATERIAL : material;
        this.amount = 1;
        this.name = null;
        this.lore = new ArrayList<>();
        this.glowing = false;
        this.hideExtras = true;
        this.skullOwner = null;
        this.customModelData = 0;
    }

    public static CosmicItem of(String material) {
        return new CosmicItem(material);
    }

    public static CosmicItem of(String material, String name, String... lore) {
        return new CosmicItem(material).setName(name).setLore(lore);
    }

    /** An item that renders as an empty slot. */
    public static CosmicItem air() {
        return new CosmicItem("minecraft:air");
    }

    public CosmicItem setAmount(int amount) {
        this.amount = Math.max(1, Math.min(99, amount));
        return this;
    }

    public CosmicItem setLore(List<String> lore) {
        this.lore = lore == null ? new ArrayList<>() : new ArrayList<>(lore);
        return this;
    }

    public CosmicItem setLore(String... lore) {
        return setLore(lore == null ? null : Arrays.asList(lore));
    }

    public CosmicItem addLore(String... lines) {
        if (lines != null) this.lore.addAll(Arrays.asList(lines));
        return this;
    }

    public CosmicItem addLore(List<String> lines) {
        if (lines != null) this.lore.addAll(lines);
        return this;
    }

    /**
     * The material as a lowercase namespaced id: {@code "DIAMOND"} and {@code "minecraft:diamond"}
     * both give {@code "minecraft:diamond"}.
     */
    public String getMaterialKey() {
        return normalizeKey(material);
    }

    public boolean isAir() {
        String key = getMaterialKey();
        return key.equals("minecraft:air") || key.equals("minecraft:cave_air") || key.equals("minecraft:void_air");
    }

    /**
     * Normalises an item or block id to a lowercase namespaced id, defaulting the namespace to
     * {@code minecraft}.
     */
    public static String normalizeKey(String id) {
        if (id == null || id.isBlank()) return FALLBACK_MATERIAL;
        String key = id.trim().toLowerCase(Locale.ROOT);
        return key.indexOf(':') < 0 ? "minecraft:" + key : key;
    }

    /**
     * The path of an id without its namespace: {@code "minecraft:diamond"} and {@code "DIAMOND"}
     * both give {@code "diamond"}.
     */
    public static String keyPath(String id) {
        String key = normalizeKey(id);
        return key.substring(key.indexOf(':') + 1);
    }

    public CosmicItem copy() {
        CosmicItem item = new CosmicItem(material);
        item.amount = amount;
        item.name = name;
        item.lore = new ArrayList<>(lore);
        item.glowing = glowing;
        item.hideExtras = hideExtras;
        item.skullOwner = skullOwner;
        item.customModelData = customModelData;
        return item;
    }

    public void write(DataOutput out) throws IOException {
        out.writeUTF(material);
        out.writeByte(amount);
        writeNullable(out, name);
        out.writeShort(lore.size());
        for (String line : lore) out.writeUTF(line == null ? "" : line);
        out.writeBoolean(glowing);
        out.writeBoolean(hideExtras);
        writeNullable(out, skullOwner);
        out.writeInt(customModelData);
    }

    public static CosmicItem read(DataInput in) throws IOException {
        CosmicItem item = new CosmicItem(in.readUTF());
        item.amount = in.readByte();
        item.name = readNullable(in);
        int loreSize = in.readShort();
        for (int i = 0; i < loreSize; i++) item.lore.add(in.readUTF());
        item.glowing = in.readBoolean();
        item.hideExtras = in.readBoolean();
        item.skullOwner = readNullable(in);
        item.customModelData = in.readInt();
        return item;
    }

    static void writeNullable(DataOutput out, String value) throws IOException {
        out.writeBoolean(value != null);
        if (value != null) out.writeUTF(value);
    }

    static String readNullable(DataInput in) throws IOException {
        return in.readBoolean() ? in.readUTF() : null;
    }
}
