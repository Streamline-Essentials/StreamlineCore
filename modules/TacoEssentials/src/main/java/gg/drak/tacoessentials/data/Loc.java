package gg.drak.tacoessentials.data;

import lombok.Getter;
import singularity.Singularity;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.server.CosmicServer;

/**
 * A position in a world, as stored in the database. The world is kept as the platform's own
 * name for it (a Bukkit world name, or a dimension id such as {@code minecraft:the_nether} on
 * the mod loaders) so rows stay readable and editable in an external database client.
 */
@Getter
public final class Loc {

    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public Loc(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static Loc of(CosmicLocation location) {
        return new Loc(location.getWorldName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    public CosmicLocation toCosmic() {
        return new CosmicLocation(new CosmicServer(Singularity.getServerName()), new PlayerWorld(world),
                new WorldPosition(x, y, z), new PlayerRotation(yaw, pitch));
    }

    /** {@code world x, y, z} with the {@code minecraft:} namespace dropped. */
    public String describe() {
        return String.format("%s %.0f, %.0f, %.0f", shortWorld(world), x, y, z);
    }

    public static String shortWorld(String world) {
        return world.startsWith("minecraft:") ? world.substring("minecraft:".length()) : world;
    }
}
