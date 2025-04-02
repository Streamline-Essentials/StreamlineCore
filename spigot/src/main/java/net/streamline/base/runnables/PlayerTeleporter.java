package net.streamline.base.runnables;

import host.plas.bou.scheduling.TaskManager;
import net.streamline.api.base.timers.AbstractPlayerTeleporter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import singularity.configs.given.GivenConfigs;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.server.CosmicServer;
import singularity.data.teleportation.TPTicket;
import singularity.utils.MessageUtils;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerTeleporter extends AbstractPlayerTeleporter {
//    private static final ExecutorService executor = Executors.newFixedThreadPool(4); // Adjust as needed

    public static void init() {
        setInstance(new PlayerTeleporter());
        startInstance();
    }

    @Override
    public void tick() {
        if (areTicketsPending()) return;

        getStage().set(TeleportStage.COLLECTION);
        ConcurrentSkipListSet<TPTicket> tickets = getTicketsPending().join();

        getStage().set(TeleportStage.TELEPORTATION);
//        tickets.forEach(ticket -> executor.submit(() -> processTicket(ticket)));
        tickets.forEach(this::processTicket);

        unpendTickets();
        getStage().set(TeleportStage.READY);
    }

    private void processTicket(TPTicket ticket) {
        try {
            if (ticket.getCreateDate().before(new Date(System.currentTimeMillis() - (7 * 1000)))) {
                clearTicket(ticket, 1);
                return;
            }

            if (isOnCorrectServer(ticket)) {
                Player player = Bukkit.getPlayer(UUID.fromString(ticket.getIdentifier()));
                if (player == null) {
                    clearTicket(ticket, 2);
                    return;
                }

                teleportPlayer(player, ticket);
                clearTicket(ticket, 3);
            }
        } catch (Exception e) {
            MessageUtils.logWarning("Error processing ticket: " + ticket.getIdentifier(), e);
        }
    }

    public static boolean isOnCorrectServer(TPTicket ticket) {
        CosmicServer targetServer = ticket.getTargetServer();
        String myServer = GivenConfigs.getServerName();

        if (targetServer == null || myServer == null) {
            return true;
        }

        if (targetServer.getIdentifier().equals("--null")) return true;

        return targetServer.getIdentifier().equals(myServer);
    }

    private void teleportPlayer(Player player, TPTicket ticket) {
        PlayerWorld world = ticket.getTargetWorld();
        WorldPosition position = ticket.getTargetLocation();
        PlayerRotation rotation = ticket.getTargetRotation();

        TaskManager.runTask(() -> {
            try {
                World targetWorld = Bukkit.getWorld(world.getIdentifier());
                if (targetWorld == null) {
                    MessageUtils.logWarning("World " + world.getIdentifier() + " not found for teleportation.");
                    return;
                }

                Location location = new Location(
                        targetWorld,
                        position.getX(),
                        position.getY(),
                        position.getZ(),
                        rotation != null ? rotation.getYaw() : 0,
                        rotation != null ? rotation.getPitch() : 0
                );

                player.teleport(location);
//                MessageUtils.logInfo("Teleported " + player.getName() + " to X: " + position.getX() +
//                        ", Y: " + position.getY() + ", Z: " + position.getZ() + " in world " + world.getIdentifier());
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to teleport player " + player.getName(), e);
            }
        });
    }

    private static void clearTicket(TPTicket ticket, int instance) {
        ticket.clear();
//        MessageUtils.logInfo("Cleared teleportation ticket for player " + ticket.getIdentifier() + ". [" + instance + "]");
    }
}
