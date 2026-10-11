package singularity.client.hud;

/**
 * Lets the player add text elements of their own to a module's HUD from the module menu. A
 * module offers it with {@link HudRegistry#setCustomElements(String, CustomHudElements)}; the
 * menu then shows an add button by that module's HUD elements and a delete button on each
 * element whose {@link HudElement#isCustom()} is true.
 *
 * <p>Both methods run on the render thread, so they must be quick. The module registers the
 * new element itself, keeps it across restarts, and places it at (0, 0) to start.</p>
 */
public interface CustomHudElements {

    /**
     * Adds and registers a new element.
     *
     * @param id       what the player typed as its identifier
     * @param template what the player typed as its text
     * @return {@code null} once added, otherwise why it was not, to show the player
     */
    String add(String id, String template);

    /**
     * Unregisters and forgets an element the player added.
     *
     * @return {@code null} once removed, otherwise why it was not
     */
    String remove(String elementId);
}
