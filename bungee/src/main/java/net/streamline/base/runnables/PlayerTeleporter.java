package net.streamline.base.runnables;

import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.streamline.api.base.timers.AbstractPlayerTeleporter;
import net.streamline.base.StreamlineBungee;
import singularity.data.teleportation.TPTicket;
import singularity.utils.MessageUtils;

import java.util.Date;
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

            ProxiedPlayer player = StreamlineBungee.getInstance().getProxy().getPlayer(UUID.fromString(ticket.getIdentifier()));
            if (player == null) {
                clearTicket(ticket, 2);
                return;
            }

            teleportPlayerAsync(player, ticket.getTargetServer().getIdentifier());
//            clearTicket(ticket, 3); // Handled by the Spigot side
        } catch (Exception e) {
            MessageUtils.logWarning("Error processing ticket: " + ticket.getIdentifier(), e);
        }
    }

    private void teleportPlayerAsync(ProxiedPlayer player, String server) {
        ServerInfo targetServer = StreamlineBungee.getInstance().getProxy().getServerInfo(server);
        if (targetServer != null) {
            ServerConnectRequest request = ServerConnectRequest.builder()
                    .target(targetServer)
                    .reason(ServerConnectEvent.Reason.PLUGIN)
                    .build();
            player.connect(request);
        }
    }

    private static void clearTicket(TPTicket ticket, int instance) {
        ticket.clear();
        MessageUtils.logInfo("Cleared teleportation ticket for player " + ticket.getIdentifier() + ". [" + instance + "]");
    }
}
