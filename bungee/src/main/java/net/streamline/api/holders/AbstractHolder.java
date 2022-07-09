package net.streamline.api.holders;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ProxyServer;

public abstract class AbstractHolder<T> {
    @Getter @Setter
    private String plugin;
    @Getter @Setter
    private T api;

    AbstractHolder(String plugin) {
        this.plugin = plugin;
    }

    public boolean isPresent() { return (ProxyServer.getInstance().getPluginManager().getPlugin(this.plugin) != null); }
}
