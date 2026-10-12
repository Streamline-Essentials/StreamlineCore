package host.plas.stonedamager.events;

import host.plas.stonedamager.data.DamagerSelection;
import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;

/**
 * A damager selection is about to hurt a living entity. Cancel it to spare the entity this
 * time, or change {@link #getDamage()}.
 *
 * <p>Fired on the thread that owns the entity: the server thread, or its region's thread on
 * Folia. Only fired when something listens for it.</p>
 */
@Getter
public class StoneDamageEvent extends CosmicEvent {
    private final DamagerSelection selection;
    private final String entityUuid;
    /** The entity type id, such as {@code minecraft:zombie}. */
    private final String entityType;
    private final boolean player;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;

    /** The damage in half-hearts; zero or less spares the entity. */
    @Setter
    private double damage;

    public StoneDamageEvent(DamagerSelection selection, String entityUuid, String entityType, boolean player,
                            String worldName, double x, double y, double z, double damage) {
        this.selection = selection;
        this.entityUuid = entityUuid;
        this.entityType = entityType;
        this.player = player;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.damage = damage;
    }
}
