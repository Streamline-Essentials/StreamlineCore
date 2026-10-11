package host.plas.toggles;

import singularity.client.ClientBridge;
import singularity.client.hud.TextHudElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * A HUD element showing text the player can edit in the module menu, with {@link Placeholders}
 * filled in every frame. It shows nothing outside a world.
 */
public final class ToggleElement extends TextHudElement {

    private final String defaultTemplate;
    /** Told about every template the player sets, to keep it. */
    private final Consumer<ToggleElement> onEdit;
    private volatile String template;
    /** {@link #template} split at {@link Placeholders#NEWLINE}, so a frame only fills in placeholders. */
    private volatile List<String> lines;

    public ToggleElement(String id, String displayName, double defaultX, double defaultY, String defaultTemplate,
                         Consumer<ToggleElement> onEdit) {
        super(id, displayName, defaultX, defaultY);
        this.defaultTemplate = defaultTemplate;
        this.onEdit = onEdit;
        load(null);
    }

    @Override
    protected List<String> lines() {
        if (! ClientBridge.info().inWorld()) return Collections.emptyList();
        return render();
    }

    @Override
    protected List<String> previewLines() {
        List<String> rendered = render();
        return rendered.isEmpty() ? Collections.singletonList(getDisplayName()) : rendered;
    }

    private List<String> render() {
        List<String> source = lines;
        List<String> out = new ArrayList<>(source.size());
        for (String line : source) {
            String filled = Placeholders.apply(line);
            if (! filled.isEmpty()) out.add(filled);
        }
        return out;
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public String getDefaultTemplate() {
        return defaultTemplate;
    }

    @Override
    public void setTemplate(String text) {
        load(text);
        if (onEdit != null) onEdit.accept(this);
    }

    /** Sets the text without reporting it, for text read back from where it was kept. */
    public void load(String text) {
        String chosen = text == null || text.isEmpty() ? defaultTemplate : text;
        lines = Arrays.asList(chosen.split(Pattern.quote(Placeholders.NEWLINE), -1));
        template = chosen;
    }

    /** @return whether the player changed the text from the default */
    public boolean isEdited() {
        return ! template.equals(defaultTemplate);
    }

    @Override
    public Map<String, String> getPlaceholders() {
        return Placeholders.descriptions();
    }
}
