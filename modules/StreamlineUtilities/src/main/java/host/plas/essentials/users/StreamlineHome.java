package host.plas.essentials.users;

import lombok.Getter;
import lombok.Setter;
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
}
