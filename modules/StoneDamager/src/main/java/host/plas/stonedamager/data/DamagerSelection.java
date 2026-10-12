package host.plas.stonedamager.data;

import lombok.Builder;
import lombok.Getter;
import singularity.interfaces.LivingEntityView;

import java.util.Locale;

/**
 * One entry under {@code damagers:} in config.yml: which entities, in which worlds, take how
 * much damage how often while the block at the offset from them is one of the materials.
 *
 * <p>Immutable, as it is read from the server thread (on Folia, from several region threads)
 * while a reload may be building its replacement.</p>
 */
@Getter
@Builder
public final class DamagerSelection {
    private final String identifier;
    private final boolean enabled;

    private final long ticksPerDamage;
    private final double damageAmount;

    private final double xOffset;
    private final double yOffset;
    private final double zOffset;

    private final String includePermission;
    private final String excludePermission;

    private final IdList materials;
    private final IdList worlds;
    private final IdList entities;

    /**
     * Cheapest checks first, as this runs for every living entity on the server each time the
     * selection fires.
     */
    public boolean matches(LivingEntityView entity) {
        if (! enabled) return false;
        if (! entities.check(entity.getType())) return false;
        if (! worlds.check(entity.getWorldName())) return false;
        if (! materials.check(entity.getBlockAt(xOffset, yOffset, zOffset))) return false;
        return checkPermissions(entity);
    }

    /** Non-players always pass; players need the include node (if set) and must lack the exclude node (if set). */
    private boolean checkPermissions(LivingEntityView entity) {
        if (! entity.isPlayer()) return true;
        if (! isEmptyPermission(includePermission) && ! entity.hasPermission(includePermission)) return false;
        return isEmptyPermission(excludePermission) || ! entity.hasPermission(excludePermission);
    }

    public static boolean isEmptyPermission(String permission) {
        if (permission == null || permission.isBlank()) return true;
        switch (permission.trim().toLowerCase(Locale.ROOT)) {
            case "none":
            case "null":
            case "disable":
            case "disabled":
            case "off":
                return true;
            default:
                return false;
        }
    }
}
