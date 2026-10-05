package singularity.permissions;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Permission nodes that every player holds unless a permission plugin says otherwise.
 *
 * <p>Each platform resolves permissions its own way, and an unknown node is operator-only on
 * Spigot and the mod loaders. A module declares its everyone-may-use nodes here; the platform
 * then grants them by default: Spigot registers them with Bukkit as default-true, and the
 * mod loaders grant them whenever LuckPerms has no explicit value. Any explicit setting in a
 * permission plugin still wins.</p>
 */
public final class DefaultPermissions {

    private static final Set<String> NODES = ConcurrentHashMap.newKeySet();
    private static final List<Consumer<String>> LISTENERS = new CopyOnWriteArrayList<>();

    private DefaultPermissions() {}

    /**
     * Grants the nodes to everyone by default.
     *
     * @param nodes permission nodes
     */
    public static void grantByDefault(String... nodes) {
        for (String node : nodes) {
            if (node == null || node.isEmpty()) continue;
            if (NODES.add(node)) LISTENERS.forEach(listener -> listener.accept(node));
        }
    }

    /**
     * @param node a permission node
     * @return whether everyone holds the node by default
     */
    public static boolean isGrantedByDefault(String node) {
        return NODES.contains(node);
    }

    /**
     * Calls the listener for every default node, now and as more are declared. Platforms use
     * this to mirror the defaults into their own permission system.
     *
     * @param listener called once per node
     */
    public static void onGrant(Consumer<String> listener) {
        LISTENERS.add(listener);
        NODES.forEach(listener);
    }
}
