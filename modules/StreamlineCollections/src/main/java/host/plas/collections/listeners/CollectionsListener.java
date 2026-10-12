package host.plas.collections.listeners;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.collections.StreamlineCollections;
import host.plas.collections.config.CollectionsConfig;
import host.plas.collections.data.Catalog;
import host.plas.collections.data.CollectionManager;
import host.plas.collections.data.PlacedBlocks;
import host.plas.collections.data.StatFeeds;
import host.plas.collections.data.StatReader;
import host.plas.collections.data.StatSync;
import host.plas.collections.data.Track;
import host.plas.collections.data.TrackedPlayer;
import host.plas.collections.progression.ProgressionManager;
import host.plas.collections.progression.ProgressionStat;
import host.plas.collections.progression.Skill;
import host.plas.collections.slayer.SlayerCatalog;
import host.plas.collections.slayer.SlayerDefinition;
import host.plas.collections.slayer.SlayerManager;
import singularity.data.players.CosmicPlayer;
import singularity.events.player.gameplay.PlayerBrokeBlockEvent;
import singularity.events.player.gameplay.PlayerCaughtFishEvent;
import singularity.events.player.gameplay.PlayerFilledBucketEvent;
import singularity.events.player.gameplay.PlayerGameplayEvent;
import singularity.events.player.gameplay.PlayerKilledEntityEvent;
import singularity.events.player.gameplay.PlayerPlacedBlockEvent;
import singularity.events.server.CosmicChatEvent;
import singularity.events.server.LoginCompletedEvent;
import singularity.events.server.LogoutEvent;
import singularity.gui.CosmicItem;
import singularity.modules.ModuleUtils;
import singularity.objects.world.CosmicBlock;
import singularity.scheduler.ModuleDelayedRunnable;

/**
 * Loads collections, slayer and progression on join, saves them on quit, and counts what players
 * do. Gameplay events only fire on game servers, where every one reports an action that already
 * went through.
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

        for (Track track : Track.values()) {
            TrackedPlayer progress = track.getLoader().getOrCreate(player.getUuid());
            progress.setName(player.getCurrentName());
        }
        ProgressionManager.count(player.getUuid(), ProgressionStat.JOINS, 1);

        new JoinTask(player, 0);
    }

    /**
     * Once a player's stored progress has loaded, reads their statistics (baselines, syncs and
     * floors) and reminds them of levels waiting; retried a few times while a load is still running.
     */
    private static final class JoinTask extends ModuleDelayedRunnable {
        private static final int MAX_TRIES = 10;

        private final CosmicPlayer player;
        private final int attempt;

        private JoinTask(CosmicPlayer player, int attempt) {
            super(StreamlineCollections.getInstance(), 40);
            this.player = player;
            this.attempt = attempt;
        }

        @Override
        public void runDelayed() {
            if (! player.isOnline()) return;

            for (Track track : Track.values()) {
                TrackedPlayer progress = track.loaded(player.getUuid()).orElse(null);
                if (progress == null) return;
                if (! progress.isFullyLoaded()) {
                    if (attempt + 1 < MAX_TRIES) new JoinTask(player, attempt + 1);
                    return;
                }
            }

            StatReader.submit("read statistics for " + player.getCurrentName(), () -> {
                CollectionsConfig config = StreamlineCollections.getMainConfig();
                StatFeeds.baseline(player);
                if (config.isStatSyncEnabled() && config.isStatSyncOnJoin()) StatSync.sync(player, true);
                if (SlayerManager.isEnabled() && SlayerManager.getConfig().isSyncStats()) SlayerManager.sync(player);
                ProgressionManager.joined(player);

                if (config.isRemindOnJoin()) CollectionManager.remind(player);
                if (SlayerManager.getConfig().isRemindOnJoin()) SlayerManager.remind(player);
            });
        }
    }

    @BaseProcessor
    public void onQuit(LogoutEvent event) {
        if (! StreamlineCollections.isTrackingServer()) return;
        CosmicPlayer player = event.getPlayer();
        if (player == null) return;

        ProgressionManager.count(player.getUuid(), ProgressionStat.LEAVES, 1);
        try {
            if (StreamlineCollections.getMainConfig().isTrackStatistics()) StatFeeds.refresh(player);
            ProgressionManager.refresh(player);
        } catch (Throwable t) {
            StreamlineCollections.getInstance().logWarning("Could not count statistics for " + player.getCurrentName() + " on quit: " + t.getMessage());
        }

        for (Track track : Track.values()) {
            track.loaded(player.getUuid()).ifPresent(progress -> progress.saveAndUnload(true));
        }
    }

    @BaseProcessor
    public void onChat(CosmicChatEvent event) {
        if (! StreamlineCollections.isTrackingServer() || ! (event.getSender() instanceof CosmicPlayer)) return;
        String message = event.getOriginalMessage();
        if (message == null || message.startsWith("/")) return;
        ProgressionManager.count(event.getSender().getUuid(), ProgressionStat.CHAT_MESSAGES, 1);
    }

    @BaseProcessor
    public void onPlace(PlayerPlacedBlockEvent event) {
        CosmicBlock block = event.getBlock();
        PlacedBlocks placed = StreamlineCollections.getPlacedBlocks();
        if (placed != null) placed.mark(block.getWorldName(), (int) block.getX(), (int) block.getY(), (int) block.getZ());

        if (! tracking(event)) return;
        String uuid = event.getPlayer().getUuid();
        ProgressionManager.count(uuid, ProgressionStat.PLACED_BLOCKS, 1);
        ProgressionManager.train(uuid, Skill.forPlace(block.getType()), 1);
    }

    @BaseProcessor
    public void onBreak(PlayerBrokeBlockEvent event) {
        CosmicBlock block = event.getBlock();
        PlacedBlocks placed = StreamlineCollections.getPlacedBlocks();
        // The mark goes whether or not the break counts.
        boolean wasPlaced = placed != null
                && placed.clear(block.getWorldName(), (int) block.getX(), (int) block.getY(), (int) block.getZ());

        if (! tracking(event)) return;
        String uuid = event.getPlayer().getUuid();
        ProgressionManager.count(uuid, ProgressionStat.BROKEN_BLOCKS, 1);
        ProgressionManager.train(uuid, Skill.forBreak(block.getType()), 1);

        CollectionsConfig config = StreamlineCollections.getMainConfig();
        if (! config.isTrackBlockBreak()) return;

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
        if (! tracking(event)) return;

        if (SlayerManager.isEnabled()) {
            SlayerDefinition mob = SlayerCatalog.forEntity(event.getEntityType());
            if (mob != null) SlayerManager.add(event.getPlayer(), mob, 1L);
        }

        if (! StreamlineCollections.getMainConfig().isTrackMobKills()) return;
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
