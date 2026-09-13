package singularity.interfaces;

import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;

/**
 * Contract for backend (Spigot/Bukkit) specific operations that cannot be performed
 * through the proxy-agnostic API layer.
 *
 * <p>Implementations are registered per platform and invoked when a cross-platform
 * action requires direct access to a backend server, such as performing a world teleport.</p>
 */
public interface IBackendHandler {

    /**
     * Teleports the given player to the specified location on the backend server.
     *
     * <p>The location must belong to a world that is loaded on the backend server
     * that the player is currently connected to.</p>
     *
     * @param player   the player to teleport; must be online
     * @param location the target location, including world, coordinates, and optionally rotation
     */
    public void teleport(CosmicPlayer player, CosmicLocation location);
}
