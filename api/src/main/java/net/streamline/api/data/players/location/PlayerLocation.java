package net.streamline.api.data.players.location;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.server.StreamServer;
import org.jetbrains.annotations.NotNull;

@Getter @Setter
public class PlayerLocation implements Comparable<PlayerLocation> {
    private StreamPlayer player;

    private PlayerWorld world;
    private WorldPosition position;
    private PlayerRotation rotation;

    public PlayerLocation(StreamPlayer player, PlayerWorld world, WorldPosition position, PlayerRotation rotation) {
        this.player = player;

        this.world = world;
        this.position = position;
        this.rotation = rotation;
    }

    public PlayerLocation(StreamPlayer player) {
        this(player, new PlayerWorld("--null"), new WorldPosition(0, 0, 0), new PlayerRotation());
    }

    @Override
    public int compareTo(@NotNull PlayerLocation o) {
        return player.compareTo(o.getPlayer());
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
        return player.getServerName();
    }

    public PlayerLocation setX(double x) {
        position.setX(x);
        return this;
    }

    public PlayerLocation setY(double y) {
        position.setY(y);
        return this;
    }

    public PlayerLocation setZ(double z) {
        position.setZ(z);
        return this;
    }

    public PlayerLocation setYaw(float yaw) {
        rotation.setYaw(yaw);
        return this;
    }

    public PlayerLocation setPitch(float pitch) {
        rotation.setPitch(pitch);
        return this;
    }

    public PlayerLocation setWorldName(String worldName) {
        world = new PlayerWorld(worldName);
        return this;
    }

    public PlayerLocation setServerName(String serverName) {
        player.setServerName(serverName);
        return this;
    }

    public String asString() {
        return "[" +
                "player=" + player.getUuid() +
                ",server=" + getServerName() +
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
}
