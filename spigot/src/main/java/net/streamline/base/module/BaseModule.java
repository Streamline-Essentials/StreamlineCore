package net.streamline.base.module;

import lombok.Getter;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.modules.SimpleModule;
import net.streamline.api.modules.dependencies.Dependency;
import net.streamline.base.listeners.BaseListener;
import net.streamline.platform.listeners.PlatformListener;

import java.util.Collections;
import java.util.List;

public class BaseModule extends SimpleModule {
    @Getter
    private static BaseModule instance;

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
    }

    @Override
    public void onEnable() {
        ModuleUtils.listen(new BaseListener(), this);
    }

    @Override
    public void onDisable() {
//        ModuleUtils.unl
    }
}
