package singularity.timers;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.players.CosmicPlayer;
import singularity.data.server.CosmicServer;
import singularity.data.teleportation.TPTicket;
import singularity.modules.ModuleUtils;
import singularity.scheduler.BaseRunnable;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A recurring scheduler task that processes pending {@link TPTicket}s every 5 ticks.
 *
 * <p>On each execution the flusher iterates over all tickets in
 * {@link TPTicket#getPendingTickets()} and attempts to satisfy each one:
 * <ul>
 *   <li>On a proxy server: switches the target player to the ticket's destination
 *       server, or clears the ticket if the player is offline.</li>
 *   <li>On a backend server: teleports the player to the ticket's location if this
 *       server is the intended target and the ticket has not expired.</li>
 * </ul>
 *
 * <p>A static {@link AtomicBoolean} guard prevents re-entrant execution if a previous
 * run has not yet finished.
 */
public class TPTicketFlusher extends BaseRunnable {

    /**
     * Guard flag set to {@code true} while a flush is in progress to prevent
     * concurrent or re-entrant execution.
     */
    @Getter @Setter
    private static AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Creates and registers the {@code TPTicketFlusher} with a period of 5 ticks
     * and no initial delay.
     */
    public TPTicketFlusher() {
        super(0, 5);

        MessageUtils.logInfo("Registered &cTPTicket Flusher&r...");
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
        if (running.get()) return;
        running.set(true);

        ConcurrentSkipListSet<TPTicket> pending = TPTicket.getPendingTickets();
        if (pending.isEmpty()) {
            running.set(false);
            return;
        }

        pending.forEach(ticket -> {
            try {
                CosmicServer targetServer = ticket.getTargetServer();

                if (Singularity.isProxy()) {
                    CosmicPlayer p = UserUtils.getPlayer(ticket.getIdentifier()).orElse(null);
                    if (p == null || !p.isOnline()) {
                        MessageUtils.logDebug("&cTPTicketFlusher&f: &dTPTicket &ffor &d" + ticket.getIdentifier() + " &fcould not find player, clearing it.");
                        ticket.clear();
                        return;
                    }

                    if (p.getServer().equals(targetServer)) {
                        ticket.unpend();
                        return;
                    }

                    p.connect(targetServer.getIdentifier());
                    ticket.unpend();
                    return;
                }

                if (!targetServer.equals(getOwnServer())) {
                    MessageUtils.logDebug("&cTPTicketFlusher&f: &dTPTicket &ffor &d" + ticket.getIdentifier() + " &fis not for this server, clearing it.");
                    ticket.unpend();
                    return;
                }

                if (ticket.isOld()) {
                    MessageUtils.logWarning("&cTPTicketFlusher&f: &dTPTicket &ffor &d" + ticket.getIdentifier() + " &fis too old, clearing it.");
                    ticket.clear();
                    return;
                }

                CosmicPlayer p = UserUtils.getPlayer(ticket.getIdentifier()).orElse(null);
                if (p == null || !p.isOnline()) return;

                ticket.teleportWithDelayAndClear(20);
                ticket.clear();
            } catch (Throwable e) {
                MessageUtils.logWarning("&cTPTicketFlusher&f: An error occurred while processing &dTPTicket &ffor &d" + ticket.getIdentifier() + "&f: " + e.getMessage());
                e.printStackTrace();
            }
        });

        running.set(false);
    }

    /**
     * Returns the {@link CosmicServer} that represents the server this flusher is
     * currently running on.
     *
     * @return the current server's {@link CosmicServer} instance
     */
    public static CosmicServer getOwnServer() {
        return GivenConfigs.getServer().getCosmicServer();
    }
}
