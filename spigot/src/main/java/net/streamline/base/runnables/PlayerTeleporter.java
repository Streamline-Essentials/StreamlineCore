package net.streamline.base.runnables;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import host.plas.bou.scheduling.TaskManager;
import lombok.Getter;
import lombok.Setter;
import net.streamline.base.Streamline;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.teleportation.TPTicket;
import singularity.scheduler.BaseRunnable;
import singularity.utils.MessageUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerTeleporter extends BaseRunnable {
    @Getter @Setter
    private static AtomicBoolean checking = new AtomicBoolean(false);

    public PlayerTeleporter() {
        super(0, 10);
    }

    @Override
    public void run() {
        if (checking.get()) return;

        checking.set(true);
        GivenConfigs.getMainDatabase().pullAllTPTickets().whenComplete((tickets, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                checking.set(false);
                return;
            }

            tickets.forEach(ticket -> {
                Player player = Streamline.getInstance().getProxy().getPlayer(ticket.getIdentifier());
                if (player == null) {
                    ticket.clear();
                    return;
                }

                teleportPlayerAsync(player, ticket);
            });

            checking.set(false);
        });
    }

    private static void teleportPlayerAsync(Player player, TPTicket ticket) {
        PlayerWorld world = ticket.getTargetWorld();
        WorldPosition position = ticket.getTargetLocation();
        PlayerRotation rotation = ticket.getTargetRotation();

        CompletableFuture.runAsync(() -> {
            AtomicBoolean success = new AtomicBoolean(false);

            TaskManager.runTask(() -> {
                World targetWorld = Bukkit.getWorld(world.getIdentifier());
                if (targetWorld == null) {
                    MessageUtils.logWarning("World " + world.getIdentifier() + " not found for teleportation.");
                    success.set(false);
                    return;
                }

                Location location = rotation != null
                        ? new Location(targetWorld, position.getX(), position.getY(), position.getZ(), rotation.getYaw(), rotation.getPitch())
                        : new Location(targetWorld, position.getX(), position.getY(), position.getZ());

                TaskManager.teleport(player, location);
                success.set(true);
            });

//            long startTime = System.currentTimeMillis();
//
//            // If success is true OR 5 seconds have passed, stop waiting
//            while (! success.get() && System.currentTimeMillis() - startTime < 5000) {
//                Thread.onSpinWait();
//            }

            if (success.get()) {
                MessageUtils.logInfo(String.format(
                        "Teleported %s to %s at %.2f, %.2f, %.2f with yaw %.2f and pitch %.2f.",
                        player.getName(), world.getIdentifier(), position.getX(), position.getY(), position.getZ(),
                        rotation != null ? rotation.getYaw() : 0, rotation != null ? rotation.getPitch() : 0
                ));

                ticket.clear();
            } else {
                MessageUtils.logWarning("Failed to teleport " + player.getName() + " within the allotted time.");
            }
        });
    }
}