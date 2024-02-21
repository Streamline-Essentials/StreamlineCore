package net.streamline.platform.handlers;

import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.interfaces.IBackendHandler;
import net.streamline.api.data.players.location.PlayerLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BackendHandler implements IBackendHandler {
    @Override
    public void teleport(StreamPlayer player, PlayerLocation location) {
        Player p = Bukkit.getPlayer(UUID.fromString(player.getUuid()));
        if (p == null) return;

        Location l = new Location(Bukkit.getWorld(location.getWorldName()), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        p.teleport(l);
    }
}
