package net.streamline.api.data.players.location;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter @Setter
public class WorldPosition implements Comparable<WorldPosition> {
    private double x, y, z;

    public WorldPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int compareTo(@NotNull WorldPosition o) {
        if (x != o.x) return Double.compare(x, o.x);
        if (y != o.y) return Double.compare(y, o.y);
        return Double.compare(z, o.z);
    }

    public WorldPosition copy() {
        return new WorldPosition(x, y, z);
    }

    public double distance(WorldPosition other) {
        return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2) + Math.pow(z - other.z, 2));
    }

    public BlockPosition asBlockPosition() {
        int x = (int) Math.round(this.x);
        int y = (int) Math.round(this.y);
        int z = (int) Math.round(this.z);

        return new BlockPosition(x, y, z);
    }
}
