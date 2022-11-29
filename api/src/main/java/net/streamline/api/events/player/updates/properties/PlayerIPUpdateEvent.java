package net.streamline.api.events.player.updates.properties;

import net.streamline.api.events.player.updates.PlayerPropertyUpdateEvent;

import java.net.InetAddress;

public class PlayerIPUpdateEvent extends PlayerPropertyUpdateEvent<InetAddress> {
    public PlayerIPUpdateEvent(String playerUuid, InetAddress newValue) {
        super(playerUuid, newValue);
    }
}
