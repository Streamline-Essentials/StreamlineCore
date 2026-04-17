package singularity.events.player.updates.properties;

import singularity.events.player.updates.PlayerPropertyUpdateEvent;

import java.net.InetAddress;

/**
 * Fired when a player's stored IP address is about to be updated.
 *
 * <p>This is a typed specialisation of {@link PlayerPropertyUpdateEvent} for
 * {@link InetAddress}, raised during the login/connection phase when the platform
 * reports a new address for the player.</p>
 */
public class PlayerIPUpdateEvent extends PlayerPropertyUpdateEvent<InetAddress> {

    /**
     * Constructs a {@code PlayerIPUpdateEvent} for the given player UUID and new address.
     *
     * @param playerUuid the UUID of the player whose IP is being updated;
     *                   must not be {@code null}
     * @param newValue   the new {@link InetAddress} to assign to the player
     */
    public PlayerIPUpdateEvent(String playerUuid, InetAddress newValue) {
        super(playerUuid, newValue);
    }
}
