package singularity.data.players.location;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BlockPosition implements Comparable<BlockPosition> {
    private int x, y, z;

    public BlockPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int compareTo(BlockPosition o) {
        if (x != o.x) return x - o.x;
        if (y != o.y) return y - o.y;
        return z - o.z;
    }

    public double distance(BlockPosition other) {
        return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2) + Math.pow(z - other.z, 2));
    }

    public WorldPosition asWorldPosition() {
        return new WorldPosition(x, y, z);
    }
}
