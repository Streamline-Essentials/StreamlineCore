package net.streamline.api.savables.users;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.modules.ModuleUtils;

public class StreamlineLocation {
    @Getter
    private static final String nullWorld = "{{NULL}}";
    @Getter
    private static final double nullCoordinate = Double.MIN_VALUE;
    @Getter
    private static final float nullRotation = Float.MIN_VALUE;

    @Getter
    private final String world;
    @Getter
    private final double x;
    @Getter
    private final double y;
    @Getter
    private final double z;
    @Getter
    private final float yaw;
    @Getter
    private final float pitch;

    public StreamlineLocation(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public StreamlineLocation(String string) {
        if (string == null) {
            this.world = getNullWorld();
            this.x = getNullCoordinate();
            this.y = getNullCoordinate();
            this.z = getNullCoordinate();
            this.yaw = getNullRotation();
            this.pitch = getNullRotation();
            return;
        }

        String world;
        double x, y, z;
        float yaw, pitch;

        try {
            String[] split = string.split(",");
            if (split.length != 6) {
                world = getNullWorld();
                x = getNullCoordinate();
                y = getNullCoordinate();
                z = getNullCoordinate();
                yaw = getNullRotation();
                pitch = getNullRotation();
            } else {
                world = split[0];
                x = Double.parseDouble(split[1]);
                y = Double.parseDouble(split[2]);
                z = Double.parseDouble(split[3]);
                yaw = Float.parseFloat(split[4]);
                pitch = Float.parseFloat(split[5]);
            }
        } catch (Exception e) {
            world = getNullWorld();
            x = getNullCoordinate();
            y = getNullCoordinate();
            z = getNullCoordinate();
            yaw = getNullRotation();
            pitch = getNullRotation();
        }
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public boolean isNull() {
        return world.equals(getNullWorld()) && x == getNullCoordinate() && y == getNullCoordinate() && z == getNullCoordinate() && yaw == getNullRotation() && pitch == getNullRotation();
    }

    public void teleport(StreamlinePlayer player, String server) {
        ModuleUtils.connect(player, server);
    }

    @Override
    public String toString() {
        return world + "," + x + "," + y + "," + z + "," + yaw + "," + pitch;
    }
}
