package net.streamline.platform.handlers;

import net.minecraft.server.level.ServerPlayer;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.savables.UserManager;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.interfaces.IBackendHandler;

public class BackendHandler implements IBackendHandler {

    @Override
    public void teleport(CosmicPlayer player, CosmicLocation location) {
        ServerPlayer p = BasePlugin.getPlayer(player.getUuid());
        if (p == null) return;
        UserManager.teleport(p, location);
    }
}
