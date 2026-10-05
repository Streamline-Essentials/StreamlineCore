package gg.drak.tacoessentials.teleport;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.commands.Msg;
import singularity.data.players.CosmicPlayer;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pending {@code /tpa} and {@code /tpahere} requests, held in memory only. Each request gets a
 * small numeric id that the target passes to {@code /tpaccept} or {@code /tpadeny}.
 * Synchronized: commands run on the server thread, expiry on Streamline's scheduler.
 */
public final class TpaManager {

    public enum Kind {
        /** The sender travels to the target. */
        TO_TARGET,
        /** The target travels to the sender. */
        HERE
    }

    public static final class Request {
        private final int id;
        private final String sender;
        private final String target;
        private final Kind kind;
        private final long createdMillis;

        Request(int id, String sender, String target, Kind kind, long createdMillis) {
            this.id = id;
            this.sender = sender;
            this.target = target;
            this.kind = kind;
            this.createdMillis = createdMillis;
        }

        public int getId() {
            return id;
        }

        public String getSender() {
            return sender;
        }

        public String getTarget() {
            return target;
        }

        public Kind getKind() {
            return kind;
        }

        long expiresMillis() {
            return createdMillis + TacoEssentials.getConfig().tpaTimeoutSeconds() * 1000L;
        }
    }

    private static final Map<Integer, Request> REQUESTS = new LinkedHashMap<>();
    private static int nextId = 1;

    private TpaManager() {}

    /** Creates a request, replacing any earlier request from the same sender to the same target. */
    public static synchronized Request create(String sender, String target, Kind kind) {
        REQUESTS.values().removeIf(r -> r.sender.equals(sender) && r.target.equals(target));
        int id;
        do {
            id = nextId;
            nextId = nextId >= 9999 ? 1 : nextId + 1;
        } while (REQUESTS.containsKey(id));
        Request request = new Request(id, sender, target, kind, System.currentTimeMillis());
        REQUESTS.put(id, request);
        return request;
    }

    /** Requests addressed to {@code target}, newest first. */
    public static synchronized List<Request> incoming(String target) {
        List<Request> out = new ArrayList<>();
        for (Request r : REQUESTS.values()) if (r.target.equals(target)) out.add(r);
        out.sort(Comparator.comparingLong((Request r) -> r.createdMillis).reversed());
        return out;
    }

    public static synchronized Request get(int id) {
        return REQUESTS.get(id);
    }

    public static synchronized void remove(int id) {
        REQUESTS.remove(id);
    }

    /** Drops every request a disconnecting player sent or received, telling the other party. */
    public static void dropPlayer(String uuid) {
        List<Request> dropped = new ArrayList<>();
        synchronized (TpaManager.class) {
            Iterator<Request> it = REQUESTS.values().iterator();
            while (it.hasNext()) {
                Request r = it.next();
                if (! r.sender.equals(uuid) && ! r.target.equals(uuid)) continue;
                it.remove();
                dropped.add(r);
            }
        }
        for (Request r : dropped) {
            String other = r.sender.equals(uuid) ? r.target : r.sender;
            tell(other, Msg.muted("Teleport request #" + r.id + " was cancelled because the other player left."));
        }
    }

    public static void expire() {
        List<Request> expired = new ArrayList<>();
        long now = System.currentTimeMillis();
        synchronized (TpaManager.class) {
            Iterator<Request> it = REQUESTS.values().iterator();
            while (it.hasNext()) {
                Request r = it.next();
                if (now < r.expiresMillis()) continue;
                it.remove();
                expired.add(r);
            }
        }
        for (Request r : expired) {
            tell(r.sender, Msg.muted("Your teleport request #" + r.id + " expired."));
            tell(r.target, Msg.muted("Teleport request #" + r.id + " expired."));
        }
    }

    public static synchronized void clear() {
        REQUESTS.clear();
        nextId = 1;
    }

    private static void tell(String uuid, String message) {
        UserUtils.getPlayer(uuid).filter(CosmicPlayer::isOnline).ifPresent(p -> p.sendMessage(message));
    }
}
