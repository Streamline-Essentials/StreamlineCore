package net.streamline.base.events;

import gg.drak.thebase.events.processing.BaseProcessor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.base.listeners.BaseListener;
import net.streamline.base.Streamline;
import singularity.events.player.updates.properties.PlayerIPUpdateEvent;
import singularity.utils.MessageUtils;

public class BungeeBaseListener extends BaseListener {
    public BungeeBaseListener() {
        MessageUtils.logInfo("BungeeBaseListener initialized.");
    }

    @BaseProcessor
    public void onPlayerIPUpdateEvent(PlayerIPUpdateEvent event) {
        ProxiedPlayer player = Streamline.getPlayer(event.getPlayerUuid());
        if (player == null) {
            MessageUtils.logWarning("PlayerIPUpdateEvent: Player is null!");
            return;
        }

//        player.setp // TODO: fix.
    }
}
