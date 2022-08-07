package net.streamline.api.holders;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.modules.ModuleUtils;

public abstract class StreamlineDependencyHolder<T> {
    @Getter @Setter
    private String plugin;
    @Getter @Setter
    private T api;

    StreamlineDependencyHolder(String plugin) {
        this.plugin = plugin;
    }

    public boolean isPresent() {
        return ModuleUtils.serverHasPlugin(this.plugin);
    }
}
