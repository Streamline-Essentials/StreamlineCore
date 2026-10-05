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
import singularity.gui.CosmicItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

        if (item.getSkullOwner() != null && meta instanceof SkullMeta) applySkull((SkullMeta) meta, item.getSkullOwner());

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
     * Sets a skull's owner from a UUID, or from the name of an online player. A name for an
     * offline player is skipped: resolving it can block on a Mojang lookup.
     */
    private static void applySkull(SkullMeta meta, String owner) {
        try {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(owner)));
            return;
        } catch (IllegalArgumentException notUuid) {
            // Fall through to a name.
        }

        Player online = Bukkit.getPlayerExact(owner);
        if (online != null) meta.setOwningPlayer(online);
    }
}
