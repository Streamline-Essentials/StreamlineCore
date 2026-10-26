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
    /**
     * Shows {@code view} to {@code viewer}, replacing whatever they have open. When that is a
     * Streamline GUI with a {@linkplain GuiView#isLayoutCompatible compatible layout}, its items
     * (and title) are swapped in place instead of the window closing and reopening.
     */
    boolean open(CosmicPlayer viewer, GuiView view);

    /** Shows {@code view} to a viewer who may already have it open; the same as {@link #open}. */
    boolean update(CosmicPlayer viewer, GuiView view);

    /** Closes whatever GUI {@code viewer} has open. */
    void close(CosmicPlayer viewer);
}
