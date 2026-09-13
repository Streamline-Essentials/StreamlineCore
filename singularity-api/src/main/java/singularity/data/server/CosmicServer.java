package singularity.data.server;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a cross-platform server reference identified by a string name.
 *
 * <p>Two {@code CosmicServer} instances are considered equal when their identifiers
 * match, making this class safe to use as a map key or set member.</p>
 */
@Getter @Setter
public class CosmicServer implements Identifiable {

    /**
     * The name used to identify this server across the network.
     */
    private String identifier;

    /**
     * Constructs a {@code CosmicServer} with the given identifier.
     *
     * @param identifier the server name
     */
    public CosmicServer(String identifier) {
        this.identifier = identifier;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the server's identifier string.</p>
     */
    @Override
    public String toString() {
        return getIdentifier();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Two {@code CosmicServer} objects are equal when their identifiers match.
     * Falls back to the default {@link Object#equals(Object)} behaviour for
     * non-{@code CosmicServer} objects.</p>
     *
     * @param obj the object to compare with
     * @return {@code true} if {@code obj} is a {@code CosmicServer} with the same identifier
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CosmicServer) {
            CosmicServer other = (CosmicServer) obj;
            return this.getIdentifier().equals(other.getIdentifier());
        } else {
            return super.equals(obj);
        }
    }
}
