package net.streamline.base;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.handlers.GameplayHandler;
import net.streamline.platform.listeners.FabricListener;

/**
 * Runs on dedicated servers and on clients, where Streamline comes up with the integrated
 * server of a world the client hosts. {@link StreamlineFabricClient} adds the client side.
 */
public class StreamlineFabric extends BasePlugin implements ModInitializer {

    @Override
    public void onInitialize() {
        setClient(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT);
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
