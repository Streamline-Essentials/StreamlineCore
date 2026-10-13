package singularity.client.hud;

import lombok.Getter;
import lombok.Setter;

/**
 * HUD elements the player tied together in the HUD editor. A group moves as one and has a
 * frame of its own, drawn around the area its members cover.
 *
 * <p>Members are the placements whose {@link HudPlacement#getGroup()} is this group's id, so an
 * element whose module is not running stays in its group and comes back to it. Members keep
 * their own positions; the group itself stores only its name and frame.</p>
 */
@Getter
public final class HudGroup {

    private final String id;
    @Setter
    private volatile String name;
    private final HudBoxStyle style;

    public HudGroup(String id, String name, HudBoxStyle style) {
        this.id = id;
        this.name = name;
        this.style = style;
    }

    public HudGroup copy() {
        return new HudGroup(id, name, style.copy());
    }

    @Override
    public boolean equals(Object o) {
        if (! (o instanceof HudGroup)) return false;
        HudGroup other = (HudGroup) o;
        return id.equals(other.id) && name.equals(other.name) && style.equals(other.style);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
