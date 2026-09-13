package singularity.database.servers;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.server.CosmicServer;
import singularity.interfaces.ISingularityExtension;

/**
 * A lightweight representation of a server node that has been registered in the
 * shared StreamlineCore database.
 *
 * <p>Each {@code SavedServer} stores the server's UUID (used as the primary key),
 * a human-readable name, and a {@link ISingularityExtension.ServerType} that
 * distinguishes proxy nodes from backend servers.</p>
 *
 * <p>Instances are written to and read from the database by
 * {@link singularity.database.CoreDBOperator#putServer(SavedServer)} and
 * {@link singularity.database.CoreDBOperator#pullServer(String)}.</p>
 */
@Getter @Setter
public class SavedServer implements Identifiable {

    /**
     * The unique identifier (UUID) for this server, used as the primary key in the
     * servers table.
     */
    private String identifier;

    /**
     * Returns the server UUID. Alias for {@link #getIdentifier()} provided for
     * semantic clarity in server-related contexts.
     *
     * @return the server UUID string
     */
    public String getUuid() {
        return identifier;
    }

    /**
     * Sets the server UUID. Alias for {@link #setIdentifier(String)} provided for
     * semantic clarity in server-related contexts.
     *
     * @param uuid the new UUID string to assign
     */
    public void setUuid(String uuid) {
        this.identifier = uuid;
    }

    /** The human-readable display name of the server. */
    private String name;

    /**
     * The role of this server within the network (e.g. {@code PROXY} or
     * {@code BACKEND}).
     */
    private ISingularityExtension.ServerType type;

    /**
     * Creates a {@code SavedServer} with the given UUID, name, and type.
     *
     * @param identifier the unique UUID string for this server
     * @param name       the human-readable server name
     * @param type       the server role (proxy or backend)
     */
    public SavedServer(String identifier, String name, ISingularityExtension.ServerType type) {
        this.identifier = identifier;
        this.name = name;
        this.type = type;
    }

    /**
     * Asynchronously upserts this server's record in the main database by calling
     * {@link singularity.database.CoreDBOperator#putServerAsync(SavedServer)}.
     */
    public void push() {
        // Push to database.
        Singularity.getMainDatabase().putServerAsync(this);
    }

    /**
     * Constructs a {@link CosmicServer} wrapping this server's display name,
     * suitable for use where a full server object is expected.
     *
     * @return a new {@link CosmicServer} backed by this server's name
     */
    public CosmicServer getCosmicServer() {
        return new CosmicServer(getName());
    }
}
