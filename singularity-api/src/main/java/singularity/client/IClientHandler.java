package singularity.client;

/**
 * The game client's side of {@link ClientBridge}, installed by the mod-loader client.
 */
public interface IClientHandler {

    /** Opens the client module menu on the client thread. */
    void openMenu();

    /** Opens the HUD layout editor on the client thread. */
    void openHudEditor();

    ClientInfo info();
}
