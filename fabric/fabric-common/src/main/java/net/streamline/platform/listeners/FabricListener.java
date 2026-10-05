package net.streamline.platform.listeners;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;

/**
 * Forwards Fabric API callbacks to {@link ModEvents}.
 *
 * <p>Fabric API offers no hook before a command runs and none for formatting player names,
 * so on Fabric {@code CosmicCommandPreprocessEvent} is never fired and nicknames do not
 * appear in vanilla chat or the player list. Chat can be cancelled but not rewritten.</p>
 *
 * <p>Of the gameplay events, Fabric API covers block breaks and deaths only: block placing,
 * fishing and bucket filling are not reported, and kills are reported without their drops.</p>
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

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) ->
                ModEvents.onChat(sender, message.signedContent(), null));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer) ModEvents.onDeath((ServerPlayer) entity);
            // Fabric API exposes no death drops, so kills are reported without them.
            if (damageSource.getEntity() instanceof ServerPlayer) {
                GameplayEvents.onEntityKilled((ServerPlayer) damageSource.getEntity(), entity, Collections.emptyList());
            }
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer) GameplayEvents.onBlockBroken((ServerPlayer) player, world, pos, state);
        });
    }
}
