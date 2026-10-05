package net.streamline.platform.listeners;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.streamline.platform.handlers.GameplayHandler;

/**
 * Forwards Forge game-bus events to {@link ModEvents}, for Forge versions on EventBus 6
 * (a single {@link MinecraftForge#EVENT_BUS} carrying every event).
 */
public final class ForgeListener {

    private ForgeListener() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent e) -> ModEvents.onServerStarting(e.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> ModEvents.onServerStarted(e.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> ModEvents.onServerStopping(e.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> ModEvents.onServerStopped(e.getServer()));
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> ModEvents.onRegisterCommands(e.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer) ModEvents.onPlayerJoin((ServerPlayer) e.getEntity());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer) ModEvents.onPlayerQuit((ServerPlayer) e.getEntity());
        });
        MinecraftForge.EVENT_BUS.addListener((ServerChatEvent e) -> {
            boolean allowed = ModEvents.onChat(e.getPlayer(), e.getRawText(), message -> e.setMessage(Component.literal(message)));
            if (! allowed) e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener((CommandEvent e) -> {
            if (! (e.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer)) return;
            ServerPlayer player = (ServerPlayer) e.getParseResults().getContext().getSource().getEntity();
            if (! ModEvents.onCommand(player, e.getParseResults().getReader().getString())) e.setCanceled(true);
        });
        // Last, so a death another mod cancels is not reported.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingDeathEvent e) -> {
            if (e.isCanceled() || ! (e.getEntity() instanceof ServerPlayer)) return;
            ModEvents.onDeath((ServerPlayer) e.getEntity());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.NameFormat e) ->
                GameplayHandler.displayName(e.getEntity().getUUID()).ifPresent(e::setDisplayname));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.TabListNameFormat e) ->
                GameplayHandler.displayName(e.getEntity().getUUID()).ifPresent(e::setDisplayName));
    }

    /** The gameplay handler for Forge-family loaders, which re-render names on request. */
    public static GameplayHandler gameplayHandler() {
        return new GameplayHandler() {
            @Override
            protected void refreshNames(ServerPlayer player) {
                player.refreshDisplayName();
                player.refreshTabListName();
            }
        };
    }
}
