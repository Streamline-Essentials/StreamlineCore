package singularity.client.hud;

import lombok.Getter;

/**
 * Something drawn on a game client's HUD, which the player can move, scale and hide in the
 * HUD editor. Register it with {@link HudRegistry#register(String, HudElement)}.
 *
 * <p>{@link #render}, {@link #width} and {@link #height} run on the render thread every
 * frame, so they must be cheap and must not block.</p>
 */
@Getter
public abstract class HudElement {

    /** Unique among the owning module's elements. */
    private final String id;
    private final String displayName;
    /** Where the element starts, as a fraction of the screen width and height (0..1). */
    private final double defaultX;
    private final double defaultY;

    protected HudElement(String id, String displayName, double defaultX, double defaultY) {
        this.id = id;
        this.displayName = displayName;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
    }

    /**
     * Draws the element with its top-left corner at (0, 0).
     *
     * @param preview whether this is the HUD editor, where an element with nothing to show
     *                should still draw something to grab
     */
    public abstract void render(HudRenderer renderer, boolean preview);

    /** @return the unscaled width in pixels */
    public abstract int width(HudRenderer renderer, boolean preview);

    /** @return the unscaled height in pixels */
    public abstract int height(HudRenderer renderer, boolean preview);

    /** @return whether to draw it at all right now; the editor shows it regardless */
    public boolean isVisible() {
        return true;
    }
}
