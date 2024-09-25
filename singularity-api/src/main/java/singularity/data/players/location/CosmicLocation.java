package singularity.data.players.location;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.data.server.CosmicServer;
import org.jetbrains.annotations.NotNull;

@Getter @Setter
public class CosmicLocation implements Comparable<CosmicLocation> {
    private CosmicServer server;
    private PlayerWorld world;
    private WorldPosition position;
    private PlayerRotation rotation;

    public CosmicLocation(CosmicServer server, PlayerWorld world, WorldPosition position, PlayerRotation rotation) {
        this.server = server;
        this.world = world;
        this.position = position;
        this.rotation = rotation;
    }

    public CosmicLocation(CosmicPlayer player) {
        this(new CosmicServer(""), new PlayerWorld("--null"), new WorldPosition(0, 0, 0), new PlayerRotation());
    }

    @Override
    public int compareTo(@NotNull CosmicLocation o) {
        if (world.compareTo(o.world) != 0) return world.compareTo(o.world);
        if (position.compareTo(o.position) != 0) return position.compareTo(o.position);
        return rotation.getYaw() == o.rotation.getYaw() && rotation.getPitch() == o.rotation.getPitch() ? 0 : 1;
    }

    public double getX() {
        return position.getX();
    }

    public double getY() {
        return position.getY();
    }

    public double getZ() {
        return position.getZ();
    }

    public float getYaw() {
        return rotation.getYaw();
    }

    public float getPitch() {
        return rotation.getPitch();
    }

    public BlockPosition asBlockPosition() {
        return getPosition().asBlockPosition();
    }

    public int getBlockX() {
        return asBlockPosition().getX();
    }

    public int getBlockY() {
        return asBlockPosition().getY();
    }

    public int getBlockZ() {
        return asBlockPosition().getZ();
    }

    public String getWorldName() {
        return world.getIdentifier();
    }

    public String getServerName() {
        return getServer().getIdentifier();
    }

    public CosmicLocation setX(double x) {
        position.setX(x);
        return this;
    }

    public CosmicLocation setY(double y) {
        position.setY(y);
        return this;
    }

    public CosmicLocation setZ(double z) {
        position.setZ(z);
        return this;
    }

    public CosmicLocation setYaw(float yaw) {
        rotation.setYaw(yaw);
        return this;
    }

    public CosmicLocation setPitch(float pitch) {
        rotation.setPitch(pitch);
        return this;
    }

    public CosmicLocation setWorldName(String worldName) {
        world = new PlayerWorld(worldName);
        return this;
    }

    public CosmicLocation setServerName(String serverName) {
        server = new CosmicServer(serverName);
        return this;
    }

    public String asString() {
        return "[" +
                "server=" + getServerName() +
                ",world=" + getWorldName() +
                ",position={"
                + "x=" + getX() +
                ",y=" + getY() +
                ",z=" + getZ() +
                "}" +
                ",rotation={"
                + "yaw=" + getYaw() +
                ",pitch=" + getPitch() +
                "}" +
                "]";
    }

    public void teleport(CosmicPlayer otherPlayer) {
        Singularity.getInstance().getUserManager().teleport(otherPlayer, this);
    }
}
