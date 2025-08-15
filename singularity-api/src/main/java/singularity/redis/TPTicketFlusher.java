package singularity.redis;

import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.players.CosmicPlayer;
import singularity.data.server.CosmicServer;
import singularity.data.teleportation.TPTicket;
import singularity.scheduler.BaseRunnable;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.concurrent.ConcurrentSkipListSet;

public class TPTicketFlusher extends BaseRunnable {
    public TPTicketFlusher() {
        super(0, 5);

        MessageUtils.logInfo("Registered &cTPTicket Flusher&r...");
    }

    @Override
    public void run() {
        ConcurrentSkipListSet<TPTicket> pending = TPTicket.getPendingTickets();
        if (pending.isEmpty()) return;

        pending.forEach(ticket -> {
            if (! ticket.getTargetServer().equals(getOwnServer())) {
                MessageUtils.logDebug("&cTPTicketFlusher&f: &dTPTicket &ffor &d" + ticket.getIdentifier() + " &fis not for this server, clearing it.");
                ticket.clear();
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
    }

    public static CosmicServer getOwnServer() {
        return GivenConfigs.getServer().getCosmicServer();
    }
}
