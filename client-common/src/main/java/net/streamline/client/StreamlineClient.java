package net.streamline.client;

import net.streamline.client.compat.ClientCompat;
import net.streamline.client.gui.HudEditorScreen;
import net.streamline.client.gui.MenuScreen;
import singularity.client.ClientBridge;
import singularity.client.ClientInfo;
import singularity.client.IClientHandler;

/**
 * The game client's side of Streamline: installs the {@link IClientHandler} behind
 * {@link ClientBridge}, and draws the HUD when a loader's HUD hook calls
 * {@link #renderHud(Canvas)}.
 *
 * <p>Only client-only classes (each loader's client listener and HUD hook) may refer to
 * this package; dedicated servers have none of the client classes it uses.</p>
 */
public final class StreamlineClient {

    private static volatile boolean initialized;

    private StreamlineClient() {}

    /** Installs the client handler. Safe to call more than once. */
    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        ClientBridge.setHandler(new Handler());
    }

    /**
     * Draws the HUD elements, unless the GUI is hidden (F1) or the HUD editor, which draws
     * them itself, is open.
     */
    public static void renderHud(Canvas canvas) {
        if (ClientCompat.hudHidden()) return;
        if (ClientCompat.currentScreen() instanceof HudEditorScreen) return;
        HudOverlay.render(canvas);
    }

    private static final class Handler implements IClientHandler {

        /**
         * Commands run while the chat screen is still open, and the chat screen closes
         * itself once the command returns, so a screen set right away would be closed with it.
         * {@link ClientCompat#later(Runnable)} queues the switch for the client's next task run.
         */
        @Override
        public void openMenu() {
            ClientCompat.later(() -> ClientCompat.setScreen(new MenuScreen(ClientCompat.currentScreen())));
        }

        @Override
        public void openHudEditor() {
            ClientCompat.later(() -> ClientCompat.setScreen(new HudEditorScreen(ClientCompat.currentScreen())));
        }

        @Override
        public ClientInfo info() {
            return ClientCompat.clientInfo();
        }
    }
}
