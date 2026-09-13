package net.streamline.base.runnables;

import net.md_5.bungee.api.connection.Server;
import net.streamline.base.StreamlineBungee;
import net.streamline.platform.savables.UserManager;
import singularity.data.players.CosmicPlayer;
import singularity.scheduler.BaseRunnable;
import singularity.utils.UserUtils;

/**
 * A high-frequency periodic task that runs every server tick to ensure that
 * every online BungeeCord player has a fully initialised {@link CosmicPlayer}
 * record in memory. Players that are already loaded are skipped to keep
 * overhead minimal.
 *
 * <p>For each unloaded player the task:
 * <ol>
 *   <li>Gets or creates the corresponding {@link CosmicPlayer}.</li>
 *   <li>Updates the player's current IP, name, and connected server name.</li>
 *   <li>Calls {@code ensureLoaded()} to finish any deferred initialisation.</li>
 * </ol>
 */
public class PlayerChecker extends BaseRunnable {

    /**
     * Constructs a new {@code PlayerChecker} that starts immediately with no
     * initial delay and repeats every tick (period {@code 1}).
     */
    public PlayerChecker() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Iterates over all online BungeeCord players. Players whose Streamline
     * record is already loaded are skipped. For the rest, a {@link CosmicPlayer}
     * is created (or retrieved), its connection properties are updated, and
     * {@code ensureLoaded()} is called to complete initialisation.
     */
    @Override
    public void run() {
        StreamlineBungee.getPlayersByUUID().forEach((uuid, player) -> {
            if (UserUtils.isLoaded(player.getUniqueId().toString())) return;

            CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
            if (streamPlayer == null) return;

            streamPlayer.setCurrentIp(UserManager.getInstance().parsePlayerIP(player));
            streamPlayer.setCurrentName(player.getName());
            Server server = player.getServer();
            if (server != null) streamPlayer.setServerName(server.getInfo().getName());

            streamPlayer.ensureLoaded();
        });
    }
}
