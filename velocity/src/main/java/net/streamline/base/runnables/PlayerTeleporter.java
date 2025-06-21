package net.streamline.base.runnables;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.streamline.api.base.timers.AbstractPlayerTeleporter;
import net.streamline.base.StreamlineVelocity;
import singularity.data.teleportation.TPTicket;
import singularity.utils.MessageUtils;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

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

            Optional<Player> player = StreamlineVelocity.getInstance().getProxy().getPlayer(UUID.fromString(ticket.getIdentifier()));
            if (player.isEmpty()) {
                clearTicket(ticket, 2);
                return;
            }

            teleportPlayerAsync(player.get(), ticket.getTargetServer().getIdentifier());
//            clearTicket(ticket, 3); // Handled by the Spigot side
        } catch (Exception e) {
            MessageUtils.logWarning("Error processing ticket: " + ticket.getIdentifier(), e);
        }
    }

    private void teleportPlayerAsync(Player player, String server) {
        Optional<RegisteredServer> targetServer = StreamlineVelocity.getInstance().getProxy().getServer(server);
        if (targetServer.isPresent()) {
            player.createConnectionRequest(targetServer.get()).connect();
            MessageUtils.logInfo("Teleported player " + player.getUsername() + " to server " + server + ".");
        }
    }

    private static void clearTicket(TPTicket ticket, int instance) {
        ticket.clear();
        MessageUtils.logInfo("Cleared teleportation ticket for player " + ticket.getIdentifier() + ". [" + instance + "]");
    }
}
