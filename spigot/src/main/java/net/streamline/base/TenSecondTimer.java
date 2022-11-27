package net.streamline.base;

import net.streamline.api.messages.builders.PlayerLocationMessageBuilder;
import net.streamline.api.savables.users.StreamlineLocation;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.platform.savables.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class TenSecondTimer implements Runnable {
    @Override
    public void run() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            StreamlinePlayer streamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);

            Location location = player.getLocation();
            World world = location.getWorld();
            if (world == null) world = Bukkit.getWorlds().get(0);
            StreamlineLocation streamlineLocation = new StreamlineLocation(world.getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

            PlayerLocationMessageBuilder.build(streamlinePlayer, streamlineLocation, streamlinePlayer).send();
        });
    }
}
