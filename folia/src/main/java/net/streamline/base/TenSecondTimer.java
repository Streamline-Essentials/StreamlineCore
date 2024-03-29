package net.streamline.base;

import lombok.Getter;
import net.streamline.api.messages.builders.PlayerLocationMessageBuilder;
import net.streamline.api.savables.users.StreamlineLocation;
import net.streamline.api.savables.users.StreamPlayer;
import net.streamline.platform.savables.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

@Getter
public class TenSecondTimer implements Runnable {
    final Player player;
    final int taskId;

    public TenSecondTimer(Player player) {
        this.player = player;
        this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Streamline.getInstance(), this, 20 * 2, 20 * 10);
    }

    @Override
    public void run() {
        if (! checkPlayer()) return;

        StreamPlayer StreamPlayer = UserManager.getInstance().getOrGetPlayer(player);

        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) world = Bukkit.getWorlds().get(0);
        StreamlineLocation streamlineLocation = new StreamlineLocation(world.getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        PlayerLocationMessageBuilder.build(StreamPlayer, streamlineLocation, StreamPlayer).send();
    }

    public boolean checkPlayer() {
        if (player == null) {
            Bukkit.getScheduler().cancelTask(taskId);
            return false;
        }
        if (! player.isOnline()) {
            Bukkit.getScheduler().cancelTask(taskId);
            return false;
        }
        return true;
    }
}
