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

public class TPTicketFlusher extends BaseRunnable {
    @Getter @Setter
    private static AtomicBoolean running = new AtomicBoolean(false);

    public TPTicketFlusher() {
        super(0, 5);

        MessageUtils.logInfo("Registered &cTPTicket Flusher&r...");
    }

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
            CosmicServer targetServer = ticket.getTargetServer();

            if (Singularity.isProxy()) {
                CosmicPlayer p = UserUtils.getPlayer(ticket.getIdentifier()).orElse(null);
                if (p == null || ! p.isOnline()) {
                    MessageUtils.logDebug("&cTPTicketFlusher&f: &dTPTicket &ffor &d" + ticket.getIdentifier() + " &fcould not find player, clearing it.");
                    ticket.clear();
                    return;
                }

                if (p.getServer().equals(targetServer)) {
                    ticket.unpend();
                    return;
                }

                ModuleUtils.connect(p, targetServer.getIdentifier());
                ticket.unpend();
                return;
            }

            if (! targetServer.equals(getOwnServer())) {
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
            if (p == null || ! p.isOnline()) return;

            Singularity.getInstance().getUserManager().teleport(p, ticket.toLocation());
            ticket.clear();
        });

        running.set(false);
    }

    public static CosmicServer getOwnServer() {
        return GivenConfigs.getServer().getCosmicServer();
    }
}
