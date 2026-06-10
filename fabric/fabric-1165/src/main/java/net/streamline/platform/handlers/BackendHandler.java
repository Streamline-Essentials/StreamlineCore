package net.streamline.platform.handlers;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.streamline.base.StreamlineFabric;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.WorldPosition;
import singularity.interfaces.IBackendHandler;

import java.util.UUID;

public class BackendHandler implements IBackendHandler {

    @Override
    public void teleport(CosmicPlayer player, CosmicLocation location) {
        if (StreamlineFabric.getInstance().getServer() == null) return;
        ServerPlayerEntity p = StreamlineFabric.getInstance().getServer()
                .getPlayerManager().getPlayer(UUID.fromString(player.getUuid()));
        if (p == null) return;

        WorldPosition pos = location.getPosition();
        PlayerRotation rot = location.getRotation();

        // Default to overworld
        ServerWorld world = StreamlineFabric.getInstance().getServer().getOverworld();
        p.teleport(world, pos.getX(), pos.getY(), pos.getZ(), rot.getYaw(), rot.getPitch());
    }
}
