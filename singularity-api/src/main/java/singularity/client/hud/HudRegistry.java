package singularity.client.hud;

import singularity.Singularity;
import singularity.utils.MessageUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Every {@link HudElement} the running modules registered, and where the player placed
 * each one.
 *
 * <p>Modules register from their own threads while the render thread reads every frame, so
 * both maps are concurrent. Placements are kept in {@code client/hud-layout.properties} in
 * Streamline's data folder, keyed {@code <module-id>:<element-id>}, and outlive the elements:
 * a module that stops and starts again finds its elements where the player left them.</p>
 */
public final class HudRegistry {

    /** Registered elements by key, sorted so the editor lists them in a stable order. */
    private static final Map<String, Registered> ELEMENTS = new ConcurrentSkipListMap<>();
    private static final Map<String, HudPlacement> PLACEMENTS = new ConcurrentHashMap<>();
    private static volatile boolean loaded;

    private HudRegistry() {}

    /** An element with the module that owns it. */
    public static final class Registered {
        private final String owner;
        private final HudElement element;

        private Registered(String owner, HudElement element) {
            this.owner = owner;
            this.element = element;
        }

        public String getOwner() {
            return owner;
        }

        public HudElement getElement() {
            return element;
        }

        public String getKey() {
            return key(owner, element.getId());
        }

        public HudPlacement getPlacement() {
            return placement(owner, element);
        }
    }

    public static String key(String owner, String elementId) {
        return owner + ":" + elementId;
    }

    /**
     * Adds an element owned by a module, replacing one with the same id. It is removed again
     * when the module stops.
     *
     * @param owner the owning module's identifier
     */
    public static void register(String owner, HudElement element) {
        ensureLoaded();
        ELEMENTS.put(key(owner, element.getId()), new Registered(owner, element));
    }

    public static void unregister(String owner, String elementId) {
        ELEMENTS.remove(key(owner, elementId));
    }

    public static void unregisterAll(String owner) {
        ELEMENTS.values().removeIf(registered -> registered.owner.equals(owner));
    }

    /** @return a snapshot of every registered element */
    public static List<Registered> elements() {
        return new ArrayList<>(ELEMENTS.values());
    }

    /** @return the elements one module registered */
    public static List<Registered> elementsOf(String owner) {
        List<Registered> result = new ArrayList<>();
        for (Registered registered : ELEMENTS.values()) {
            if (registered.owner.equals(owner)) result.add(registered);
        }
        return result;
    }

    /** @return where the element sits, created at its default place on first use */
    public static HudPlacement placement(String owner, HudElement element) {
        ensureLoaded();
        return PLACEMENTS.computeIfAbsent(key(owner, element.getId()),
                key -> new HudPlacement(element.getDefaultX(), element.getDefaultY(), 1.0, true));
    }

    /** Puts the element back at its default place and scale, shown. */
    public static void reset(String owner, HudElement element) {
        PLACEMENTS.put(key(owner, element.getId()),
                new HudPlacement(element.getDefaultX(), element.getDefaultY(), 1.0, true));
    }

    private static File file() {
        Singularity<?, ?, ?, ?, ?> instance = Singularity.getInstance();
        if (instance == null) return null;
        return new File(new File(instance.getDataFolder(), "client"), "hud-layout.properties");
    }

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        File file = file();
        if (file == null) return;
        loaded = true;
        if (! file.isFile()) return;

        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            MessageUtils.logWarning("Could not read " + file + ": " + e.getMessage());
            return;
        }
        for (String name : properties.stringPropertyNames()) {
            // x,y,scale,enabled
            String[] parts = properties.getProperty(name).split(",");
            if (parts.length < 4) continue;
            try {
                PLACEMENTS.put(name, new HudPlacement(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()),
                        Double.parseDouble(parts[2].trim()), Boolean.parseBoolean(parts[3].trim())));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    /** Writes every placement, including those of elements not registered right now. */
    public static synchronized void save() {
        File file = file();
        if (file == null) return;
        Properties properties = new Properties();
        for (Map.Entry<String, HudPlacement> entry : PLACEMENTS.entrySet()) {
            HudPlacement p = entry.getValue();
            properties.setProperty(entry.getKey(), p.getX() + "," + p.getY() + "," + p.getScale() + "," + p.isEnabled());
        }
        file.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(file)) {
            properties.store(out, "Streamline HUD layout: <module-id>:<element-id> = x,y,scale,enabled");
        } catch (IOException e) {
            MessageUtils.logWarning("Could not write " + file + ": " + e.getMessage());
        }
    }
}
