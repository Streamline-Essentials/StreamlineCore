package singularity.client.hud;

import singularity.client.ClientEnvironment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 *
 * <p>{@link HudGroup}s are kept in the same file under {@link #GROUP_PREFIX}{@code <id>}, a
 * prefix no {@code <module-id>:<element-id>} key starts with. A placement's line is
 * {@code x,y,scale,enabled,removed,background,backgroundColor,border,borderColor,group}, and a
 * group's {@code background,backgroundColor,border,borderColor,name}; the name comes last so it
 * may hold commas. Fields after {@code removed} are optional, so older files still load.</p>
 */
public final class HudRegistry {

    /** Registered elements by key, sorted so the editor lists them in a stable order. */
    private static final Map<String, Registered> ELEMENTS = new ConcurrentSkipListMap<>();
    private static final Map<String, HudPlacement> PLACEMENTS = new ConcurrentHashMap<>();
    /** Modules that let the player add elements, by module id. */
    private static final Map<String, CustomHudElements> CUSTOM = new ConcurrentHashMap<>();
    /** Groups by id, sorted so menus list them in a stable order. */
    private static final Map<String, HudGroup> GROUPS = new ConcurrentSkipListMap<>();

    /** Starts the key of a group's line in {@code hud-layout.properties}. */
    public static final String GROUP_PREFIX = "@group:";
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
        CUSTOM.remove(owner);
    }

    /**
     * Lets the player add elements to a module's HUD from the module menu; {@code null} stops
     * offering it. It is dropped with the module's elements when the module stops.
     */
    public static void setCustomElements(String owner, CustomHudElements custom) {
        if (custom == null) CUSTOM.remove(owner);
        else CUSTOM.put(owner, custom);
    }

    /** @return how the player adds elements to a module's HUD, or {@code null} if they cannot */
    public static CustomHudElements customElements(String owner) {
        return CUSTOM.get(owner);
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

    /** Puts the element back at its default place and scale, shown, keeping its frame and group. */
    public static void reset(String owner, HudElement element) {
        HudPlacement old = PLACEMENTS.get(key(owner, element.getId()));
        PLACEMENTS.put(key(owner, element.getId()), new HudPlacement(element.getDefaultX(), element.getDefaultY(), 1.0, true,
                false, old == null ? new HudBoxStyle() : old.getStyle().copy(), old == null ? null : old.getGroup()));
    }

    /**
     * Forgets where an element was placed, so one added later with its id starts at its default
     * place, outside any group.
     */
    public static void forgetPlacement(String owner, String elementId) {
        HudPlacement old = PLACEMENTS.remove(key(owner, elementId));
        if (old != null && old.getGroup() != null) dissolveIfLonely(old.getGroup());
    }

    // ---------------------------------------------------------------- groups

    /** @return every group, sorted by id */
    public static List<HudGroup> groups() {
        ensureLoaded();
        return new ArrayList<>(GROUPS.values());
    }

    /** @return the group with this id, or {@code null} */
    public static HudGroup group(String id) {
        if (id == null) return null;
        ensureLoaded();
        return GROUPS.get(id);
    }

    /** @return the group the registered element belongs to, or {@code null} */
    public static HudGroup groupOf(Registered registered) {
        return group(registered.getPlacement().getGroup());
    }

    /**
     * @return the registered elements in a group, removed ones included; members whose module is
     * not running are not listed but stay in the group
     */
    public static List<Registered> members(String groupId) {
        List<Registered> result = new ArrayList<>();
        if (groupId == null) return result;
        for (Registered registered : ELEMENTS.values()) {
            if (groupId.equals(registered.getPlacement().getGroup())) result.add(registered);
        }
        return result;
    }

    /** @return the keys of every placement in a group, registered or not */
    public static List<String> memberKeys(String groupId) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, HudPlacement> entry : PLACEMENTS.entrySet()) {
            if (groupId.equals(entry.getValue().getGroup())) result.add(entry.getKey());
        }
        return result;
    }

    /**
     * Ties elements together into a new group named "Group N". Elements already in a group
     * leave it first, and a group left with fewer than two members is dissolved.
     *
     * @param keys element keys ({@link #key(String, String)}); unknown keys are skipped
     * @return the new group, or {@code null} if fewer than two of the keys have a placement
     */
    public static HudGroup createGroup(Collection<String> keys) {
        ensureLoaded();
        List<HudPlacement> placements = new ArrayList<>();
        for (String key : keys) {
            HudPlacement placement = PLACEMENTS.get(key);
            if (placement != null && ! placements.contains(placement)) placements.add(placement);
        }
        if (placements.size() < 2) return null;
        int n = 1;
        while (GROUPS.containsKey(String.valueOf(n))) n++;
        HudGroup group = new HudGroup(String.valueOf(n), "Group " + n, new HudBoxStyle());
        GROUPS.put(group.getId(), group);
        List<String> left = new ArrayList<>();
        for (HudPlacement placement : placements) {
            if (placement.getGroup() != null) left.add(placement.getGroup());
            placement.setGroup(group.getId());
        }
        for (String old : left) dissolveIfLonely(old);
        return group;
    }

    /** Takes one element out of its group; a group left with fewer than two members is dissolved. */
    public static void unlink(String key) {
        HudPlacement placement = PLACEMENTS.get(key);
        if (placement == null || placement.getGroup() == null) return;
        String group = placement.getGroup();
        placement.setGroup(null);
        dissolveIfLonely(group);
    }

    /** Dissolves a group, leaving each member where it is with its own frame. */
    public static void ungroup(String groupId) {
        if (groupId == null) return;
        for (HudPlacement placement : PLACEMENTS.values()) {
            if (groupId.equals(placement.getGroup())) placement.setGroup(null);
        }
        GROUPS.remove(groupId);
    }

    private static void dissolveIfLonely(String groupId) {
        if (memberKeys(groupId).size() < 2) ungroup(groupId);
    }

    // ---------------------------------------------------------------- snapshots

    /** The whole layout at one moment: every placement and group, copied. */
    public static final class Snapshot {
        private final Map<String, HudPlacement> placements;
        private final Map<String, HudGroup> groups;

        private Snapshot(Map<String, HudPlacement> placements, Map<String, HudGroup> groups) {
            this.placements = placements;
            this.groups = groups;
        }

        @Override
        public boolean equals(Object o) {
            if (! (o instanceof Snapshot)) return false;
            Snapshot other = (Snapshot) o;
            return placements.equals(other.placements) && groups.equals(other.groups);
        }

        @Override
        public int hashCode() {
            return Objects.hash(placements.size(), groups.size());
        }
    }

    /** @return a copy of the current layout, for {@link #restore(Snapshot)} */
    public static Snapshot snapshot() {
        ensureLoaded();
        Map<String, HudPlacement> placements = new HashMap<>();
        for (Map.Entry<String, HudPlacement> entry : PLACEMENTS.entrySet()) placements.put(entry.getKey(), entry.getValue().copy());
        Map<String, HudGroup> groups = new HashMap<>();
        for (Map.Entry<String, HudGroup> entry : GROUPS.entrySet()) groups.put(entry.getKey(), entry.getValue().copy());
        return new Snapshot(placements, groups);
    }

    /**
     * Puts the layout back as it was in a snapshot and saves it. Placements made since are
     * dropped, so those elements go back to their default place.
     */
    public static void restore(Snapshot snapshot) {
        PLACEMENTS.clear();
        for (Map.Entry<String, HudPlacement> entry : snapshot.placements.entrySet()) PLACEMENTS.put(entry.getKey(), entry.getValue().copy());
        GROUPS.clear();
        for (Map.Entry<String, HudGroup> entry : snapshot.groups.entrySet()) GROUPS.put(entry.getKey(), entry.getValue().copy());
        save();
    }

    private static File file() {
        return ClientEnvironment.file("hud-layout.properties");
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
            ClientEnvironment.logWarning("Could not read " + file + ": " + e.getMessage());
            return;
        }
        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            if (name.startsWith(GROUP_PREFIX)) {
                // background,backgroundColor,border,borderColor,name
                String id = name.substring(GROUP_PREFIX.length());
                String[] parts = value.split(",", 5);
                if (id.isEmpty()) continue;
                String groupName = parts.length > 4 && ! parts[4].trim().isEmpty() ? parts[4].trim() : "Group " + id;
                GROUPS.put(id, new HudGroup(id, groupName, HudBoxStyle.parse(parts, 0)));
                continue;
            }
            // x,y,scale,enabled[,removed[,background,backgroundColor,border,borderColor[,group]]]
            String[] parts = value.split(",");
            if (parts.length < 4) continue;
            try {
                PLACEMENTS.put(name, new HudPlacement(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()),
                        Double.parseDouble(parts[2].trim()), Boolean.parseBoolean(parts[3].trim()),
                        parts.length > 4 && Boolean.parseBoolean(parts[4].trim()),
                        HudBoxStyle.parse(parts, 5), parts.length > 9 ? parts[9].trim() : null));
            } catch (NumberFormatException ignored) {
            }
        }
        // A member of a group that is gone is out of any group, and so is a group's lone member.
        for (HudPlacement placement : PLACEMENTS.values()) {
            if (placement.getGroup() != null && ! GROUPS.containsKey(placement.getGroup())) placement.setGroup(null);
        }
        for (String id : new ArrayList<>(GROUPS.keySet())) dissolveIfLonely(id);
    }

    /** Writes every placement, including those of elements not registered right now. */
    public static synchronized void save() {
        File file = file();
        if (file == null) return;
        Properties properties = new Properties();
        for (Map.Entry<String, HudPlacement> entry : PLACEMENTS.entrySet()) {
            HudPlacement p = entry.getValue();
            properties.setProperty(entry.getKey(), p.getX() + "," + p.getY() + "," + p.getScale() + "," + p.isEnabled() + "," + p.isRemoved()
                    + "," + p.getStyle().serialize() + "," + (p.getGroup() == null ? "" : p.getGroup()));
        }
        for (HudGroup group : GROUPS.values()) {
            properties.setProperty(GROUP_PREFIX + group.getId(), group.getStyle().serialize() + "," + group.getName());
        }
        file.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(file)) {
            properties.store(out, "Streamline HUD layout: <module-id>:<element-id> = x,y,scale,enabled,removed,background,backgroundColor,border,borderColor,group"
                    + " / @group:<id> = background,backgroundColor,border,borderColor,name");
        } catch (IOException e) {
            ClientEnvironment.logWarning("Could not write " + file + ": " + e.getMessage());
        }
    }
}
