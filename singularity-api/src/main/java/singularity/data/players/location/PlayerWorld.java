package singularity.data.players.location;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;
import singularity.data.server.CosmicServer;

/**
 * Represents a Minecraft world (dimension) as a platform-agnostic value object
 * identified by its name.
 *
 * <p>Two {@code PlayerWorld} instances are considered equal when their identifiers match,
 * regardless of the platform on which they were created.</p>
 */
@Getter @Setter
public class PlayerWorld implements Identifiable {

    /**
     * The name used to identify this world across platforms.
     */
    private String identifier;

    /**
     * Constructs a {@code PlayerWorld} with the given world name.
     *
     * @param worldName the name of the world
     */
    public PlayerWorld(String worldName) {
        this.identifier = worldName;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the world's identifier string.</p>
     */
    @Override
    public String toString() {
        return getIdentifier();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Two {@code PlayerWorld} objects are equal when their identifiers match.
     * Falls back to the default {@link Object#equals(Object)} behaviour for non-{@code PlayerWorld} objects.</p>
     *
     * @param obj the object to compare with
     * @return {@code true} if {@code obj} is a {@code PlayerWorld} with the same identifier
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PlayerWorld) {
            PlayerWorld other = (PlayerWorld) obj;
            return this.getIdentifier().equals(other.getIdentifier());
        } else {
            return super.equals(obj);
        }
    }
}
