package host.plas.essentials.users;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.server.CosmicServer;
import singularity.modules.ModuleUtils;

@Getter @Setter
public class StreamlineHome extends CosmicLocation {
    private String name;

    public StreamlineHome(String name, String server, String world, double x, double y, double z, float yaw, float pitch) {
        super(new CosmicServer(server), new PlayerWorld(world), new WorldPosition(x, y, z), new PlayerRotation(yaw, pitch));
        this.name = name;
    }

    public void teleport(CosmicPlayer player) {
        player.teleport(this);
    }

    /**
     * Homes are keyed by name: a user's homes live in a sorted set, and ordering them by
     * position would merge two differently named homes set on the same spot.
     */
    @Override
    public int compareTo(@NotNull CosmicLocation o) {
        if (o instanceof StreamlineHome) return getName().compareTo(((StreamlineHome) o).getName());

        return super.compareTo(o);
    }
}
