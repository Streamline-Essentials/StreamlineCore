package net.streamline.apib;

import lombok.Getter;
import lombok.Setter;
import mc.obliviate.inventory.InventoryAPI;
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
    private static SLAPI<?, ?, ?> slapi;
    @Getter @Setter
    private static Plugin plugin;
    @Getter @Setter
    private static PAPIDepend papiDepend;

    public SLAPIB(SLAPI<?, ?, ?> slapi, JavaPlugin plugin) {
        instance = this;
        setPlugin(plugin);
        setSlapi(slapi);
        new InventoryAPI(plugin).init();

        setPapiDepend(new PAPIDepend());
    }
}
