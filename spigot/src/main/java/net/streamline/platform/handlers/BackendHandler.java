package net.streamline.platform.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BackendHandler implements IBackendHandler {
    @Override
    public void teleport(StreamlinePlayer player, StreamlineLocation location) {
        Player p = Bukkit.getPlayer(UUID.fromString(player.getUuid()));
        if (p == null) return;

        Location l = new Location(Bukkit.getWorld(location.getWorld()), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        p.teleport(l);
    }
}
