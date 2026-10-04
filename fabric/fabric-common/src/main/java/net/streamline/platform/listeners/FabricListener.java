package net.streamline.platform.listeners;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Forwards Fabric API callbacks to {@link ModEvents}.
 */
public final class FabricListener {

    private FabricListener() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(ModEvents::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(ModEvents::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(ModEvents::onServerStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(ModEvents::onServerStopped);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ModEvents.onRegisterCommands(dispatcher));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ModEvents.onPlayerJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ModEvents.onPlayerQuit(handler.getPlayer()));
    }
}
