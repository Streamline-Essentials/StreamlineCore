package net.streamline.base.runnables;

import net.streamline.base.StreamlineVelocity;
import net.streamline.platform.savables.UserManager;
import singularity.data.players.CosmicPlayer;
import singularity.scheduler.BaseRunnable;
import singularity.utils.UserUtils;

/**
 * A periodic task that runs every tick (delay 0, period 1) and ensures that every
 * connected Velocity {@link com.velocitypowered.api.proxy.Player} has a fully
 * initialised and loaded {@link CosmicPlayer} counterpart.
 *
 * <p>For each online player whose {@link CosmicPlayer} is not yet marked as loaded,
 * the task resolves or creates the profile, populates IP, name, and current server,
 * then calls {@link CosmicPlayer#ensureLoaded()} to finalise the loading sequence.
 */
public class PlayerChecker extends BaseRunnable {
    /**
     * Constructs a new {@code PlayerChecker} scheduled to start immediately (delay 0)
     * and repeat every tick (period 1).
     */
    public PlayerChecker() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterates all online players and, for those whose {@link CosmicPlayer} is not yet
     * fully loaded, retrieves or creates the profile, updates the current IP, username,
     * and server name, then marks the profile as loaded.
     */
    @Override
    public void run() {
        StreamlineVelocity.getPlayersByUUID().forEach((uuid, player) -> {
            if (UserUtils.isLoaded(player.getUniqueId().toString())) return;

            CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
            if (streamPlayer == null) return;

            streamPlayer.setCurrentIp(UserManager.getInstance().parsePlayerIP(player.getUniqueId().toString()));
            streamPlayer.setCurrentName(player.getUsername());
            player.getCurrentServer().ifPresent(serverConnection -> streamPlayer.setServerName(serverConnection.getServerInfo().getName()));

            streamPlayer.ensureLoaded();
        });
    }
}
