package net.streamline.platform.handlers;

import net.streamline.api.interfaces.IBackendHandler;
import net.streamline.api.savables.users.StreamlineLocation;
import net.streamline.api.savables.users.StreamPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BackendHandler implements IBackendHandler {
    @Override
    public void teleport(StreamPlayer player, StreamlineLocation location) {
        Player p = Bukkit.getPlayer(UUID.fromString(player.getUuid()));
        if (p == null) return;

        Location l = new Location(Bukkit.getWorld(location.getWorld()), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        p.teleport(l);
    }
}
