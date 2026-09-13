package net.streamline.base.runnables;

import net.streamline.base.StreamlineSpigot;
import net.streamline.platform.savables.UserManager;
import singularity.data.players.CosmicPlayer;
import singularity.scheduler.BaseRunnable;
import singularity.utils.UserUtils;

/**
 * A periodic task that runs every server tick (period of {@code 1}) and ensures
 * that all online Bukkit players have a corresponding, fully-loaded
 * {@link singularity.data.players.CosmicPlayer} entry.
 *
 * <p>Players that joined before the Streamline data layer finished initialising
 * may be missing from the user cache. This runnable detects those gaps and
 * triggers data loading so that the player is properly tracked.
 */
public class PlayerChecker extends BaseRunnable {
    /**
     * Constructs a new {@code PlayerChecker} with no initial delay and a period
     * of {@code 1} tick.
     */
    public PlayerChecker() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterates over all online players and, for any player whose
     * {@link singularity.data.players.CosmicPlayer} is not yet loaded, creates
     * or fetches the entry, updates the player's IP and name, and calls
     * {@code ensureLoaded()} to trigger any pending persistence.
     */
    @Override
    public void run() {
        StreamlineSpigot.getPlayersByUUID().forEach((uuid, player) -> {
            if (UserUtils.isLoaded(player.getUniqueId().toString())) return;

            CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
            if (streamPlayer == null) return;

            streamPlayer.setCurrentIp(UserManager.getInstance().parsePlayerIP(player));
            streamPlayer.setCurrentName(player.getName());

            streamPlayer.ensureLoaded();
        });
    }
}
