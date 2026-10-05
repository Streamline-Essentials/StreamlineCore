package net.streamline.platform.listeners;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.streamline.platform.handlers.GameplayHandler;

/**
 * Forwards Forge events to {@link ModEvents}, for Forge versions on EventBus 7, where each
 * event type carries its own {@code BUS}. On a cancellable bus a listener returning
 * {@code true} cancels the event.
 */
public final class ForgeListener {

    private ForgeListener() {}

    public static void register() {
        ServerStartingEvent.BUS.addListener(e -> ModEvents.onServerStarting(e.getServer()));
        ServerStartedEvent.BUS.addListener(e -> ModEvents.onServerStarted(e.getServer()));
        ServerStoppingEvent.BUS.addListener(e -> ModEvents.onServerStopping(e.getServer()));
        ServerStoppedEvent.BUS.addListener(e -> ModEvents.onServerStopped(e.getServer()));
        RegisterCommandsEvent.BUS.addListener(e -> ModEvents.onRegisterCommands(e.getDispatcher()));
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer) ModEvents.onPlayerJoin((ServerPlayer) e.getEntity());
        });
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer) ModEvents.onPlayerQuit((ServerPlayer) e.getEntity());
        });
        ServerChatEvent.BUS.addListener(e ->
                ! ModEvents.onChat(e.getPlayer(), e.getRawText(), message -> e.setMessage(Component.literal(message))));
        CommandEvent.BUS.addListener(e -> {
            if (! (e.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer)) return false;
            ServerPlayer player = (ServerPlayer) e.getParseResults().getContext().getSource().getEntity();
            return ! ModEvents.onCommand(player, e.getParseResults().getReader().getString());
        });
        // A monitor sees only deaths no other mod cancelled.
        LivingDeathEvent.BUS.addListener(Priority.MONITOR, e -> {
            if (e.getEntity() instanceof ServerPlayer) ModEvents.onDeath((ServerPlayer) e.getEntity());
        });
        PlayerEvent.NameFormat.BUS.addListener(e ->
                GameplayHandler.displayName(e.getEntity().getUUID()).ifPresent(e::setDisplayname));
        PlayerEvent.TabListNameFormat.BUS.addListener(e ->
                GameplayHandler.displayName(e.getEntity().getUUID()).ifPresent(e::setDisplayName));
    }

    /** The gameplay handler for Forge, which re-renders names on request. */
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
