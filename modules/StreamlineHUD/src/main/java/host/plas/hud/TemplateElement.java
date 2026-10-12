package host.plas.hud;

import singularity.client.ClientBridge;
import singularity.client.ClientInfo;
import singularity.client.hud.TextHudElement;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A HUD element showing text the player can edit, with {@link Placeholders} filled in from
 * {@link ClientBridge#info()} every frame. It shows nothing outside a world; the HUD editor and
 * the menu preview fill it from {@link SampleInfo} instead.
 */
public final class TemplateElement extends TextHudElement {

    private final String defaultTemplate;
    /** When the element is worth showing, such as ping only once it is known. */
    private final Predicate<ClientInfo> shown;
    private final boolean custom;
    /** Told about every template the player sets, to keep it. */
    private final Consumer<TemplateElement> onEdit;
    private volatile Template template;

    public TemplateElement(String id, String displayName, double defaultX, double defaultY, String defaultTemplate,
                           Predicate<ClientInfo> shown, boolean custom, Consumer<TemplateElement> onEdit) {
        super(id, displayName, defaultX, defaultY);
        this.defaultTemplate = defaultTemplate;
        this.shown = shown;
        this.custom = custom;
        this.onEdit = onEdit;
        this.template = Template.compile(defaultTemplate);
    }

    @Override
    protected List<String> lines() {
        ClientInfo info = ClientBridge.info();
        if (! info.inWorld() || (shown != null && ! shown.test(info))) return Collections.emptyList();
        return template.render(info);
    }

    @Override
    protected List<String> previewLines() {
        List<String> lines = template.render(SampleInfo.INSTANCE);
        return lines.isEmpty() ? Collections.singletonList(getDisplayName()) : lines;
    }

    @Override
    public String getTemplate() {
        return template.source();
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
        template = Template.compile(text == null || text.isEmpty() ? defaultTemplate : text);
    }

    /** @return whether the player changed the text from the default */
    boolean isEdited() {
        return ! template.source().equals(defaultTemplate);
    }

    @Override
    public Map<String, String> getPlaceholders() {
        return Placeholders.descriptions();
    }

    @Override
    public boolean isCustom() {
        return custom;
    }
}
