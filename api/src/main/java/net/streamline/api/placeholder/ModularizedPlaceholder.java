package net.streamline.api.placeholder;

import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.modules.StreamlineModule;

public class ModularizedPlaceholder extends CustomPlaceholder {
    @Getter
    private final StreamlineModule module;

    public ModularizedPlaceholder(StreamlineModule module, String key, String value) {
        super(key, value);
        this.module = module;
    }

    @Override
    public void register() {
        SLAPI.getInstance().getPlatform().getRATAPI().registerModularizedPlaceholder(this);
    }

    @Override
    public void unregister() {
        SLAPI.getInstance().getPlatform().getRATAPI().unregisterModularizedPlaceholder(this);
    }
}
