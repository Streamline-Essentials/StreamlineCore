package net.streamline.platform.handlers;

import net.minecraft.server.level.ServerPlayer;
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
        ServerPlayer p = StreamlineFabric.getInstance().getServer()
                .getPlayerList().getPlayer(UUID.fromString(player.getUuid()));
        if (p == null) return;

        WorldPosition pos = location.getPosition();
        PlayerRotation rot = location.getRotation();

        p.teleportTo(pos.getX(), pos.getY(), pos.getZ());
        p.setYRot(rot.getYaw());
        p.setXRot(rot.getPitch());
    }
}
