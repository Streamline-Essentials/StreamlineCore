package net.streamline.apib.craft;

import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.messenger.Messenger;
import net.streamline.api.SLAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SavedItemStack {
    @Getter @Setter
    private String material;
    @Getter @Setter
    private int count;
    @Getter @Setter
    private String name;
    @Getter @Setter
    private List<String> lore;
    @Getter @Setter
    private ConcurrentHashMap<String, Integer> enchants;

    public void loreFrom(String[] lore) {
        this.lore = new ArrayList<>();
        this.lore.addAll(Arrays.stream(lore).toList());
    }

    public void addLore(String line) {
        this.lore.add(line);
    }

    public void addEnchant(Enchantment enchantment, int level) {
        this.enchants.put(enchantment.getKey().getKey(), level);
    }

    public SavedItemStack(ItemStack from) {
        this.setMaterial(from.getType().toString());
        this.setCount(from.getAmount());

        ItemMeta meta = from.getItemMeta();

        if (meta != null) {
            this.setName(meta.getDisplayName());
            this.setLore(meta.getLore());
        }
        for (Enchantment enchantment : from.getEnchantments().keySet()) {
            this.addEnchant(enchantment, from.getEnchantmentLevel(enchantment));
        }
    }

    public ItemStack get() {
        ItemStack stack = new ItemStack(
                Material.valueOf(this.getMaterial()),
                this.getCount()
        );
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            SLAPI.getInstance().getMessenger().logWarning("Could not get ItemMeta for an item!");
            return null;
        }
        if (! this.getName().equals("")) meta.setDisplayName(this.getName());
        meta.setLore(this.getLore());
        for (String enchant : enchants.keySet()) {
            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.fromString(enchant));
            if (enchantment == null) continue;
            meta.addEnchant(enchantment, enchants.get(enchant), false);
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
