package singularity.gui;

import singularity.data.players.CosmicPlayer;

/**
 * A platform's renderer for {@link CosmicGui}s. Set by backend platforms (Spigot, Fabric, Forge,
 * NeoForge); proxies have none and hand GUIs to the player's backend instead.
 *
 * <p>Implementations may be called from any thread and move the work onto the player's own
 * thread themselves. They report what the player does through
 * {@link GuiManager#handleClick(String, String, int, GuiClickType)} and
 * {@link GuiManager#handleClosed(String, String)}, passing the {@link GuiView#getId() id} of the
 * view the player was looking at. A screen replaced by another, or reopened to change its size or
 * title, must not report a close for the view that replaced it.</p>
 */
public interface IGuiHandler {
    /** Opens {@code view} for {@code viewer}, replacing whatever screen they have open. */
    boolean open(CosmicPlayer viewer, GuiView view);

    /**
     * Shows {@code view} to a viewer who may already have it open. When the open screen shows a
     * view with the same id and a {@linkplain GuiView#isLayoutCompatible compatible layout}, its
     * items are swapped in place; otherwise this behaves like {@link #open}.
     */
    boolean update(CosmicPlayer viewer, GuiView view);

    /** Closes whatever GUI {@code viewer} has open. */
    void close(CosmicPlayer viewer);
}
