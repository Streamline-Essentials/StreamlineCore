package net.streamline.platform.handlers.gui;

import host.plas.bou.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import singularity.gui.CosmicItem;
import singularity.utils.profiles.PlayerLookup;
import singularity.utils.profiles.Textures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Turns {@link CosmicItem}s into Bukkit {@link ItemStack}s.
 *
 * <p>Names start with {@code &r} and lore lines with {@code &7}: CraftBukkit's legacy-text
 * conversion only drops the default italics once a format code appears, and lore without its own
 * colour otherwise shows in the default purple.</p>
 */
public final class BukkitItems {
    private BukkitItems() {
    }

    public static ItemStack toStack(CosmicItem item) {
        if (item == null || item.isAir()) return null;

        Material material = Material.matchMaterial(item.getMaterialKey());
        if (material == null || material.isAir() || ! material.isItem()) material = Material.BARRIER;

        ItemStack stack = new ItemStack(material, item.getAmount());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        if (item.getName() != null) meta.setDisplayName(ColorUtils.colorizeHard("&r" + item.getName()));

        if (! item.getLore().isEmpty()) {
            List<String> lore = new ArrayList<>();
            for (String line : item.getLore()) lore.add(ColorUtils.colorizeHard("&7" + line));
            meta.setLore(lore);
        }

        if (item.isGlowing()) applyGlow(meta);
        if (item.isHideExtras()) meta.addItemFlags(ItemFlag.values());

        if (item.getCustomModelData() > 0) {
            try {
                meta.setCustomModelData(item.getCustomModelData());
            } catch (Throwable ignored) {
                // Not supported on this server version.
            }
        }

        if (item.getSkullOwner() != null && meta instanceof SkullMeta) applySkull((SkullMeta) meta, item);

        stack.setItemMeta(meta);
        return stack;
    }

    private static void applyGlow(ItemMeta meta) {
        try {
            meta.setEnchantmentGlintOverride(true);
            return;
        } catch (Throwable ignored) {
            // Before 1.20.5 there is no glint override; a hidden enchantment gives the glint.
        }

        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (enchantment == null) return;
        meta.addEnchant(enchantment, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    /**
     * Gives a head its owner's skin without the server looking it up. A profile without
     * textures makes the server ask Mojang's session server for them, which knows no Bedrock
     * (Floodgate) or offline-mode player, rate-limits busy servers, and logs every failure.
     *
     * <p>In order: the skin {@link singularity.gui.GuiManager} attached from
     * {@link PlayerLookup}; the live profile of an online owner, when it has textures; otherwise
     * the default head, which the GUI replaces once the lookup finishes. Only with lookups
     * turned off does a Java owner's UUID go to the server, as plain Bukkit would.</p>
     */
    private static void applySkull(SkullMeta meta, CosmicItem item) {
        String owner = item.getSkullOwner();
        UUID uuid = null;
        try {
            uuid = UUID.fromString(owner);
        } catch (IllegalArgumentException notUuid) {
            // A name.
        }

        if (item.getSkinTexture() != null && applyTexture(meta, uuid, owner, item.getSkinTexture())) return;

        Player online = uuid != null ? Bukkit.getPlayer(uuid) : Bukkit.getPlayerExact(owner);
        if (online != null) {
            try {
                PlayerProfile live = online.getPlayerProfile();
                if (! live.getTextures().isEmpty()) {
                    meta.setOwnerProfile(live);
                    return;
                }
            } catch (Throwable ignored) {
                // Before 1.18.1 there are no profiles; fall through.
            }
        }

        if (uuid == null || PlayerLookup.isEnabled()) return;
        if (PlayerLookup.isBedrock(uuid) || PlayerLookup.isOfflineModeUuid(uuid)) return;
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
    }

    /** Puts {@code texturesValue} on the head as its owner's skin; {@code false} when this server cannot. */
    private static boolean applyTexture(SkullMeta meta, UUID uuid, String owner, String texturesValue) {
        String name = owner != null && JAVA_NAME.matcher(owner).matches() ? owner : null;
        // Heads need an id; a value derived from the texture keeps equal skins stacking.
        UUID id = uuid != null ? uuid : UUID.nameUUIDFromBytes(texturesValue.getBytes(StandardCharsets.UTF_8));

        try {
            // Paper keeps the property exactly, signature and all.
            com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(id, name);
            profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", texturesValue));
            meta.setPlayerProfile(profile);
            return true;
        } catch (Throwable notPaper) {
            // Spigot: only the skin URL can be set.
        }

        try {
            String url = Textures.skinUrl(texturesValue).orElse(null);
            if (url == null) return false;
            PlayerProfile profile = Bukkit.createPlayerProfile(id, name);
            PlayerTextures textures = profile.getTextures();
            try {
                textures.setSkin(new URL(url), Textures.isSlim(texturesValue) ? PlayerTextures.SkinModel.SLIM : PlayerTextures.SkinModel.CLASSIC);
            } catch (NoSuchMethodError olderApi) {
                textures.setSkin(new URL(url));
            }
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private static final Pattern JAVA_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
}
