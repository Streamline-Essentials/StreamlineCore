package singularity.client.input;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Game keys client modules hold down for the player, such as toggle sprint and toggle sneak.
 *
 * <p>A held key stays down, as if the player kept pressing it, until every module holding it
 * lets go: the client presses it again each tick, so it survives screens opening, deaths,
 * respawns and world changes. A module's holds end when it stops.</p>
 *
 * <p>Off a game client nothing reads these, so holding a key there does nothing.</p>
 */
public final class ClientInput {

    /** The game keys a module can hold. */
    public enum GameKey {
        SPRINT,
        SNEAK,
    }

    /** Module identifier to the keys it holds. */
    private static final Map<String, Set<GameKey>> HELD = new ConcurrentHashMap<>();

    private ClientInput() {}

    /** Holds {@code key} down for {@code moduleId}, or lets go of it. */
    public static void hold(String moduleId, GameKey key, boolean held) {
        if (held) {
            HELD.compute(moduleId, (id, keys) -> {
                Set<GameKey> next = keys == null ? EnumSet.noneOf(GameKey.class) : EnumSet.copyOf(keys);
                next.add(key);
                return next;
            });
        } else {
            HELD.computeIfPresent(moduleId, (id, keys) -> {
                Set<GameKey> next = EnumSet.copyOf(keys);
                next.remove(key);
                return next.isEmpty() ? null : next;
            });
        }
    }

    /** @return whether any module holds {@code key} */
    public static boolean isHeld(GameKey key) {
        for (Set<GameKey> keys : HELD.values()) {
            if (keys.contains(key)) return true;
        }
        return false;
    }

    /** @return whether {@code moduleId} holds {@code key} */
    public static boolean isHeld(String moduleId, GameKey key) {
        Set<GameKey> keys = HELD.get(moduleId);
        return keys != null && keys.contains(key);
    }

    /** Lets go of every key {@code moduleId} holds; the client releases them on its next tick. */
    public static void forgetModule(String moduleId) {
        HELD.remove(moduleId);
    }
}
