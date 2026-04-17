package singularity.events.server;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.CosmicSenderEvent;

/**
 * Fired when a player is kicked from a server and optionally redirected to
 * another server.
 *
 * <p>The event carries the name of the server the player was removed from, the
 * human-readable kick reason, and the name of the server they are being sent
 * to as a fallback (or {@code "none"} if no redirect is configured).</p>
 */
@Setter
@Getter
public class KickedFromServerEvent extends CosmicSenderEvent {

    /**
     * The name of the server from which the player was kicked, or {@code "none"}
     * if no originating server is recorded.
     */
    private String fromServer;

    /**
     * The human-readable reason the player was kicked from the server.
     */
    private String reason;

    /**
     * The name of the server the player is being redirected to after the kick,
     * or {@code "none"} if no redirect destination is configured.
     */
    private String toServer;

    /**
     * Constructs a new {@code KickedFromServerEvent}.
     *
     * @param player     the player that was kicked; must not be {@code null}
     * @param fromServer the name of the server the player was kicked from
     * @param reason     the kick reason shown to the player
     * @param toServer   the fallback server the player is redirected to, or
     *                   {@code "none"} if there is no redirect
     */
    public KickedFromServerEvent(CosmicPlayer player, String fromServer, String reason, String toServer) {
        super(player);
        this.fromServer = fromServer;
        this.reason = reason;
        this.toServer = toServer;
    }

    /**
     * Returns {@code true} if the originating server is recorded as
     * {@code "none"} (case-insensitive), indicating that no specific source
     * server was set.
     *
     * @return {@code true} when {@link #fromServer} equals {@code "none"}
     *         ignoring case, {@code false} otherwise
     */
    public boolean isFromServerNone() {
        return this.fromServer.equalsIgnoreCase("none");
    }

    /**
     * Returns {@code true} if the redirect destination is recorded as
     * {@code "none"} (case-insensitive), indicating the player will not be
     * forwarded to another server.
     *
     * @return {@code true} when {@link #toServer} equals {@code "none"}
     *         ignoring case, {@code false} otherwise
     */
    public boolean isToServerNone() {
        return this.toServer.equalsIgnoreCase("none");
    }
}
