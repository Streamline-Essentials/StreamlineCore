package net.streamline.base;

import host.plas.bou.libs.universalScheduler.scheduling.tasks.MyScheduledTask;
import lombok.Getter;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.players.location.PlayerLocation;
import net.streamline.api.data.players.location.PlayerRotation;
import net.streamline.api.data.players.location.PlayerWorld;
import net.streamline.api.data.players.location.WorldPosition;
import net.streamline.api.data.server.StreamServer;
import net.streamline.api.messages.builders.PlayerLocationMessageBuilder;
import net.streamline.api.utils.UserUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

@Getter
public class TenSecondTimer implements Runnable {
    final Player player;
    final MyScheduledTask task;

    public TenSecondTimer(Player player) {
        this.player = player;
        this.task = Streamline.getScheduler().runTaskTimerAsynchronously(this, 20 * 2, 20 * 10);
    }

    @Override
    public void run() {
        if (! checkPlayer()) return;

        StreamPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

        StreamServer streamServer = streamPlayer.getServer();
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) world = Bukkit.getWorlds().get(0);
//            StreamServer streamlineServer = new StreamServer("--null");
        PlayerWorld streamlineWorld = new PlayerWorld(world.getName());
        WorldPosition streamlinePosition = new WorldPosition(location.getX(), location.getY(), location.getZ());
        PlayerRotation streamlineRotation = new PlayerRotation(location.getYaw(), location.getPitch());

        PlayerLocation streamlineLocation = new PlayerLocation(streamServer, streamlineWorld, streamlinePosition, streamlineRotation);

        PlayerLocationMessageBuilder.build(streamPlayer, streamlineLocation, streamPlayer).send();
    }

    public boolean checkPlayer() {
        if (player == null) {
            task.cancel();
            return false;
        }
        if (! player.isOnline()) {
            task.cancel();
            return false;
        }
        return true;
    }
}
