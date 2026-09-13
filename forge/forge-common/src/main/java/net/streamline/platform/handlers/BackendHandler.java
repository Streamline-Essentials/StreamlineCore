package net.streamline.platform.handlers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.WorldPosition;
import singularity.interfaces.IBackendHandler;

import java.util.UUID;

public class BackendHandler implements IBackendHandler {

    @Override
    public void teleport(CosmicPlayer player, CosmicLocation location) {
        if (ServerLifecycleHooks.getCurrentServer() == null) return;
        ServerPlayer p = ServerLifecycleHooks.getCurrentServer()
                .getPlayerList().getPlayer(UUID.fromString(player.getUuid()));
        if (p == null) return;

        WorldPosition pos = location.getPosition();
        PlayerRotation rot = location.getRotation();

        // Default to overworld; a proper implementation would resolve by world name
        ServerLevel world = ServerLifecycleHooks.getCurrentServer().overworld();
        p.teleportTo(world, pos.getX(), pos.getY(), pos.getZ(), rot.getYaw(), rot.getPitch());
    }
}
