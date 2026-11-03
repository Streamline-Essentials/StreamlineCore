package gg.drak.tacoessentials.teleport;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.commands.Msg;
import gg.drak.tacoessentials.commands.Perms;
import gg.drak.tacoessentials.data.Loc;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/**
 * Player-initiated teleports (homes, warps, spawn, {@code /tpa}, {@code /rtp}, {@code /back})
 * with a stand-still countdown of {@code teleport.warmup-seconds}. Moving to another block
 * cancels it; {@link Perms#BYPASS_WARMUP} or a countdown of 0 teleports at once. Staff
 * teleports go through {@link Teleports} directly and never wait.
 *
 * <p>Streamline has no movement or damage event on every platform, so {@link #tick()} polls the
 * position of each waiting player instead, and taking damage does not cancel a countdown.</p>
 */
public final class Warmups {

    /** Where the player goes, read when the countdown ends so a moving target is followed. */
    public interface Destination {
        Loc resolve() throws Msg.Fail;
    }

    private static final class Pending {
        private final CosmicPlayer player;
        private final Loc start;
        private final long dueMillis;
        private final Destination destination;
        private final boolean recordBack;
        private final Consumer<Loc> onArrive;

        Pending(CosmicPlayer player, Loc start, long dueMillis, Destination destination, boolean recordBack, Consumer<Loc> onArrive) {
            this.player = player;
            this.start = start;
            this.dueMillis = dueMillis;
            this.destination = destination;
            this.recordBack = recordBack;
            this.onArrive = onArrive;
        }
    }

    private static final Map<String, Pending> PENDING = new ConcurrentHashMap<>();

    private Warmups() {}

    /**
     * Teleports the player after the countdown, replacing any countdown they already had.
     *
     * @param recordBack whether the spot left behind is recorded for {@code /back}
     * @param onArrive   runs with the destination once the player is there
     */
    public static void teleport(CosmicPlayer player, Destination destination, boolean recordBack, Consumer<Loc> onArrive) {
        String uuid = player.getUuid();
        cancel(uuid, false);
        int seconds = player.hasPermission(Perms.BYPASS_WARMUP) ? 0 : TacoEssentials.getConfig().warmupSeconds();
        Optional<CosmicLocation> here = gameplay().getLocation(uuid);
        if (seconds <= 0 || here.isEmpty()) {
            go(player, destination, recordBack, onArrive);
            return;
        }
        PENDING.put(uuid, new Pending(player, Loc.of(here.get()), System.currentTimeMillis() + seconds * 1000L,
                destination, recordBack, onArrive));
        player.sendMessage(Msg.info("Teleporting in " + seconds + " second" + (seconds == 1 ? "" : "s") + ". Don't move."));
    }

    /** Checks every waiting player; runs on Streamline's scheduler twice a second. */
    public static void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Pending>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Pending> entry = it.next();
            Pending pending = entry.getValue();
            Optional<CosmicLocation> here = gameplay().getLocation(entry.getKey());
            if (here.isEmpty()) {
                it.remove();
                continue;
            }
            if (! Loc.of(here.get()).sameBlock(pending.start)) {
                it.remove();
                pending.player.sendMessage(Msg.error("Teleport cancelled because you moved."));
                continue;
            }
            if (now < pending.dueMillis) continue;
            it.remove();
            go(pending.player, pending.destination, pending.recordBack, pending.onArrive);
        }
    }

    public static boolean isWaiting(String uuid) {
        return PENDING.containsKey(uuid);
    }

    /** Drops the player's countdown, telling them when {@code notify} is set. */
    public static void cancel(String uuid, boolean notify) {
        Pending pending = PENDING.remove(uuid);
        if (pending != null && notify) pending.player.sendMessage(Msg.error("Teleport cancelled."));
    }

    public static void clear() {
        PENDING.clear();
    }

    private static void go(CosmicPlayer player, Destination destination, boolean recordBack, Consumer<Loc> onArrive) {
        try {
            Loc to = destination.resolve();
            if (! Teleports.teleport(player.getUuid(), to, recordBack)) {
                throw Msg.fail("That location is in a world that no longer exists (" + to.getWorld() + ").");
            }
            if (onArrive != null) onArrive.accept(to);
        } catch (Msg.Fail fail) {
            player.sendMessage(Msg.error(fail.getMessage()));
        }
    }
}
