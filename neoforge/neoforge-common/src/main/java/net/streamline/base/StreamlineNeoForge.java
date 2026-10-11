package net.streamline.base;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.handlers.GameplayHandler;
import net.streamline.platform.listeners.NeoForgeBreakListener;
import net.streamline.platform.listeners.NeoForgeClientListener;
import net.streamline.platform.listeners.NeoForgeListener;

@Mod("streamlinecore")
public class StreamlineNeoForge extends BasePlugin {

    public StreamlineNeoForge(IEventBus modBus, Dist dist) {
        initialize();
        NeoForge.EVENT_BUS.register(new NeoForgeListener());
        NeoForge.EVENT_BUS.register(new NeoForgeBreakListener());
        // Client classes exist only on a client, so the listener touching them loads only there.
        if (dist.isClient()) NeoForge.EVENT_BUS.register(new NeoForgeClientListener());
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.NEOFORGE;
    }

    @Override
    protected GameplayHandler createGameplayHandler() {
        return new GameplayHandler() {
            @Override
            protected void refreshNames(ServerPlayer player) {
                player.refreshDisplayName();
                player.refreshTabListName();
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

    public static StreamlineNeoForge getInstance() {
        return (StreamlineNeoForge) BasePlugin.getInstance();
    }
}
