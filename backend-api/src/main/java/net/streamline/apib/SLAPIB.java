package net.streamline.apib;

import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryProvider;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.objects.SingleSet;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class SLAPIB {
    @Getter @Setter
    private static SLAPIB instance;
    @Getter @Setter
    private static InventoryManager inventoryManager;

    @Getter
    private final File dataFolder;

    public SLAPIB(File dataFolder, JavaPlugin plugin) {
        instance = this;
        this.dataFolder = dataFolder;
        setInventoryManager(new InventoryManager(plugin));
        getInventoryManager().init();
    }

    public static SmartInventory.Builder getInventoryBuilder() {
        return SmartInventory.builder().manager(getInventoryManager());
    }

    public static SmartInventory getInventory(String id, InventoryProvider provider, int sizeRows, int sizeColumns, String title) {
        return getInventoryBuilder()
                .id(id)
                .provider(provider)
                .size(sizeRows, sizeColumns)
                .title(title)
                .build();
    }
}
