package singularity.objects.world;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;

@Getter @Setter
public class CosmicBlock implements Comparable<CosmicBlock> {
    private PlayerWorld world;
    private WorldPosition location;

    private String type;

    public CosmicBlock(PlayerWorld world, WorldPosition location, String type) {
        this.world = world;
        this.location = location;
        this.type = type;
    }

    public double getX() {
        return location.getX();
    }

    public double getY() {
        return location.getY();
    }

    public double getZ() {
        return location.getZ();
    }

    public String getWorldName() {
        return world.getIdentifier();
    }

    public CosmicBlock setX(double x) {
        this.location.setX(x);
        return this;
    }

    public CosmicBlock setY(double y) {
        this.location.setY(y);
        return this;
    }

    public CosmicBlock setZ(double z) {
        this.location.setZ(z);
        return this;
    }

    public CosmicBlock setWorld(PlayerWorld world) {
        this.world = world;
        return this;
    }

    public CosmicBlock setWorld(String worldName) {
        this.world = new PlayerWorld(worldName);
        return this;
    }

    public double distance(CosmicBlock other) {
        if (other == null) return -1d;

        return getLocation().distance(other.getLocation());
    }

    @Override
    public int compareTo(@NotNull CosmicBlock o) {
        if (this.world != o.world) {
            return this.world.compareTo(o.world);
        } else {
            return this.location.compareTo(o.location);
        }
    }
}
