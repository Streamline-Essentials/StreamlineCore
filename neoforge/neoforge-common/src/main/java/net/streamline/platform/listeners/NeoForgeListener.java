package net.streamline.platform.listeners;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.streamline.platform.handlers.GameplayHandler;

/**
 * Forwards NeoForge game-bus events to {@link ModEvents}.
 */
public class NeoForgeListener {

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ModEvents.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ModEvents.onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ModEvents.onServerStopping(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        ModEvents.onServerStopped(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModEvents.onRegisterCommands(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) ModEvents.onPlayerJoin((ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer) ModEvents.onPlayerQuit((ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        boolean allowed = ModEvents.onChat(event.getPlayer(), event.getRawText(),
                message -> event.setMessage(Component.literal(message)));
        if (! allowed) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (! (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer)) return;
        ServerPlayer player = (ServerPlayer) event.getParseResults().getContext().getSource().getEntity();
        if (! ModEvents.onCommand(player, event.getParseResults().getReader().getString())) event.setCanceled(true);
    }

    /** Last, so a death another mod cancels is not reported. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || ! (event.getEntity() instanceof ServerPlayer)) return;
        ModEvents.onDeath((ServerPlayer) event.getEntity());
    }

    /** Last, so a placement another mod cancels is not reported. Breaks: {@link NeoForgeBreakListener}. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || ! (event.getEntity() instanceof ServerPlayer) || ! (event.getLevel() instanceof Level)) return;
        GameplayEvents.onBlockPlaced((ServerPlayer) event.getEntity(), (Level) event.getLevel(), event.getPos(), event.getPlacedBlock());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrops(LivingDropsEvent event) {
        if (event.isCanceled() || ! (event.getSource().getEntity() instanceof ServerPlayer)) return;
        GameplayEvents.onEntityKilled((ServerPlayer) event.getSource().getEntity(), event.getEntity(),
                GameplayEvents.stacksOf(event.getDrops()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFished(ItemFishedEvent event) {
        if (event.isCanceled() || ! (event.getEntity() instanceof ServerPlayer)) return;
        GameplayEvents.onFishCaught((ServerPlayer) event.getEntity(), event.getDrops());
    }

    @SubscribeEvent
    public void onNameFormat(PlayerEvent.NameFormat event) {
        GameplayHandler.displayName(event.getEntity().getUUID()).ifPresent(event::setDisplayname);
    }

    @SubscribeEvent
    public void onTabListName(PlayerEvent.TabListNameFormat event) {
        GameplayHandler.displayName(event.getEntity().getUUID()).ifPresent(event::setDisplayName);
    }
}
