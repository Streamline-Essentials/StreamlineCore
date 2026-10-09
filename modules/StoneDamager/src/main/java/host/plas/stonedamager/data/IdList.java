package host.plas.stonedamager.data;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * A config list of ids that is either a whitelist (only these match) or a blacklist (all but
 * these match). An empty whitelist matches nothing; an empty blacklist matches everything.
 *
 * <p>Matching ignores case and treats a missing namespace as {@code minecraft:}, so both
 * {@code minecraft:stonecutter} and the Bukkit-style {@code STONECUTTER} name the same block.</p>
 */
public final class IdList {
    private final Set<String> ids;
    private final boolean blacklist;

    public IdList(Collection<String> ids, boolean blacklist) {
        this.ids = new TreeSet<>();
        for (String id : ids) {
            if (id != null && ! id.isBlank()) this.ids.add(normalize(id));
        }
        this.blacklist = blacklist;
    }

    /**
     * @param id the id to look up, or {@code null} when it is unknown (such as an unloaded block)
     * @return whether the id passes the list; an unknown id passes only a blacklist
     */
    public boolean check(String id) {
        boolean listed = id != null && ids.contains(normalize(id));
        return listed != blacklist;
    }

    public boolean isEmpty() {
        return ids.isEmpty();
    }

    public static String normalize(String id) {
        String lower = id.trim().toLowerCase(Locale.ROOT);
        return lower.indexOf(':') < 0 ? "minecraft:" + lower : lower;
    }
}
