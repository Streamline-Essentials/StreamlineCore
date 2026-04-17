package net.streamline.base;

import host.plas.bou.scheduling.BaseRunnable;
import lombok.Getter;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.server.CosmicServer;
import singularity.messages.builders.PlayerLocationMessageBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import singularity.utils.UserUtils;

/**
 * A repeating task that fires every 10 seconds (200 ticks) for a single
 * online player and synchronises their Bukkit {@link Location} to the
 * cross-platform {@link singularity.data.players.location.CosmicLocation}
 * representation.
 *
 * <p>The task cancels itself automatically whenever the tracked player goes
 * offline or their {@link singularity.data.players.CosmicPlayer} entry cannot
 * be found.
 */
@Getter
public class TenSecondTimer extends BaseRunnable {
    /**
     * The Bukkit {@link Player} whose location this timer tracks.
     * Lombok generates a {@code getPlayer()} accessor via the class-level
     * {@code @Getter}.
     */
    final Player player;

    /**
     * Constructs a new {@code TenSecondTimer} for the given player with an
     * initial delay and period of 10 seconds (200 ticks).
     *
     * @param player the online Bukkit player to track
     */
    public TenSecondTimer(Player player) {
        super(20 * 10, 20 * 10); // Initial delay and period of 10 seconds (20 ticks = 1 second)
        this.player = player;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the player's current {@link Location}, converts it to a
     * {@link singularity.data.players.location.CosmicLocation}, persists it on
     * the {@link singularity.data.players.CosmicPlayer}, and broadcasts the
     * update via {@link singularity.messages.builders.PlayerLocationMessageBuilder}.
     * The task cancels itself if the player is no longer online.
     */
    @Override
    public void run() {
        if (! checkPlayer()) return;

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
        if (streamPlayer == null) {
            cancel();
            return;
        }

        CosmicServer cosmicServer = streamPlayer.getServer();
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) world = Bukkit.getWorlds().get(0);
//            StreamServer streamlineServer = new StreamServer("--null");
        PlayerWorld streamlineWorld = new PlayerWorld(world.getName());
        WorldPosition streamlinePosition = new WorldPosition(location.getX(), location.getY(), location.getZ());
        PlayerRotation streamlineRotation = new PlayerRotation(location.getYaw(), location.getPitch());

        CosmicLocation streamlineLocation = new CosmicLocation(cosmicServer, streamlineWorld, streamlinePosition, streamlineRotation);

        streamPlayer.setLocation(streamlineLocation);

        PlayerLocationMessageBuilder.build(streamPlayer, streamlineLocation, streamPlayer).send();
    }

    /**
     * Verifies that the tracked player is still available and online.
     *
     * <p>Cancels this task and returns {@code false} if the player reference is
     * {@code null} or the player has gone offline.
     *
     * @return {@code true} if the player is non-null and currently online;
     *         {@code false} otherwise
     */
    public boolean checkPlayer() {
        if (player == null) {
            cancel();
            return false;
        }
        if (! player.isOnline()) {
            cancel();
            return false;
        }
        return true;
    }
}
