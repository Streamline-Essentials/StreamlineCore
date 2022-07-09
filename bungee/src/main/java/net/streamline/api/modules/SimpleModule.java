package net.streamline.api.modules;

import net.streamline.api.modules.BundledModule;

import java.util.Collections;
import java.util.List;

public class SimpleModule extends BundledModule {
    public SimpleModule(String identifier, List<String> authors, List<String> softDepends, List<String> hardDepends) {
        super(identifier, authors, softDepends, hardDepends);
    }

    @Override
    protected void onLoad() {

    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}
