package host.plas.tags.data;

import host.plas.tags.StreamlineTags;
import singularity.Singularity;
import singularity.data.console.CosmicSender;
import singularity.database.CoreDBOperator;
import singularity.modules.ModuleUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Chat tags. A player can use a tag when they hold {@code streamline.tags.tag.<id>}, were granted
 * it by staff, or hold {@code streamline.tags.group.<group>} for a group that contains it.
 *
 * <p>Tag definitions and groups are cached for everyone and re-read from the database every
 * {@code refresh-interval-seconds} (and on {@code /tag reload}), so tags made on another server
 * of a network show up here too. Grants and the active selection are cached per online player,
 * which keeps the chat placeholders off the database.</p>
 *
 * <p>Everything lives in Streamline's main database. Statements use {@code REPLACE INTO}, which
 * MySQL and SQLite both accept.</p>
 */
public final class TagStore {
    private TagStore() {
    }

    public static final String TAG_PERMISSION = "streamline.tags.tag.";
    public static final String GROUP_PERMISSION = "streamline.tags.group.";

    private static final Map<String, String> displays = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> groups = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> grants = new ConcurrentHashMap<>();
    private static final Map<String, String> active = new ConcurrentHashMap<>();
    private static volatile ExecutorService writer;

    public static void ensureTables() {
        db().execute(
                "CREATE TABLE IF NOT EXISTS `" + table("tags") + "` (`tag_id` VARCHAR(64) NOT NULL PRIMARY KEY, `display` TEXT NOT NULL);;"
                        + "CREATE TABLE IF NOT EXISTS `" + table("tag_groups") + "` (`group_id` VARCHAR(64) NOT NULL PRIMARY KEY);;"
                        + "CREATE TABLE IF NOT EXISTS `" + table("tag_group_members") + "` (`group_id` VARCHAR(64) NOT NULL, "
                        + "`tag_id` VARCHAR(64) NOT NULL, PRIMARY KEY (`group_id`, `tag_id`));;"
                        + "CREATE TABLE IF NOT EXISTS `" + table("tag_grants") + "` (`uuid` VARCHAR(36) NOT NULL, "
                        + "`tag_id` VARCHAR(64) NOT NULL, PRIMARY KEY (`uuid`, `tag_id`));;"
                        + "CREATE TABLE IF NOT EXISTS `" + table("tag_active") + "` (`uuid` VARCHAR(36) NOT NULL PRIMARY KEY, "
                        + "`tag_id` VARCHAR(64) NOT NULL);;",
                s -> {});
    }

    // ---- definitions ---------------------------------------------------------------------------

    /** Re-reads every tag and group. Blocking; call it off the server thread. */
    public static void loadDefinitions() {
        Map<String, String> readDisplays = new ConcurrentHashMap<>();
        Map<String, Set<String>> readGroups = new ConcurrentHashMap<>();
        query("SELECT `tag_id`, `display` FROM `" + table("tags") + "`;", rs -> readDisplays.put(rs.getString(1), rs.getString(2)));
        query("SELECT `group_id` FROM `" + table("tag_groups") + "`;", rs -> readGroups.put(rs.getString(1), ConcurrentHashMap.newKeySet()));
        query("SELECT `group_id`, `tag_id` FROM `" + table("tag_group_members") + "`;", rs ->
                readGroups.computeIfAbsent(rs.getString(1), k -> ConcurrentHashMap.newKeySet()).add(rs.getString(2)));

        displays.keySet().retainAll(readDisplays.keySet());
        displays.putAll(readDisplays);
        groups.keySet().retainAll(readGroups.keySet());
        groups.putAll(readGroups);
    }

    public static boolean exists(String tagId) {
        return tagId != null && displays.containsKey(tagId);
    }

    public static String display(String tagId) {
        return tagId == null ? null : displays.get(tagId);
    }

    public static List<String> ids() {
        List<String> ids = new ArrayList<>(displays.keySet());
        Collections.sort(ids);
        return ids;
    }

    public static boolean groupExists(String groupId) {
        return groupId != null && groups.containsKey(groupId);
    }

    public static List<String> groupIds() {
        List<String> ids = new ArrayList<>(groups.keySet());
        Collections.sort(ids);
        return ids;
    }

