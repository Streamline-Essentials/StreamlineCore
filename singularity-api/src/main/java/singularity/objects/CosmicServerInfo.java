package singularity.objects;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * Snapshot of information about a single Minecraft server as seen from the
 * proxy.
 *
 * <p>Instances hold the server's unique identifier, human-readable name, MOTD,
 * network address, and the UUIDs of players currently connected to that server.
 * The online-user set can be refreshed at any time via
 * {@link #updateUsersTo(List)} or {@link #updateUsersTo(CosmicPlayer...)}.
 */
@Getter
@Setter
public class CosmicServerInfo {

    /**
     * The unique, internal identifier of this server (e.g. {@code "lobby"}).
     * Immutable after construction.
     */
    private final String identifier;

    /** The human-readable display name of this server. */
    private String name;

    /** The Message of the Day currently configured for this server. */
    private String motd;

    /** The network address (host:port) of this server. */
    private String address;

    /**
     * The UUIDs of players who are currently connected to this server.
     * Updated by {@link #updateUsersTo}.
     */
    private ConcurrentSkipListSet<String> onlineUsers;

    /**
     * Constructs a fully populated {@code CosmicServerInfo}.
     *
     * @param identifier  the unique server identifier; must not be {@code null}
     * @param name        the display name of the server
     * @param motd        the server's Message of the Day
     * @param address     the network address in {@code host:port} form
     * @param onlineUsers the UUIDs of players currently on this server
     */
    public CosmicServerInfo(String identifier, String name, String motd, String address, ConcurrentSkipListSet<String> onlineUsers) {
        this.identifier = identifier;
        this.name = name;
        this.motd = motd;
        this.address = address;
        this.onlineUsers = onlineUsers;
    }

    /**
     * Replaces the current online-user set with the UUIDs extracted from the
     * given list of players.
     *
     * @param users the players now present on this server
     */
    public void updateUsersTo(List<CosmicPlayer> users) {
        onlineUsers = new ConcurrentSkipListSet<>();
        users.forEach(a -> onlineUsers.add(a.getUuid()));
    }

    /**
     * Replaces the current online-user set with the UUIDs extracted from the
     * given players (varargs overload of {@link #updateUsersTo(List)}).
     *
     * @param users the players now present on this server
     */
    public void updateUsersTo(CosmicPlayer... users) {
        updateUsersTo(Arrays.stream(users).collect(Collectors.toList()));
    }
}
