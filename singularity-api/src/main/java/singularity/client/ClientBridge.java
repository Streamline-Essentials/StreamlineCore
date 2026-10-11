package singularity.client;

import lombok.Getter;
import lombok.Setter;
import singularity.client.hud.HudRegistry;
import singularity.client.settings.ClientSettingsRegistry;

/**
 * Entry point for client modules: whether Streamline runs in a game client, what that client
 * knows, and its menu and HUD editor.
 *
 * <p>Only a mod-loader client installs a {@link IClientHandler}. Everywhere else every
 * method here does nothing, so a module tagged for both servers and clients can call it
 * without checking first. HUD elements and settings registered on a server are kept but
 * never shown.</p>
 */
public final class ClientBridge {

    @Getter
    @Setter
    private static volatile IClientHandler handler;

    private ClientBridge() {}

    /** @return whether Streamline runs in a game client */
    public static boolean isClient() {
        return handler != null;
    }

    /** @return the client's live values, or {@link ClientInfo#NONE} off a client */
    public static ClientInfo info() {
        IClientHandler current = handler;
        if (current == null) return ClientInfo.NONE;
        ClientInfo info = current.info();
        return info == null ? ClientInfo.NONE : info;
    }

    public static void openMenu() {
        IClientHandler current = handler;
        if (current != null) current.openMenu();
    }

    public static void openHudEditor() {
        IClientHandler current = handler;
        if (current != null) current.openHudEditor();
    }

    /**
     * Drops every HUD element and settings page a module registered. Runs when the module
     * stops, so nothing on the render thread calls into a module that is gone.
     */
    public static void forgetModule(String moduleId) {
        HudRegistry.unregisterAll(moduleId);
        ClientSettingsRegistry.unregister(moduleId);
    }
}
