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

    /** {@code world;x;y;z;yaw;pitch}; world names and dimension ids never hold a {@code ;}. */
    public String serialize() {
        return world + ";" + x + ";" + y + ";" + z + ";" + yaw + ";" + pitch;
    }

    /** The location {@link #serialize()} wrote, or {@code null} when the text is not one. */
    public static Loc parse(String text) {
        if (text == null) return null;
        String[] parts = text.split(";");
        if (parts.length != 6) return null;
        try {
            return new Loc(parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Whether both sit in the same block of the same world. */
    public boolean sameBlock(Loc other) {
        return world.equals(other.world) && (int) Math.floor(x) == (int) Math.floor(other.x)
                && (int) Math.floor(y) == (int) Math.floor(other.y) && (int) Math.floor(z) == (int) Math.floor(other.z);
    }

    public static String shortWorld(String world) {
        return world.startsWith("minecraft:") ? world.substring("minecraft:".length()) : world;
    }
}
