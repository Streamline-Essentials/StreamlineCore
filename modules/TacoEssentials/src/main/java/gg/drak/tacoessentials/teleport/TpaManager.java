package gg.drak.tacoessentials.teleport;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.commands.Msg;
import singularity.data.players.CosmicPlayer;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Pending {@code /tpa} and {@code /tpahere} requests, held in memory only. A receiver answers
 * one by its sender's name ({@code /tpaccept <player>}) or, with no name, the newest one; a
 * sender withdraws theirs with {@code /tpacancel}. Synchronized: commands run on the server
 * thread, expiry on Streamline's scheduler. Players are messaged outside the lock.
 */
public final class TpaManager {

    public enum Kind {
        /** The sender travels to the target. */
        TO_TARGET,
        /** The target travels to the sender. */
        HERE
    }

    public static final class Request {
        private final String sender;
        private final String senderName;
        private final String target;
        private final String targetName;
        private final Kind kind;
        private final long createdMillis;

        Request(String sender, String senderName, String target, String targetName, Kind kind, long createdMillis) {
            this.sender = sender;
            this.senderName = senderName;
            this.target = target;
            this.targetName = targetName;
            this.kind = kind;
            this.createdMillis = createdMillis;
        }

        public String getSender() {
            return sender;
        }

        public String getSenderName() {
            return senderName;
        }

        public String getTarget() {
            return target;
        }

        public String getTargetName() {
            return targetName;
        }

        public Kind getKind() {
            return kind;
        }

        long expiresMillis() {
            return createdMillis + TacoEssentials.getConfig().tpaTimeoutSeconds() * 1000L;
        }
    }

    private static final List<Request> REQUESTS = new ArrayList<>();

    private TpaManager() {}

    /** Creates a request, replacing any earlier request from the same sender to the same target. */
    public static synchronized Request create(CosmicPlayer sender, CosmicPlayer target, Kind kind) {
        REQUESTS.removeIf(r -> r.sender.equals(sender.getUuid()) && r.target.equals(target.getUuid()));
        Request request = new Request(sender.getUuid(), sender.getCurrentName(), target.getUuid(), target.getCurrentName(),
                kind, System.currentTimeMillis());
        REQUESTS.add(request);
        return request;
    }

    /** Requests addressed to {@code target}, newest first. */
    public static synchronized List<Request> incoming(String target) {
        List<Request> out = new ArrayList<>();
        for (Request r : REQUESTS) if (r.target.equals(target)) out.add(r);
        out.sort(Comparator.comparingLong((Request r) -> r.createdMillis).reversed());
        return out;
    }

    /** Requests {@code sender} has made, newest first. */
    public static synchronized List<Request> outgoing(String sender) {
        List<Request> out = new ArrayList<>();
        for (Request r : REQUESTS) if (r.sender.equals(sender)) out.add(r);
        out.sort(Comparator.comparingLong((Request r) -> r.createdMillis).reversed());
        return out;
    }

    /**
     * The request to {@code target} from the player named {@code senderName}, or the newest one
     * when no name is given; {@code null} when there is none.
     */
    public static synchronized Request find(String target, String senderName) {
        for (Request r : incoming(target)) {
            if (senderName == null || r.senderName.equalsIgnoreCase(senderName)) return r;
        }
        return null;
    }

    /** @return whether the request was still pending */
    public static synchronized boolean remove(Request request) {
        return REQUESTS.remove(request);
    }

    /** Drops every request a disconnecting player sent or received, telling the other party. */
    public static void dropPlayer(String uuid) {
        List<Request> dropped = new ArrayList<>();
        synchronized (TpaManager.class) {
            Iterator<Request> it = REQUESTS.iterator();
            while (it.hasNext()) {
                Request r = it.next();
                if (! r.sender.equals(uuid) && ! r.target.equals(uuid)) continue;
                it.remove();
                dropped.add(r);
            }
        }
        for (Request r : dropped) {
            boolean senderLeft = r.sender.equals(uuid);
            tell(senderLeft ? r.target : r.sender, Msg.muted("The teleport request "
                    + (senderLeft ? "from " + r.senderName : "to " + r.targetName) + " was cancelled because they left."));
        }
    }

    public static void expire() {
        List<Request> expired = new ArrayList<>();
        long now = System.currentTimeMillis();
        synchronized (TpaManager.class) {
            Iterator<Request> it = REQUESTS.iterator();
            while (it.hasNext()) {
                Request r = it.next();
                if (now < r.expiresMillis()) continue;
                it.remove();
                expired.add(r);
            }
        }
        for (Request r : expired) {
            tell(r.sender, Msg.muted("Your teleport request to " + r.targetName + " expired."));
            tell(r.target, Msg.muted("The teleport request from " + r.senderName + " expired."));
        }
    }

    public static synchronized void clear() {
        REQUESTS.clear();
    }

    private static void tell(String uuid, String message) {
        UserUtils.getPlayer(uuid).filter(CosmicPlayer::isOnline).ifPresent(p -> p.sendMessage(message));
    }
}
