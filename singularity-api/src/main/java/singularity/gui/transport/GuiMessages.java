package singularity.gui.transport;

import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.gui.GuiClickType;
import singularity.gui.GuiManager;
import singularity.gui.GuiView;
import singularity.messages.proxied.ProxiedMessage;
import singularity.modules.ModuleUtils;
import singularity.utils.MessageUtils;

/**
 * The proxy-to-backend transport for {@link singularity.gui.CosmicGui}s, over the API channel's
 * {@link ProxiedMessage}s.
 *
 * <ul>
 *     <li>{@code gui-open} (proxy → backend): render a view, or update it in place.</li>
 *     <li>{@code gui-close} (proxy → backend): close the view the proxy sent.</li>
 *     <li>{@code gui-click} (backend → proxy): a click in a slot the view marked clickable.</li>
 *     <li>{@code gui-closed} (backend → proxy): the player's screen for a view closed.</li>
 * </ul>
 *
 * <p>Proxy-to-backend messages ride a serverbound plugin message, which Minecraft caps at
 * {@value #MAX_PAYLOAD_BYTES} bytes; a larger one would disconnect the player, so it is refused
 * here instead.</p>
 */
public final class GuiMessages {
    private GuiMessages() {
    }

    public static final String OPEN = "gui-open";
    public static final String CLOSE = "gui-close";
    public static final String CLICK = "gui-click";
    public static final String CLOSED = "gui-closed";

    /** The largest serverbound custom payload Minecraft accepts. */
    public static final int MAX_PAYLOAD_BYTES = 32767;

    private static final String KEY_VIEWER = "viewer";
    private static final String KEY_VIEW = "view";
    private static final String KEY_UPDATE = "update";
    private static final String KEY_GUI = "gui";
    private static final String KEY_SLOT = "slot";
    private static final String KEY_CLICK = "click";

    public static boolean isGuiMessage(String subChannel) {
        return OPEN.equals(subChannel) || CLOSE.equals(subChannel)
                || CLICK.equals(subChannel) || CLOSED.equals(subChannel);
    }

    public static boolean sendOpen(CosmicPlayer viewer, GuiView view, boolean update) {
        ProxiedMessage message = new ProxiedMessage(viewer, true);
        message.setSubChannel(OPEN);
        message.write(KEY_VIEWER, viewer.getUuid());
        message.write(KEY_VIEW, view.encode());
        message.write(KEY_UPDATE, String.valueOf(update));
        return send(message, "GUI '" + view.getTitle() + "'");
    }

    public static void sendClose(CosmicPlayer viewer) {
        ProxiedMessage message = new ProxiedMessage(viewer, true);
        message.setSubChannel(CLOSE);
        message.write(KEY_VIEWER, viewer.getUuid());
        send(message, "GUI close");
    }

    public static void sendClick(CosmicPlayer viewer, String guiId, int slot, GuiClickType type) {
        ProxiedMessage message = new ProxiedMessage(viewer, false);
        message.setSubChannel(CLICK);
        message.write(KEY_VIEWER, viewer.getUuid());
        message.write(KEY_GUI, guiId);
        message.write(KEY_SLOT, String.valueOf(slot));
        message.write(KEY_CLICK, type.name());
        send(message, "GUI click");
    }

    public static void sendClosed(CosmicPlayer viewer, String guiId) {
        ProxiedMessage message = new ProxiedMessage(viewer, false);
        message.setSubChannel(CLOSED);
        message.write(KEY_VIEWER, viewer.getUuid());
        message.write(KEY_GUI, guiId);
        send(message, "GUI closed");
    }

    /** Handles an inbound GUI message; anything arriving from the wrong side is dropped. */
    public static void handle(ProxiedMessage message) {
        String subChannel = message.getSubChannel();
        String viewerUuid = message.getString(KEY_VIEWER);
        if (viewerUuid == null) return;

        if (OPEN.equals(subChannel) || CLOSE.equals(subChannel)) {
            if (! message.isProxyOriginated() || Singularity.isProxy()) return;

            CosmicPlayer viewer = ModuleUtils.getOrCreatePlayer(viewerUuid).orElse(null);
            if (viewer == null) return;

            if (CLOSE.equals(subChannel)) {
                GuiManager.closeRemote(viewer);
                return;
            }

            try {
                GuiView view = GuiView.decode(message.getString(KEY_VIEW));
                GuiManager.hostRemote(viewer, view, message.getBoolean(KEY_UPDATE));
            } catch (Exception e) {
                MessageUtils.logWarning("Could not read a GUI the proxy sent for " + viewerUuid, e);
            }
            return;
        }

        if (CLICK.equals(subChannel) || CLOSED.equals(subChannel)) {
            if (message.isProxyOriginated() || ! Singularity.isProxy()) return;

            String guiId = message.getString(KEY_GUI);
            if (CLOSED.equals(subChannel)) {
                GuiManager.handleClosed(viewerUuid, guiId);
                return;
            }

            int slot;
            try {
                slot = Integer.parseInt(message.getString(KEY_SLOT));
            } catch (Exception e) {
                return;
            }
            GuiManager.handleClick(viewerUuid, guiId, slot, GuiClickType.fromName(message.getString(KEY_CLICK)));
        }
    }

    private static boolean hasProxyMessenger() {
        return Singularity.getInstance() != null && Singularity.getInstance().getProxyMessenger() != null;
    }

    private static boolean send(ProxiedMessage message, String what) {
        if (! hasProxyMessenger()) {
            MessageUtils.logWarning("Cannot send " + what + ": this platform has no proxy messenger.");
            return false;
        }

        int size = message.read().length;
        if (size > MAX_PAYLOAD_BYTES) {
            MessageUtils.logWarning("Cannot send " + what + " to " + message.getCarrier().getCurrentName()
                    + ": it encodes to " + size + " bytes, over Minecraft's " + MAX_PAYLOAD_BYTES
                    + "-byte plugin message limit. Use fewer or shorter item names and lore.");
            return false;
        }

        message.send();
        return true;
    }
}
