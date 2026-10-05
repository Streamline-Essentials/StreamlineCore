package host.plas.collections;

import host.plas.collections.commands.CollectionsAdminCommand;
import host.plas.collections.commands.CollectionsCommand;
import host.plas.collections.commands.CollectionsLeaderboardCommand;
import host.plas.collections.config.CatalogConfig;
import host.plas.collections.config.CollectionsConfig;
import host.plas.collections.data.CollectionManager;
import host.plas.collections.data.PlacedBlocks;
import host.plas.collections.database.CollectionKeeper;
import host.plas.collections.database.CollectionLoader;
import host.plas.collections.listeners.CollectionsListener;
import host.plas.collections.placeholders.CollectionsExpansion;
import host.plas.collections.timers.RemindTimer;
import host.plas.collections.timers.SaveTimer;
import host.plas.collections.timers.StatSyncTimer;
import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.Singularity;
import singularity.modules.SimpleModule;

import java.util.List;

/**
 * Collections: players gather items by breaking blocks, killing mobs, fishing and filling
 * buckets; each collection has levels whose rewards are claimed from a menu.
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
    private static CollectionKeeper keeper;
    @Getter @Setter
    private static CollectionLoader loader;

    @Getter @Setter
    private static PlacedBlocks placedBlocks;
    @Getter @Setter
    private static CollectionsListener listener;
    @Getter @Setter
    private static CollectionsExpansion expansion;

    @Getter @Setter
    private static SaveTimer saveTimer;
    @Getter @Setter
    private static RemindTimer remindTimer;
    @Getter @Setter
    private static StatSyncTimer statSyncTimer;

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

        keeper = new CollectionKeeper();
        loader = new CollectionLoader();

        if (isTrackingServer()) {
            placedBlocks = new PlacedBlocks(getDataFolder());
        }

        listener = new CollectionsListener();
        expansion = new CollectionsExpansion();

        restartTimers();
    }

    @Override
    public void onDisable() {
        stopTimers();

        if (isTrackingServer()) {
            getLoader().getLoaded().forEach(player -> {
                try {
                    player.saveAndUnload(false);
                } catch (Throwable t) {
                    logWarning("Failed to save collections for " + player.getIdentifier() + ": " + t.getMessage());
                }
            });
            if (placedBlocks != null) placedBlocks.flush(true);
        }

        if (expansion != null) expansion.stop();
    }

    @Override
    public void registerCommands() {
        setCommands(List.of(
                new CollectionsCommand(),
                new CollectionsLeaderboardCommand(),
                new CollectionsAdminCommand()
        ));
    }

    /** Reloads both config files and restarts the timers. */
    public static void reload() {
        mainConfig.reloadResource(true);
        catalogConfig.reloadResource(true);
        CollectionManager.setCatalog(catalogConfig.load());
        CollectionManager.clearBoards();
        restartTimers();
    }

    private static void restartTimers() {
        stopTimers();
        if (! isTrackingServer()) return;

        saveTimer = new SaveTimer(mainConfig.getSaveIntervalSeconds());
        int remindMinutes = mainConfig.getRemindIntervalMinutes();
        if (remindMinutes > 0) remindTimer = new RemindTimer(remindMinutes);

        int syncMinutes = mainConfig.getStatSyncIntervalMinutes();
        if (mainConfig.isStatSyncEnabled() && syncMinutes > 0) statSyncTimer = new StatSyncTimer(syncMinutes);
    }

    private static void stopTimers() {
        if (saveTimer != null) saveTimer.cancel();
        if (remindTimer != null) remindTimer.cancel();
        if (statSyncTimer != null) statSyncTimer.cancel();
        saveTimer = null;
        remindTimer = null;
        statSyncTimer = null;
    }
}
