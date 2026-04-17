package net.streamline.platform.handlers;

import singularity.data.players.CosmicPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import singularity.data.players.location.CosmicLocation;
import singularity.interfaces.IBackendHandler;

import java.util.UUID;

/**
 * Spigot implementation of {@link singularity.interfaces.IBackendHandler} that
 * executes backend-only operations (such as teleportation) using the Bukkit API.
 */
public class BackendHandler implements IBackendHandler {
    /**
     * {@inheritDoc}
     *
     * <p>Looks up the Bukkit {@link Player} for the given {@link CosmicPlayer}
     * by UUID and teleports them to the world, coordinates, and rotation
     * specified in {@code location}. Does nothing if the player is not online
     * or the target world does not exist.
     */
    @Override
    public void teleport(CosmicPlayer player, CosmicLocation location) {
        Player p = Bukkit.getPlayer(UUID.fromString(player.getUuid()));
        if (p == null) return;

        Location l = new Location(Bukkit.getWorld(location.getWorldName()), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        p.teleport(l);
    }
}
