package host.plas.hud;

import singularity.client.ClientBridge;
import singularity.client.ClientInfo;
import singularity.client.hud.TextHudElement;

import java.util.Collections;
import java.util.List;

/**
 * A one-line HUD element whose text is read from {@link ClientBridge#info()} every frame.
 * It shows nothing outside a world; the HUD editor shows its preview line instead.
 */
public final class InfoElement extends TextHudElement {

    /** Builds the line for this frame, or returns {@code null} to show nothing. */
    @FunctionalInterface
    public interface Line {
        String build(ClientInfo info);
    }

    private final Line line;
    private final List<String> preview;

    public InfoElement(String id, String displayName, double defaultX, double defaultY, String previewLine, Line line) {
        super(id, displayName, defaultX, defaultY);
        this.line = line;
        this.preview = Collections.singletonList(previewLine);
    }

    @Override
    protected List<String> lines() {
        ClientInfo info = ClientBridge.info();
        if (! info.inWorld()) return Collections.emptyList();
        String text = line.build(info);
        return text == null || text.isEmpty() ? Collections.emptyList() : Collections.singletonList(text);
    }

    @Override
    protected List<String> previewLines() {
        return preview;
    }
}