    public static List<String> tagsInGroup(String groupId) {
        List<String> ids = new ArrayList<>(groups.getOrDefault(groupId, Collections.emptySet()));
        Collections.sort(ids);
        return ids;
    }

    /** Creates a tag, or changes how an existing one shows. */
    public static void create(String tagId, String display) {
        displays.put(tagId, display);
        async(() -> update("REPLACE INTO `" + table("tags") + "` (`tag_id`, `display`) VALUES (?, ?);", tagId, display));
    }

    public static void remove(String tagId) {
        displays.remove(tagId);
        groups.values().forEach(set -> set.remove(tagId));
        grants.values().forEach(set -> set.remove(tagId));
        active.values().removeIf(tagId::equals);
        async(() -> {
            update("DELETE FROM `" + table("tag_group_members") + "` WHERE `tag_id` = ?;", tagId);
            update("DELETE FROM `" + table("tag_grants") + "` WHERE `tag_id` = ?;", tagId);
            update("DELETE FROM `" + table("tag_active") + "` WHERE `tag_id` = ?;", tagId);
            update("DELETE FROM `" + table("tags") + "` WHERE `tag_id` = ?;", tagId);
        });
    }

    public static void createGroup(String groupId) {
        groups.putIfAbsent(groupId, ConcurrentHashMap.newKeySet());
        async(() -> update("REPLACE INTO `" + table("tag_groups") + "` (`group_id`) VALUES (?);", groupId));
    }

    /** Deletes a group; the tags in it stay. */
    public static void removeGroup(String groupId) {
        groups.remove(groupId);
        async(() -> {
            update("DELETE FROM `" + table("tag_group_members") + "` WHERE `group_id` = ?;", groupId);
            update("DELETE FROM `" + table("tag_groups") + "` WHERE `group_id` = ?;", groupId);
        });
    }

