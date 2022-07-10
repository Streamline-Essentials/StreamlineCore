package net.streamline.api.holders;

import lombok.Getter;
import lombok.Setter;
import net.streamline.base.Streamline;

public abstract class AbstractHolder<T> {
    @Getter @Setter
    private String plugin;
    @Getter @Setter
    private T api;

    AbstractHolder(String plugin) {
        this.plugin = plugin;
    }

    public boolean isPresent() { return (Streamline.getInstance().getProxy().getPluginManager().getPlugin(this.plugin).isPresent()); }
}
