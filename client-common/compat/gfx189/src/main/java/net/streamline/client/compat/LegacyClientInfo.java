package net.streamline.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.biome.BiomeGenBase;
import singularity.client.ClientInfo;

import java.util.Locale;

/**
 * {@link ClientInfo} read live from Minecraft 1.8.9's client. 1.8.9 has no namespaced ids for
 * biomes or dimensions, so those are built from its names: {@code minecraft:} plus the
 * biome or dimension name, lowercased with underscores.
 */
final class LegacyClientInfo implements ClientInfo {

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    private static EntityPlayer player() {
        return mc().thePlayer;
    }

    private static String id(String name) {
        if (name == null || name.isEmpty()) return "";
        return "minecraft:" + name.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    @Override
    public boolean inWorld() {
        return mc().theWorld != null && player() != null;
    }

    @Override
    public int fps() {
        return Minecraft.getDebugFPS();
    }

    @Override
    public String playerName() {
        EntityPlayer player = player();
        return player == null ? "" : player.getName();
    }

    @Override
    public double x() {
        EntityPlayer player = player();
        return player == null ? 0 : player.posX;
    }

    @Override
    public double y() {
        EntityPlayer player = player();
        return player == null ? 0 : player.posY;
    }

    @Override
    public double z() {
        EntityPlayer player = player();
        return player == null ? 0 : player.posZ;
    }

    @Override
    public float yaw() {
        EntityPlayer player = player();
        return player == null ? 0 : MathHelper.wrapAngleTo180_float(player.rotationYaw);
    }

    @Override
    public float pitch() {
        EntityPlayer player = player();
        return player == null ? 0 : player.rotationPitch;
    }

    @Override
    public String facing() {
        EntityPlayer player = player();
        return player == null ? "" : player.getHorizontalFacing().getName();
    }

    @Override
    public String biome() {
        WorldClient world = mc().theWorld;
        EntityPlayer player = player();
        if (world == null || player == null) return "";
        BiomeGenBase biome = world.getBiomeGenForCoords(player.getPosition());
        return biome == null ? "" : id(biome.biomeName);
    }

    @Override
    public String dimension() {
        WorldClient world = mc().theWorld;
        return world == null ? "" : id(world.provider.getDimensionName());
    }

    @Override
    public int ping() {
        EntityPlayer player = player();
        NetHandlerPlayClient handler = mc().getNetHandler();
        if (player == null || handler == null) return -1;
        NetworkPlayerInfo info = handler.getPlayerInfo(player.getUniqueID());
        return info == null ? -1 : info.getResponseTime();
    }

    @Override
    public String serverAddress() {
        if (mc().isSingleplayer()) return "singleplayer";
        ServerData server = mc().getCurrentServerData();
        return server == null || server.serverIP == null ? "" : server.serverIP;
    }

    @Override
    public long dayTime() {
        WorldClient world = mc().theWorld;
        return world == null ? 0 : world.getWorldTime() % 24000L;
    }

    @Override
    public long day() {
        WorldClient world = mc().theWorld;
        return world == null ? 0 : world.getWorldTime() / 24000L;
    }

    @Override
    public int screenWidth() {
        return new ScaledResolution(mc()).getScaledWidth();
    }

    @Override
    public int screenHeight() {
        return new ScaledResolution(mc()).getScaledHeight();
    }
}
