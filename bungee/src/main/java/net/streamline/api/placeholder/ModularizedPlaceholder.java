package net.streamline.api.placeholder;

import lombok.Getter;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.base.Streamline;

public class ModularizedPlaceholder extends CustomPlaceholder {
    @Getter
    private final StreamlineModule module;

    public ModularizedPlaceholder(StreamlineModule module, String key, String value) {
        super(key, value);
        this.module = module;
    }

    @Override
    public void register() {
        Streamline.getRATAPI().registerModularizedPlaceholder(this);
    }

    @Override
    public void unregister() {
        Streamline.getRATAPI().unregisterModularizedPlaceholder(this);
    }
}
