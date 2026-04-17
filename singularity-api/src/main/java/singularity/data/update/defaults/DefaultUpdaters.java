package singularity.data.update.defaults;

import lombok.Getter;
import lombok.Setter;

/**
 * Static registry that holds and initializes the framework's built-in
 * {@link singularity.data.update.UpdateType} instances.
 *
 * <p>Call {@link #init()} once during startup to create and start the
 * default updaters. Individual updaters can then be retrieved via their
 * respective getters.</p>
 */
public class DefaultUpdaters {

    /**
     * The updater responsible for periodically pulling and pushing {@link singularity.data.players.CosmicPlayer}
     * data to the main database.
     */
    @Getter @Setter
    private static CosmicPlayerUpdater playerUpdater;

    /**
     * Creates and loads all built-in updaters.
     * This method should be called once during the plugin's initialization phase.
     */
    public static void init() {
        playerUpdater = new CosmicPlayerUpdater();
        playerUpdater.load();
    }
}
