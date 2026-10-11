package singularity.client.hud;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * A {@link HudElement} made of lines of text, drawn over an optional background.
 */
public abstract class TextHudElement extends HudElement {

    private static final int PADDING = 2;

    /** ARGB background behind the text; 0 draws none. */
    @Getter
    @Setter
    private volatile int background = 0x60000000;
    @Getter
    @Setter
    private volatile int textColor = 0xFFFFFFFF;
    @Getter
    @Setter
    private volatile boolean shadow = true;

    protected TextHudElement(String id, String displayName, double defaultX, double defaultY) {
        super(id, displayName, defaultX, defaultY);
    }

    /**
     * @return the lines to show now, with {@code &} color codes; empty hides the element
     */
    protected abstract List<String> lines();

    /** @return what the HUD editor shows while {@link #lines()} is empty */
    protected List<String> previewLines() {
        return Collections.singletonList(getDisplayName());
    }

    private List<String> current(boolean preview) {
        List<String> lines = lines();
        if ((lines == null || lines.isEmpty()) && preview) return previewLines();
        return lines == null ? Collections.emptyList() : lines;
    }

    @Override
    public void render(HudRenderer renderer, boolean preview) {
        List<String> lines = current(preview);
        if (lines.isEmpty()) return;
        int width = width(renderer, lines);
        int height = height(renderer, lines);
        if (background != 0) renderer.fill(0, 0, width, height, background);
        int y = PADDING;
        for (String line : lines) {
            renderer.text(line, PADDING, y, textColor, shadow);
            y += renderer.lineHeight() + 1;
        }
    }

    @Override
    public int width(HudRenderer renderer, boolean preview) {
        return width(renderer, current(preview));
    }

    @Override
    public int height(HudRenderer renderer, boolean preview) {
        return height(renderer, current(preview));
    }

    private int width(HudRenderer renderer, List<String> lines) {
        if (lines.isEmpty()) return 0;
        int max = 0;
        for (String line : lines) max = Math.max(max, renderer.textWidth(line));
        return max + PADDING * 2;
    }

    private int height(HudRenderer renderer, List<String> lines) {
        if (lines.isEmpty()) return 0;
        return lines.size() * (renderer.lineHeight() + 1) - 1 + PADDING * 2;
    }
}
