package net.streamline.platform.listeners;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Forwards NeoForge's block-break event to {@link GameplayEvents}, from 26.1, where it is {@code BreakBlockEvent}.
 */
public class NeoForgeBreakListener {

    /** Last, so a break another mod cancels is not reported. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BreakBlockEvent event) {
        if (event.isCanceled() || ! (event.getPlayer() instanceof ServerPlayer) || ! (event.getLevel() instanceof Level)) return;
        GameplayEvents.onBlockBroken((ServerPlayer) event.getPlayer(), (Level) event.getLevel(), event.getPos(), event.getState());
    }
}