package net.streamline.platform.listeners;

import org.bukkit.Keyed;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.events.player.gameplay.PlayerBrokeBlockEvent;
import singularity.events.player.gameplay.PlayerCaughtFishEvent;
import singularity.events.player.gameplay.PlayerFilledBucketEvent;
import singularity.events.player.gameplay.PlayerKilledEntityEvent;
import singularity.events.player.gameplay.PlayerPlacedBlockEvent;
import singularity.gui.CosmicItem;
import singularity.objects.world.CosmicBlock;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fires the cross-platform {@code singularity.events.player.gameplay} events. Every handler runs
 * at {@link EventPriority#MONITOR} and skips cancelled events, so the cosmic events report only
 * what actually happened.
 */
public class GameplayEventsListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        CosmicPlayer cosmic = cosmic(player);
        if (cosmic == null) return;

        Block block = event.getBlock();
        boolean ageable = false;
        boolean mature = true;
        BlockData data = block.getBlockData();
        if (data instanceof Ageable) {
            Ageable crop = (Ageable) data;
            ageable = true;
            mature = crop.getAge() >= crop.getMaximumAge();
        }

        new PlayerBrokeBlockEvent(cosmic, gameMode(player), block(block), ageable, mature).fire();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        CosmicPlayer cosmic = cosmic(player);
        if (cosmic == null) return;

        new PlayerPlacedBlockEvent(cosmic, gameMode(player), block(event.getBlockPlaced())).fire();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        CosmicPlayer cosmic = cosmic(killer);
        if (cosmic == null) return;

        List<CosmicItem> drops = new ArrayList<>();
        for (ItemStack drop : event.getDrops()) {
            if (drop == null || drop.getType().isAir()) continue;
            drops.add(CosmicItem.of(key(drop.getType())).setAmount(drop.getAmount()));
        }

        new PlayerKilledEntityEvent(cosmic, gameMode(killer), key(event.getEntityType()), drops).fire();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (! (event.getCaught() instanceof Item)) return;

        Player player = event.getPlayer();
        CosmicPlayer cosmic = cosmic(player);
        if (cosmic == null) return;

        ItemStack stack = ((Item) event.getCaught()).getItemStack();
        List<CosmicItem> caught = new ArrayList<>();
        caught.add(CosmicItem.of(key(stack.getType())).setAmount(stack.getAmount()));

        new PlayerCaughtFishEvent(cosmic, gameMode(player), caught).fire();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucket(PlayerBucketFillEvent event) {
        ItemStack filled = event.getItemStack();
        if (filled == null) return;

        Player player = event.getPlayer();
        CosmicPlayer cosmic = cosmic(player);
        if (cosmic == null) return;

        new PlayerFilledBucketEvent(cosmic, gameMode(player), key(filled.getType())).fire();
    }

    private static CosmicPlayer cosmic(Player player) {
        if (player == null) return null;
        return UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
    }

    private static String gameMode(Player player) {
        return player.getGameMode().name().toLowerCase(Locale.ROOT);
    }

    private static CosmicBlock block(Block block) {
        return new CosmicBlock(new PlayerWorld(block.getWorld().getName()),
                new WorldPosition(block.getX(), block.getY(), block.getZ()), key(block.getType()));
    }

    private static String key(Keyed keyed) {
        try {
            return keyed.getKey().toString();
        } catch (Throwable t) {
            // Legacy or unknown constants have no key.
            return CosmicItem.normalizeKey(String.valueOf(keyed));
        }
    }
}
