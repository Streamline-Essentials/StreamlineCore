package net.streamline.base;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.handlers.GameplayHandler;
import net.streamline.platform.listeners.FabricListener;

public class StreamlineFabric extends BasePlugin implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        initialize();
        FabricListener.register();
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.FABRIC;
    }

    /** Fabric has no hook for formatting player names, so there is nothing to refresh. */
    @Override
    protected GameplayHandler createGameplayHandler() {
        return new GameplayHandler() {
            @Override
            protected void refreshNames(ServerPlayer player) {
            }
        };
    }

    @Override
    public void load() {
    }

    @Override
    public void enable() {
    }

    @Override
    public void disable() {
    }

    @Override
    public void reload() {
    }

    public static StreamlineFabric getInstance() {
        return (StreamlineFabric) BasePlugin.getInstance();
    }
}
