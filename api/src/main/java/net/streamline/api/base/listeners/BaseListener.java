package net.streamline.api.base.listeners;

import gg.drak.thebase.events.BaseEventHandler;
import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import singularity.Singularity;
import net.streamline.api.base.module.BaseModule;
import singularity.data.players.CosmicPlayer;
import singularity.events.server.LoginCompletedEvent;
import singularity.events.server.LogoutEvent;
import singularity.utils.UserUtils;
import singularity.gui.GuiManager;
import singularity.messages.events.ProxyMessageInEvent;
import singularity.messages.proxied.ProxiedMessageManager;

/**
 * Core cross-platform event listener for the base Streamline module.
 *
 * <p>Implements {@link BaseEventListener} from the TheBase event system and is
 * baked into the global event bus during construction.  Currently handles
 * inbound proxy plugin-messaging channel events and routes them through the
 * {@link singularity.messages.proxied.ProxiedMessageManager}.</p>
 */
public class BaseListener implements BaseEventListener {

    /**
     * Instantiates the listener and registers it with the TheBase event handler
     * so that it receives events fired on the {@link singularity.Singularity}
     * instance.
     */
    public BaseListener() {
        BaseModule.getInstance().logInfo("Loaded " + getClass().getSimpleName());
        BaseEventHandler.bake(this, Singularity.getInstance());
    }

    /**
     * Handles an inbound proxy plugin-messaging event.
     *
     * <p>Delegates to {@link singularity.messages.proxied.ProxiedMessageManager#onProxiedMessageReceived}
     * so the payload can be dispatched to the appropriate registered handler.
     * Events with a {@code null} message or sub-channel are silently ignored.</p>
     *
     * @param event the event carrying the raw proxy message and sub-channel
     */
    @BaseProcessor
    public void onProxyMessage(ProxyMessageInEvent event) {
        if (event.getMessage() == null) return;
        if (event.getSubChannel() == null) return;

        ProxiedMessageManager.onProxiedMessageReceived(event.getMessage());
    }

    /**
     * Copies the player's LuckPerms prefix and suffix into their meta once they are fully
     * logged in, by which time LuckPerms holds them; the copy made while their data loaded
     * may have come too early.
     *
     * @param event the login event
     */
    @BaseProcessor
    public void onLoginCompleted(LoginCompletedEvent event) {
        if (! (event.getSender() instanceof CosmicPlayer)) return;

        UserUtils.syncChatMeta((CosmicPlayer) event.getSender());
    }

    /**
     * Drops any GUI state held for a player who left.
     *
     * @param event the logout event
     */
    @BaseProcessor
    public void onLogout(LogoutEvent event) {
        if (event.getPlayer() == null) return;

        GuiManager.forget(event.getPlayer().getUuid());
    }
}
