package host.plas.collections.listeners;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.collections.StreamlineCollections;
import host.plas.collections.config.CollectionsConfig;
import host.plas.collections.data.Catalog;
import host.plas.collections.data.CollectionManager;
import host.plas.collections.data.CollectionPlayer;
import host.plas.collections.data.PlacedBlocks;
import singularity.data.players.CosmicPlayer;
import singularity.events.player.gameplay.PlayerBrokeBlockEvent;
import singularity.events.player.gameplay.PlayerCaughtFishEvent;
import singularity.events.player.gameplay.PlayerFilledBucketEvent;
import singularity.events.player.gameplay.PlayerGameplayEvent;
import singularity.events.player.gameplay.PlayerKilledEntityEvent;
import singularity.events.player.gameplay.PlayerPlacedBlockEvent;
import singularity.events.server.LoginCompletedEvent;
import singularity.events.server.LogoutEvent;
import singularity.gui.CosmicItem;
import singularity.modules.ModuleUtils;
import singularity.objects.world.CosmicBlock;
import singularity.scheduler.ModuleDelayedRunnable;

/**
 * Loads progress on join, saves it on quit, and counts what players collect. Gameplay events
 * only fire on game servers, where every one reports an action that already went through.
 */
public class CollectionsListener implements BaseEventListener {

    public CollectionsListener() {
        ModuleUtils.listen(this, StreamlineCollections.getInstance());
    }

    @BaseProcessor
    public void onJoin(LoginCompletedEvent event) {
        if (! StreamlineCollections.isTrackingServer()) return;
        CosmicPlayer player = event.getPlayer();
        if (player == null) return;

        CollectionPlayer progress = StreamlineCollections.getLoader().getOrCreate(player.getUuid());
        progress.setName(player.getCurrentName());

        if (StreamlineCollections.getMainConfig().isRemindOnJoin()) {
            new ModuleDelayedRunnable(StreamlineCollections.getInstance(), 40) {
                @Override
                public void runDelayed() {
                    if (player.isOnline()) CollectionManager.remind(player);
                }
            };
        }
    }

    @BaseProcessor
    public void onQuit(LogoutEvent event) {
        if (! StreamlineCollections.isTrackingServer()) return;
        CosmicPlayer player = event.getPlayer();
        if (player == null) return;

        CollectionManager.getLoaded(player.getUuid()).ifPresent(progress -> progress.saveAndUnload(true));
    }

    @BaseProcessor
    public void onPlace(PlayerPlacedBlockEvent event) {
        PlacedBlocks placed = StreamlineCollections.getPlacedBlocks();
        if (placed == null) return;

        CosmicBlock block = event.getBlock();
        placed.mark(block.getWorldName(), (int) block.getX(), (int) block.getY(), (int) block.getZ());
    }

    @BaseProcessor
    public void onBreak(PlayerBrokeBlockEvent event) {
        CosmicBlock block = event.getBlock();
        PlacedBlocks placed = StreamlineCollections.getPlacedBlocks();
        // The mark goes whether or not the break counts.
        boolean wasPlaced = placed != null
                && placed.clear(block.getWorldName(), (int) block.getX(), (int) block.getY(), (int) block.getZ());

        CollectionsConfig config = StreamlineCollections.getMainConfig();
        if (! tracking(event) || ! config.isTrackBlockBreak()) return;

        Catalog catalog = CollectionManager.getCatalog();
        String collection = catalog.blockSource(block.getType());
        if (collection == null) return;

        if (catalog.needsMaturity(block.getType())) {
            if (! event.isFullyGrown()) return;
        } else if (wasPlaced && ! config.isCountPlacedBlocks()) {
            return;
        }

        CollectionManager.add(event.getPlayer(), collection, 1L);
    }

    @BaseProcessor
    public void onKill(PlayerKilledEntityEvent event) {
        if (! tracking(event) || ! StreamlineCollections.getMainConfig().isTrackMobKills()) return;

        for (CosmicItem drop : event.getDrops()) {
            String collection = CollectionManager.getCatalog().dropSource(drop.getMaterial());
            if (collection != null) CollectionManager.add(event.getPlayer(), collection, drop.getAmount());
        }
    }

    @BaseProcessor
    public void onFish(PlayerCaughtFishEvent event) {
        if (! tracking(event) || ! StreamlineCollections.getMainConfig().isTrackFishing()) return;

        for (CosmicItem caught : event.getCaught()) {
            String collection = CollectionManager.getCatalog().fishSource(caught.getMaterial());
            if (collection != null) CollectionManager.add(event.getPlayer(), collection, caught.getAmount());
        }
    }

    @BaseProcessor
    public void onBucket(PlayerFilledBucketEvent event) {
        if (! tracking(event) || ! StreamlineCollections.getMainConfig().isTrackBucketFill()) return;

        String collection = CollectionManager.getCatalog().bucketSource(event.getFilledItem());
        if (collection != null) CollectionManager.add(event.getPlayer(), collection, 1L);
    }

    private static boolean tracking(PlayerGameplayEvent event) {
        return StreamlineCollections.isTrackingServer() && event.getPlayer() != null && ! event.isCreativeLike();
    }
}
