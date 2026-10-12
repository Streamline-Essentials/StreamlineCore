package host.plas.collections;

import gg.drak.thebase.events.BaseEventHandler;
import host.plas.collections.commands.CollectionsAdminCommand;
import host.plas.collections.commands.CollectionsCommand;
import host.plas.collections.commands.CollectionsLeaderboardCommand;
import host.plas.collections.config.CatalogConfig;
import host.plas.collections.config.CollectionsConfig;
import host.plas.collections.data.Boards;
import host.plas.collections.data.CollectionManager;
import host.plas.collections.data.PlacedBlocks;
import host.plas.collections.data.StatReader;
import host.plas.collections.data.Track;
import host.plas.collections.listeners.CollectionsListener;
import host.plas.collections.placeholders.CollectionsExpansion;
import host.plas.collections.progression.ProgressionAdminCommand;
import host.plas.collections.progression.ProgressionBoardCommand;
import host.plas.collections.progression.ProgressionCommand;
import host.plas.collections.progression.ProgressionConfig;
import host.plas.collections.progression.ProgressionManager;
import host.plas.collections.slayer.SlayerAdminCommand;
import host.plas.collections.slayer.SlayerCommand;
import host.plas.collections.slayer.SlayerConfig;
import host.plas.collections.slayer.SlayerLeaderboardCommand;
import host.plas.collections.slayer.SlayerManager;
import host.plas.collections.timers.RemindTimer;
import host.plas.collections.timers.SaveTimer;
import host.plas.collections.timers.StatSyncTimer;
import host.plas.collections.timers.StatsTimer;
import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.Singularity;
import singularity.modules.SimpleModule;
import singularity.permissions.DefaultPermissions;
import singularity.scheduler.ModuleRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Collections, Slayer and Progression.
 *
 * <ul>
 *     <li>Collections: players gather items by breaking blocks, killing mobs, fishing and filling
 *     buckets; each collection has levels whose rewards are claimed from a menu.</li>
 *     <li>Slayer: kills per mob, with levels and rewards claimed the same way.</li>
 *     <li>Progression: cosmetic stars from weighted categories (blocks, kills, distances, chat...),
 *     plus skills.</li>
 * </ul>
 *
 * <p>Tracking, claiming and the periodic saves run on game servers. On a proxy the module only
 * serves the menus and leaderboards, read from the shared database, and hands the menus to the
 * player's backend to render.</p>
 */
public class StreamlineCollections extends SimpleModule {
    @Getter @Setter
    private static StreamlineCollections instance;

    @Getter @Setter
    private static CollectionsConfig mainConfig;
    @Getter @Setter
    private static CatalogConfig catalogConfig;

    @Getter @Setter
    private static PlacedBlocks placedBlocks;
    @Getter @Setter
    private static CollectionsListener listener;

    private static final List<CollectionsExpansion> expansions = new ArrayList<>();
    private static final List<ModuleRunnable> timers = new ArrayList<>();

    public StreamlineCollections(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Whether this server tracks and stores progress itself, rather than only showing it. */
    public static boolean isTrackingServer() {
        return ! Singularity.isProxy();
    }

    @Override
    public void onEnable() {
        instance = this;

        mainConfig = new CollectionsConfig();
        catalogConfig = new CatalogConfig();
        CollectionManager.setCatalog(catalogConfig.load());
        SlayerManager.setConfig(new SlayerConfig());
        SlayerManager.loadCatalog();
        ProgressionManager.setConfig(new ProgressionConfig());

        Track.initAll();
        StatReader.start();

        if (isTrackingServer()) {
            placedBlocks = new PlacedBlocks(getDataFolder());
        }

        // Admin commands stay operator-only; everyone may browse their own progress and the boards.
        DefaultPermissions.grantByDefault(
                "streamline.command.collections.default", "streamline.command.collectionsleaderboard.default",
                "streamline.command.slayer.default", "streamline.command.slayerlb.default",
                "streamline.command.progression.default", "streamline.command.progressionleaderboard.default");

        listener = new CollectionsListener();
        expansions.add(new CollectionsExpansion("streamlinecollections"));
        expansions.add(new CollectionsExpansion("collections"));

        restartTimers();
    }

    @Override
    public void onDisable() {
        stopTimers();
        StatReader.stop();

        if (isTrackingServer()) {
            for (Track track : Track.values()) {
                track.getLoader().getLoaded().forEach(player -> {
                    try {
                        player.saveAndUnload(false);
                    } catch (Throwable t) {
                        logWarning("Failed to save " + track.getAmountsTable() + " for " + player.getIdentifier() + ": " + t.getMessage());
                    }
                });
            }
            if (placedBlocks != null) placedBlocks.flush(true);
        }

        expansions.forEach(CollectionsExpansion::stop);
        expansions.clear();
        if (listener != null) BaseEventHandler.unbake(listener);
    }

    @Override
    public void registerCommands() {
        setCommands(List.of(
                new CollectionsCommand(),
                new CollectionsLeaderboardCommand(),
                new CollectionsAdminCommand(),
                new SlayerCommand(),
                new SlayerLeaderboardCommand(),
                new SlayerAdminCommand(),
                new ProgressionCommand(),
                new ProgressionBoardCommand(),
                new ProgressionAdminCommand()
        ));
    }

    /** Reloads every config file and restarts the timers. */
    public static void reload() {
        mainConfig.reloadResource(true);
        catalogConfig.reloadResource(true);
        CollectionManager.setCatalog(catalogConfig.load());
        SlayerManager.getConfig().reloadResource(true);
        SlayerManager.loadCatalog();
        ProgressionManager.getConfig().load();
        Boards.clearAll();
        restartTimers();
    }

    private static void restartTimers() {
        stopTimers();
        if (! isTrackingServer()) return;

        timers.add(new SaveTimer(mainConfig.getSaveIntervalSeconds()));
        timers.add(new StatsTimer(ProgressionManager.getConfig().getRefreshIntervalSeconds()));
        timers.add(new ModuleRunnable(instance, 100L, 100L) {
            @Override
            public void run() {
                ProgressionManager.checkStars();
            }
        });

        int remindMinutes = mainConfig.getRemindIntervalMinutes();
        if (remindMinutes > 0) timers.add(new RemindTimer(remindMinutes, CollectionManager::remind));
        int slayerRemindMinutes = SlayerManager.getConfig().getRemindIntervalMinutes();
        if (slayerRemindMinutes > 0) timers.add(new RemindTimer(slayerRemindMinutes, SlayerManager::remind));

        int syncMinutes = mainConfig.getStatSyncIntervalMinutes();
        if (mainConfig.isStatSyncEnabled() && syncMinutes > 0) timers.add(new StatSyncTimer(syncMinutes));
    }

    private static void stopTimers() {
        timers.forEach(ModuleRunnable::cancel);
        timers.clear();
    }
}
