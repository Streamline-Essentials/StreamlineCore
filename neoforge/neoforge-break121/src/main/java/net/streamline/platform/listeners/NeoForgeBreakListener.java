package net.streamline.platform.listeners;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Forwards NeoForge's block-break event to {@link GameplayEvents}, up to 1.21.11, where it is {@code BlockEvent.BreakEvent}.
 */
public class NeoForgeBreakListener {

    /** Last, so a break another mod cancels is not reported. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || ! (event.getPlayer() instanceof ServerPlayer) || ! (event.getLevel() instanceof Level)) return;
        GameplayEvents.onBlockBroken((ServerPlayer) event.getPlayer(), (Level) event.getLevel(), event.getPos(), event.getState());
    }
}