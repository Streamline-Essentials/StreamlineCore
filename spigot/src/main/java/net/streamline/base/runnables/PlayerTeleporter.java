package net.streamline.base.runnables;

import host.plas.bou.scheduling.TaskManager;
import lombok.Getter;
import lombok.Setter;
import net.streamline.base.Streamline;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import singularity.configs.given.GivenConfigs;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.teleportation.TPTicket;
import singularity.utils.MessageUtils;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerTeleporter extends Thread {
    public static final long TICKING_FREQUENCY = 50L;

    @Getter @Setter
    private static PlayerTeleporter instance;

    public static void init() {
        instance = new PlayerTeleporter();
        instance.start();
    }

    public static void stopInstance() {
        try {
            if (instance != null) {
                instance.interrupt();
                instance = null;
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Getter @Setter
    private static AtomicReference<TeleportStage> stage = new AtomicReference<>(TeleportStage.READY);
    @Getter @Setter
    private static AtomicLong lastRun = new AtomicLong(0);

    public enum TeleportStage {
        COLLECTION,
        TELEPORTATION,
        READY,
        ;
    }

    public static boolean isAbleToRunAgain() {
        return lastRun.get() + TICKING_FREQUENCY < System.currentTimeMillis();
    }

    public static void setLastRun() {
        lastRun.set(System.currentTimeMillis());
    }

    public PlayerTeleporter() {
        super("SL - Player Teleporter");
    }

    @Override
    public void run() {
        if (! isAbleToRunAgain()) return;

        setLastRun();

        stage.set(TeleportStage.COLLECTION);
        ConcurrentSkipListSet<TPTicket> tickets = GivenConfigs.getMainDatabase().pullAllTPTickets().join();

        stage.set(TeleportStage.TELEPORTATION);
        tickets.forEach(ticket -> {
            if (ticket.getCreateDate().before(new Date(System.currentTimeMillis() - 7 * 1000))) {
                ticket.clear();
                return;
            }

            if (ticket.getTargetServer().getIdentifier().equals(GivenConfigs.getServer().getIdentifier())) {
                Player player = Streamline.getInstance().getProxy().getPlayer(UUID.fromString(ticket.getIdentifier()));
                if (player == null) {
                    ticket.clear();
                    return;
                }

                teleportPlayer(player, ticket);
                ticket.clear();
            }
        });

        stage.set(TeleportStage.READY);
    }

    private static void teleportPlayer(Player player, TPTicket ticket) {
        PlayerWorld world = ticket.getTargetWorld();
        WorldPosition position = ticket.getTargetLocation();
        PlayerRotation rotation = ticket.getTargetRotation();

        TaskManager.runTask(() -> {
            World targetWorld = Bukkit.getWorld(world.getIdentifier());
            if (targetWorld == null) {
                MessageUtils.logWarning("World " + world.getIdentifier() + " not found for teleportation.");
                return;
            }

            Location location = rotation != null
                    ? new Location(targetWorld, position.getX(), position.getY(), position.getZ(), rotation.getYaw(), rotation.getPitch())
                    : new Location(targetWorld, position.getX(), position.getY(), position.getZ());

            TaskManager.teleport(player, location);

            MessageUtils.logInfo(String.format(
                    "Teleported %s to %s at %.2f, %.2f, %.2f with yaw %.2f and pitch %.2f.",
                    player.getName(), world.getIdentifier(), position.getX(), position.getY(), position.getZ(),
                    rotation != null ? rotation.getYaw() : 0, rotation != null ? rotation.getPitch() : 0
            ));
        });
    }
}