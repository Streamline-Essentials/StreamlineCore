package net.streamline.platform.handlers;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
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
        ServerPlayerEntity p = ServerLifecycleHooks.getCurrentServer()
                .getPlayerList().getPlayer(UUID.fromString(player.getUuid()));
        if (p == null) return;

        WorldPosition pos = location.getPosition();
        PlayerRotation rot = location.getRotation();

        ServerWorld world = (ServerWorld) ServerLifecycleHooks.getCurrentServer().overworld();
        p.teleportTo(world, pos.getX(), pos.getY(), pos.getZ(), rot.getYaw(), rot.getPitch());
    }
}
