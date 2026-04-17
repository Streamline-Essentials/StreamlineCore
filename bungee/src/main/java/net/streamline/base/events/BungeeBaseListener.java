package net.streamline.base.events;

import gg.drak.thebase.events.processing.BaseProcessor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.base.listeners.BaseListener;
import net.streamline.base.StreamlineBungee;
import singularity.events.player.updates.properties.PlayerIPUpdateEvent;
import singularity.utils.MessageUtils;

/**
 * BungeeCord platform listener that handles base Streamline cross-platform
 * events and bridges them to BungeeCord-native operations. Registered during
 * plugin start-up via the Streamline event bus.
 */
public class BungeeBaseListener extends BaseListener {

    /**
     * Constructs a new {@code BungeeBaseListener} and logs a confirmation
     * message to indicate it has been successfully registered.
     */
    public BungeeBaseListener() {
        MessageUtils.logInfo("BungeeBaseListener initialized.");
    }

    /**
     * Handles a {@link PlayerIPUpdateEvent} by resolving the corresponding
     * BungeeCord {@link ProxiedPlayer} and applying the IP change. If the
     * player cannot be resolved a warning is logged and no further action
     * is taken.
     *
     * <p><b>Note:</b> the actual IP assignment is pending a BungeeCord API
     * fix and is currently not implemented.
     *
     * @param event the player IP update event carrying the player UUID and new IP
     */
    @BaseProcessor
    public void onPlayerIPUpdateEvent(PlayerIPUpdateEvent event) {
        ProxiedPlayer player = StreamlineBungee.getPlayer(event.getPlayerUuid());
        if (player == null) {
            MessageUtils.logWarning("PlayerIPUpdateEvent: Player is null!");
            return;
        }

//        player.setp // TODO: fix.
    }
}
