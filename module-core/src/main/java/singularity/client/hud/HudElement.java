package singularity.client.hud;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * Something drawn on a game client's HUD, which the player can move, scale and hide in the
 * HUD editor. Register it with {@link HudRegistry#register(String, HudElement)}.
 *
 * <p>{@link #render}, {@link #width} and {@link #height} run on the render thread every
 * frame, so they must be cheap and must not block.</p>
 *
 * <p>An element whose text the player may edit returns it from {@link #getTemplate()}; the
 * module menu then shows a field for it, with {@link #getPlaceholders()} listed as help.
 * These methods have defaults, so a module that overrides them still loads on a core that
 * predates them, where they are never called.</p>
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

    /** @return the text the player can edit in the module menu, or {@code null} if there is none */
    public String getTemplate() {
        return null;
    }

    /** @return what {@link #getTemplate()} goes back to when the player resets it */
    public String getDefaultTemplate() {
        return null;
    }

    /**
     * Sets the player's text, called on the render thread when they finish typing. The
     * element keeps it across restarts itself.
     *
     * @param template the new text; empty puts back {@link #getDefaultTemplate()}
     */
    public void setTemplate(String template) {
    }

    /** @return the placeholders the template understands, token to meaning, in display order */
    public Map<String, String> getPlaceholders() {
        return Collections.emptyMap();
    }

    /**
     * @return whether the player added this element through the module's
     * {@link CustomHudElements}, which lets them delete it again
     */
    public boolean isCustom() {
        return false;
    }
}
