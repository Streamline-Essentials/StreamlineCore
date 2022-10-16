package net.streamline.apib;

import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryProvider;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.objects.SingleSet;
import net.streamline.apib.depends.PAPIDepend;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class SLAPIB {
    @Getter @Setter
    private static SLAPIB instance;
    @Getter @Setter
    private static InventoryManager inventoryManager;
    @Getter @Setter
    private static SLAPI<?, ?, ?> slapi;
    @Getter @Setter
    private static Plugin plugin;
    @Getter @Setter
    private static PAPIDepend papiDepend;

    public SLAPIB(SLAPI<?, ?, ?> slapi, JavaPlugin plugin) {
        instance = this;
        setPlugin(plugin);
        setSlapi(slapi);
        setInventoryManager(new InventoryManager(plugin));
        getInventoryManager().init();

        setPapiDepend(new PAPIDepend());
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
