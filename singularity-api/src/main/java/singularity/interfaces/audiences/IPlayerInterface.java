package singularity.interfaces.audiences;

import singularity.interfaces.audiences.getters.PlayerGetter;
import singularity.interfaces.audiences.real.RealPlayer;

import java.util.UUID;

/**
 * Platform-agnostic interface for looking up online players.
 *
 * <p>Provides factory methods that create {@link PlayerGetter} instances for a given
 * {@link UUID} or player name, and convenience methods that resolve those getters into
 * fully-wrapped {@link RealPlayer} objects that can be used throughout the framework.</p>
 *
 * @param <P> the platform player type (e.g., the Velocity {@code Player} or Bukkit
 *            {@code Player} class)
 */
public interface IPlayerInterface<P> {

    /**
     * Creates a {@link PlayerGetter} that, when called, will supply the online player
     * identified by the given {@link UUID}.
     *
     * @param uuid the unique ID of the player to look up
     * @return a getter whose {@code get()} method returns the player, or {@code null} if offline
     */
    PlayerGetter<P> getPlayerGetter(UUID uuid);

    /**
     * Creates a {@link PlayerGetter} that, when called, will supply the online player
     * identified by the given display/login name.
     *
     * @param playerName the name of the player to look up (case sensitivity is
     *                   platform-dependent)
     * @return a getter whose {@code get()} method returns the player, or {@code null} if offline
     */
    PlayerGetter<P> getPlayerGetter(String playerName);

    /**
     * Resolves a {@link RealPlayer} for the player with the given {@link UUID}.
     *
     * <p>Internally creates a getter via {@link #getPlayerGetter(UUID)} and delegates to
     * {@link #getPlayer(PlayerGetter)}.</p>
     *
     * @param uuid the unique ID of the player to look up
     * @return the wrapped player, or {@code null} if the player is not online
     */
    default RealPlayer<P> getPlayer(UUID uuid) {
        return getPlayer(getPlayerGetter(uuid));
    }

    /**
     * Resolves a {@link RealPlayer} for the player with the given name.
     *
     * <p>Internally creates a getter via {@link #getPlayerGetter(String)} and delegates to
     * {@link #getPlayer(PlayerGetter)}.</p>
     *
     * @param playerName the name of the player to look up
     * @return the wrapped player, or {@code null} if the player is not online
     */
    default RealPlayer<P> getPlayer(String playerName) {
        return getPlayer(getPlayerGetter(playerName));
    }

    /**
     * Resolves a {@link RealPlayer} from a pre-built {@link PlayerGetter}.
     *
     * <p>Platform implementations should call {@code playerGetter.get()} and wrap the result
     * in an appropriate {@link RealPlayer} subclass.</p>
     *
     * @param playerGetter the getter supplying the raw platform player object
     * @return the wrapped player, or {@code null} if the getter returns {@code null}
     */
    RealPlayer<P> getPlayer(PlayerGetter<P> playerGetter);
}
