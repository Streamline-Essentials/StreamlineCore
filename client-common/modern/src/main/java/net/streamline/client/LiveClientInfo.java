package net.streamline.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.streamline.client.compat.ClientCompat;
import net.streamline.platform.compat.McCompat;
import singularity.client.ClientInfo;

/**
 * {@link ClientInfo} read straight from the running game. Meant for the render thread; from
 * another thread the values may be a frame out of date.
 */
public final class LiveClientInfo implements ClientInfo {

    private static final long DAY_TICKS = 24000L;

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static LocalPlayer player() {
        Minecraft mc = mc();
        return mc == null ? null : mc.player;
    }

    @Override
    public boolean inWorld() {
        Minecraft mc = mc();
        return mc != null && mc.player != null && mc.level != null;
    }

    @Override
    public int fps() {
        return mc().getFps();
    }

    @Override
    public String playerName() {
        LocalPlayer player = player();
        return player == null ? "" : player.getName().getString();
    }

    @Override
    public double x() {
        LocalPlayer player = player();
        return player == null ? 0 : player.getX();
    }

    @Override
    public double y() {
        LocalPlayer player = player();
        return player == null ? 0 : player.getY();
    }

    @Override
    public double z() {
        LocalPlayer player = player();
        return player == null ? 0 : player.getZ();
    }

    @Override
    public float yaw() {
        LocalPlayer player = player();
        if (player == null) return 0;
        float yaw = player.getYRot() % 360f;
        if (yaw >= 180f) yaw -= 360f;
        if (yaw < -180f) yaw += 360f;
        return yaw;
    }

    @Override
    public float pitch() {
        LocalPlayer player = player();
        return player == null ? 0 : player.getXRot();
    }

    @Override
    public String facing() {
        LocalPlayer player = player();
        return player == null ? "" : player.getDirection().getName();
    }

    @Override
    public String biome() {
        LocalPlayer player = player();
        ClientLevel level = mc().level;
        if (player == null || level == null) return "";
        try {
            return McCompat.biomeId(level.getBiome(player.blockPosition()));
        } catch (RuntimeException e) {
            return "";
        }
    }

    @Override
    public String dimension() {
        ClientLevel level = mc().level;
        return level == null ? "" : ClientCompat.dimensionId(level);
    }

    @Override
    public int ping() {
        LocalPlayer player = player();
        ClientPacketListener connection = mc().getConnection();
        if (player == null || connection == null) return -1;
        PlayerInfo info = connection.getPlayerInfo(player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    @Override
    public String serverAddress() {
        Minecraft mc = mc();
        if (! inWorld()) return "";
        if (mc.hasSingleplayerServer()) return "singleplayer";
        ServerData server = mc.getCurrentServer();
        return server == null || server.ip == null ? "" : server.ip;
    }

    @Override
    public long dayTime() {
        ClientLevel level = mc().level;
        return level == null ? 0 : Math.floorMod(ClientCompat.dayTime(level), DAY_TICKS);
    }

    @Override
    public long day() {
        ClientLevel level = mc().level;
        return level == null ? 0 : Math.floorDiv(ClientCompat.dayTime(level), DAY_TICKS);
    }

    @Override
    public int screenWidth() {
        return mc().getWindow().getGuiScaledWidth();
    }

    @Override
    public int screenHeight() {
        return mc().getWindow().getGuiScaledHeight();
    }
}
