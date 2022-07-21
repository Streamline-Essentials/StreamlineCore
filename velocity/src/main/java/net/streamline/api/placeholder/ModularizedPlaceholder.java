package net.streamline.api.placeholder;

import lombok.Getter;
import net.streamline.api.modules.BundledModule;
import net.streamline.base.Streamline;

public class ModularizedPlaceholder extends CustomPlaceholder {
    @Getter
    private final BundledModule module;

    public ModularizedPlaceholder(BundledModule module, String key, String value) {
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
