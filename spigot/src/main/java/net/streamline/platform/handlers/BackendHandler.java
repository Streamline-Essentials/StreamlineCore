package net.streamline.platform.handlers;

import singularity.data.players.CosmicPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import singularity.data.players.location.CosmicLocation;
import singularity.interfaces.IBackendHandler;

import java.util.UUID;

public class BackendHandler implements IBackendHandler {
    @Override
    public void teleport(CosmicPlayer player, CosmicLocation location) {
        Player p = Bukkit.getPlayer(UUID.fromString(player.getUuid()));
        if (p == null) return;

        Location l = new Location(Bukkit.getWorld(location.getWorldName()), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        p.teleport(l);
    }
}
