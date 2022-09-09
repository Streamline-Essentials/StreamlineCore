package net.streamline.api.base.module;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.modules.SimpleModule;
import net.streamline.api.modules.dependencies.Dependency;
import net.streamline.api.base.listeners.BaseListener;

import java.util.Collections;
import java.util.List;

public class BaseModule extends SimpleModule {
    @Getter
    private static BaseModule instance;

    @Getter @Setter
    private static BaseListener baseListener;

    @Override
    public String identifier() {
        return "streamline-base";
    }

    @Override
    public List<String> authors() {
        return List.of("Quaint", "RedstonedLife");
    }

    @Override
    public List<Dependency> dependencies() {
        return Collections.emptyList();
    }

    @Override
    public void onLoad() {
        instance = this;
        setBaseListener(new BaseListener());
        ModuleUtils.listen(getBaseListener(), this);
    }

    @Override
    public void onEnable() {
        // nothing right now.
    }

    @Override
    public void onDisable() {
        // nothing right now.
    }
}
