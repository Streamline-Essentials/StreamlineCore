package net.streamline.base;

import host.plas.bou.scheduling.BaseRunnable;
import lombok.Getter;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.server.CosmicServer;
import singularity.messages.builders.PlayerLocationMessageBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import singularity.utils.UserUtils;

@Getter
public class TenSecondTimer extends BaseRunnable {
    final Player player;

    public TenSecondTimer(Player player) {
        super(20 * 10, 20 * 10); // Initial delay and period of 10 seconds (20 ticks = 1 second)
        this.player = player;
    }

    @Override
    public void run() {
        if (! checkPlayer()) return;

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
        if (streamPlayer == null) {
            cancel();
            return;
        }

        CosmicServer cosmicServer = streamPlayer.getServer();
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) world = Bukkit.getWorlds().get(0);
//            StreamServer streamlineServer = new StreamServer("--null");
        PlayerWorld streamlineWorld = new PlayerWorld(world.getName());
        WorldPosition streamlinePosition = new WorldPosition(location.getX(), location.getY(), location.getZ());
        PlayerRotation streamlineRotation = new PlayerRotation(location.getYaw(), location.getPitch());

        CosmicLocation streamlineLocation = new CosmicLocation(cosmicServer, streamlineWorld, streamlinePosition, streamlineRotation);

        streamPlayer.setLocation(streamlineLocation);

        PlayerLocationMessageBuilder.build(streamPlayer, streamlineLocation, streamPlayer).send();
    }

    public boolean checkPlayer() {
        if (player == null) {
            cancel();
            return false;
        }
        if (! player.isOnline()) {
            cancel();
            return false;
        }
        return true;
    }
}
