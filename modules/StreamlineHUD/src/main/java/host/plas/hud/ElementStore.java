package host.plas.hud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * The element text the player set and the elements they added, kept in
 * {@code elements.properties} in the module's data folder:
 * {@code template.<id>} for a built-in element's edited text and {@code custom.<id>} for an
 * added element's text. Built-in text left at its default is not written, so later versions
 * can change the defaults.
 *
 * <p>Edits come from the render thread and loading from the module's thread, so access is
 * synchronized; writes are small and rare.</p>
 */
public final class ElementStore {

    private static final String TEMPLATE = "template.";
    private static final String CUSTOM = "custom.";

    private final File file;
    private final Consumer<String> warn;
    private final Map<String, String> templates = new TreeMap<>();
    private final Map<String, String> custom = new TreeMap<>();

    public ElementStore(File file, Consumer<String> warn) {
        this.file = file;
        this.warn = warn;
    }

    public synchronized void load() {
        templates.clear();
        custom.clear();
        if (! file.isFile()) return;
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            warn.accept("Could not read " + file + ": " + e.getMessage());
            return;
        }
        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            if (name.startsWith(TEMPLATE)) templates.put(name.substring(TEMPLATE.length()), value);
            else if (name.startsWith(CUSTOM)) custom.put(name.substring(CUSTOM.length()), value);
        }
    }

    /** @return the edited text of a built-in element, or {@code null} for its default */
    public synchronized String template(String id) {
        return templates.get(id);
    }

    /** @return the added elements, id to text, by id */
    public synchronized Map<String, String> custom() {
        return new TreeMap<>(custom);
    }

    /** Keeps an element's current text: under {@code custom.} if the player added it. */
    public synchronized void saved(TemplateElement element) {
        if (element.isCustom()) custom.put(element.getId(), element.getTemplate());
        else if (element.isEdited()) templates.put(element.getId(), element.getTemplate());
        else templates.remove(element.getId());
        save();
    }

    public synchronized void removeCustom(String id) {
        if (custom.remove(id) != null) save();
    }

    private void save() {
        Properties properties = new Properties();
        for (Map.Entry<String, String> entry : templates.entrySet()) properties.setProperty(TEMPLATE + entry.getKey(), entry.getValue());
        for (Map.Entry<String, String> entry : custom.entrySet()) properties.setProperty(CUSTOM + entry.getKey(), entry.getValue());
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        try (OutputStream out = new FileOutputStream(file)) {
            properties.store(out, "StreamlineHUD element text: template.<id> = edited text, custom.<id> = added element");
        } catch (IOException e) {
            warn.accept("Could not write " + file + ": " + e.getMessage());
        }
    }
}