    public static boolean addToGroup(String groupId, String tagId) {
        boolean added = groups.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet()).add(tagId);
        if (added) {
            async(() -> update("REPLACE INTO `" + table("tag_group_members") + "` (`group_id`, `tag_id`) VALUES (?, ?);", groupId, tagId));
        }
        return added;
    }

    public static boolean removeFromGroup(String groupId, String tagId) {
        Set<String> set = groups.get(groupId);
        boolean removed = set != null && set.remove(tagId);
        if (removed) {
            async(() -> update("DELETE FROM `" + table("tag_group_members") + "` WHERE `group_id` = ? AND `tag_id` = ?;", groupId, tagId));
        }
        return removed;
    }

    // ---- players -------------------------------------------------------------------------------

    /** Reads a player's grants and active tag into the cache. Blocking; call it off the server thread. */
    public static void loadPlayer(String uuid) {
        Set<String> granted = ConcurrentHashMap.newKeySet();
        query("SELECT `tag_id` FROM `" + table("tag_grants") + "` WHERE `uuid` = ?;", rs -> granted.add(rs.getString(1)), uuid);
        grants.put(uuid, granted);

        AtomicReference<String> selected = new AtomicReference<>();
        query("SELECT `tag_id` FROM `" + table("tag_active") + "` WHERE `uuid` = ?;", rs -> selected.set(rs.getString(1)), uuid);
        if (selected.get() == null) active.remove(uuid);
        else active.put(uuid, selected.get());
    }

    public static void unloadPlayer(String uuid) {
        grants.remove(uuid);
        active.remove(uuid);
    }

    public static boolean isLoaded(String uuid) {
        return grants.containsKey(uuid);
    }

    /** Grants a tag, completing with {@code false} when the player already had it granted. */
    public static CompletableFuture<Boolean> grant(String uuid, String tagId) {
        Set<String> cached = grants.get(uuid);
        if (cached != null && ! cached.add(tagId)) return CompletableFuture.completedFuture(false);

        ExecutorService current = writer;
        Executor executor = current == null ? Runnable::run : current;
        return CompletableFuture.supplyAsync(() -> {
            if (cached == null) {
                AtomicBoolean had = new AtomicBoolean(false);
                query("SELECT 1 FROM `" + table("tag_grants") + "` WHERE `uuid` = ? AND `tag_id` = ?;", rs -> had.set(true), uuid, tagId);
                if (had.get()) return false;
            }
            update("REPLACE INTO `" + table("tag_grants") + "` (`uuid`, `tag_id`) VALUES (?, ?);", uuid, tagId);
            return true;
        }, executor);
    }

    public static void revoke(String uuid, String tagId) {
        Set<String> cached = grants.get(uuid);
        if (cached != null) cached.remove(tagId);
        async(() -> update("DELETE FROM `" + table("tag_grants") + "` WHERE `uuid` = ? AND `tag_id` = ?;", uuid, tagId));
    }

    /** Whether a loaded player may use the tag: by permission, a grant, or a group's permission. */
    public static boolean canUse(CosmicSender player, String tagId) {
        if (player == null || ! exists(tagId)) return false;
        if (ModuleUtils.hasPermission(player, TAG_PERMISSION + tagId)) return true;
        Set<String> granted = grants.get(player.getUuid());
        if (granted != null && granted.contains(tagId)) return true;
        for (Map.Entry<String, Set<String>> group : groups.entrySet()) {
            if (group.getValue().contains(tagId) && ModuleUtils.hasPermission(player, GROUP_PERMISSION + group.getKey())) return true;
        }
        return false;
    }

    /** Every tag the player may use, sorted by id. */
    public static List<String> usable(CosmicSender player) {
        List<String> out = new ArrayList<>();
        for (String id : ids()) {
            if (canUse(player, id)) out.add(id);
        }
        return out;
    }

    public static Optional<String> activeId(String uuid) {
        return Optional.ofNullable(active.get(uuid));
    }

    /** The display of the player's active tag, or an empty string when none applies. */
    public static String activeDisplay(CosmicSender player) {
        if (player == null || player.isConsole()) return "";
        String id = active.get(player.getUuid());
        if (id == null || ! canUse(player, id)) return "";
        String display = displays.get(id);
        return display == null ? "" : display;
    }

    public static boolean select(CosmicSender player, String tagId) {
        if (! canUse(player, tagId)) return false;
        String uuid = player.getUuid();
        active.put(uuid, tagId);
        async(() -> update("REPLACE INTO `" + table("tag_active") + "` (`uuid`, `tag_id`) VALUES (?, ?);", uuid, tagId));
        return true;
    }

    public static void clear(String uuid) {
        active.remove(uuid);
        async(() -> update("DELETE FROM `" + table("tag_active") + "` WHERE `uuid` = ?;", uuid));
    }

    /** Removes a player's grants and selection. */
    public static void wipe(String uuid) {
        Set<String> cached = grants.get(uuid);
        if (cached != null) cached.clear();
        active.remove(uuid);
        async(() -> {
            update("DELETE FROM `" + table("tag_grants") + "` WHERE `uuid` = ?;", uuid);
            update("DELETE FROM `" + table("tag_active") + "` WHERE `uuid` = ?;", uuid);
        });
    }

    // ---- SQL -----------------------------------------------------------------------------------

    private interface Row {
        void read(ResultSet rs) throws SQLException;
    }

    private static CoreDBOperator db() {
        return Singularity.getMainDatabase();
    }

    private static String table(String name) {
        return db().getConnectorSet().getTablePrefix() + name;
    }

    private static Consumer<PreparedStatement> bind(Object... params) {
        return stmt -> {
            try {
                for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        };
    }

    private static void update(String sql, Object... params) {
        db().executeSingle(sql, bind(params));
    }

    private static void query(String sql, Row row, Object... params) {
        db().executeQuery(sql, bind(params), rs -> {
            try {
                while (rs.next()) row.read(rs);
            } catch (SQLException e) {
                StreamlineTags.getInstance().logWarning("Tags query failed: " + e.getMessage());
            }
        });
    }

    /** Starts the writer thread; writes run on it one at a time, in the order they were made. */
    public static void start() {
        stop();
        writer = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "StreamlineTags-Writer");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Finishes queued writes, waiting up to ten seconds, and stops the writer thread. */
    public static void stop() {
        ExecutorService current = writer;
        writer = null;
        if (current == null) return;
        current.shutdown();
        try {
            current.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void async(Runnable write) {
        ExecutorService current = writer;
        if (current == null) {
            write.run();
            return;
        }
        CompletableFuture.runAsync(write, current).exceptionally(error -> {
            StreamlineTags.getInstance().logWarning("Could not save tags: " + error.getMessage());
            return null;
        });
    }
}
