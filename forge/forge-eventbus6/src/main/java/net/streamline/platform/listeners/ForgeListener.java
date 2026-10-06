package net.streamline.platform.listeners;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.level.BlockEvent;
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
        // Last, so actions another mod cancels are not reported.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (BlockEvent.BreakEvent e) -> {
            if (e.isCanceled() || ! (e.getPlayer() instanceof ServerPlayer) || ! (e.getLevel() instanceof Level)) return;
            GameplayEvents.onBlockBroken((ServerPlayer) e.getPlayer(), (Level) e.getLevel(), e.getPos(), e.getState());
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (BlockEvent.EntityPlaceEvent e) -> {
            if (e.isCanceled() || ! (e.getEntity() instanceof ServerPlayer) || ! (e.getLevel() instanceof Level)) return;
            GameplayEvents.onBlockPlaced((ServerPlayer) e.getEntity(), (Level) e.getLevel(), e.getPos(), e.getPlacedBlock());
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingDropsEvent e) -> {
            if (e.isCanceled() || ! (e.getSource().getEntity() instanceof ServerPlayer)) return;
            GameplayEvents.onEntityKilled((ServerPlayer) e.getSource().getEntity(), e.getEntity(),
                    GameplayEvents.stacksOf(e.getDrops()));
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ItemFishedEvent e) -> {
            if (e.isCanceled() || ! (e.getEntity() instanceof ServerPlayer)) return;
            GameplayEvents.onFishCaught((ServerPlayer) e.getEntity(), e.getDrops());
        });
        // High, so mods at the lower priorities see what Streamline modules decided.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, (ExplosionEvent.Detonate e) -> {
            // Fire is lit on the same positions, so clearing them also keeps an incendiary blast from burning.
            if (! EntityEvents.onExplosion(e.getExplosion().getDirectSourceEntity(), e.getExplosion().getIndirectSourceEntity())) {
                e.getAffectedBlocks().clear();
            }
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, (EntityMobGriefingEvent e) -> {
            if (e.getResult() != Event.Result.DENY && ! EntityEvents.onMobGrief(e.getEntity())) e.setResult(Event.Result.DENY);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, (LivingAttackEvent e) -> {
            if (e.isCanceled()) return;
            if (! EntityEvents.onDamaged(e.getEntity(), e.getSource(), e.getAmount())) e.setCanceled(true);
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
