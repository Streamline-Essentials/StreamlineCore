package gg.drak.tacoessentials.teleport;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.data.Loc;
import gg.drak.tacoessentials.data.Sessions;
import gg.drak.tacoessentials.data.TacoDatabase;
import singularity.Singularity;
import singularity.interfaces.IGameplayHandler;

/**
 * The single path every TacoEssentials teleport goes through, so the previous position is
 * recorded for {@code /back} exactly once.
 */
public final class Teleports {

    private Teleports() {}

    /** The platform's gameplay handler; TacoEssentials registers no commands without one. */
    public static IGameplayHandler gameplay() {
        return Singularity.getGameplayHandler();
    }

    /**
     * @return {@code false} if the player is offline or the location's world no longer exists
     */
    public static boolean teleport(String uuid, Loc to, boolean recordBack) {
        if (! gameplay().getWorldNames().contains(to.getWorld())) return false;
        if (recordBack) recordBack(uuid);
        boolean moved = gameplay().teleport(uuid, to.toCosmic());
        // A frozen player is held where staff put them, not pulled back to the old spot.
        if (moved && Sessions.isFrozen(uuid)) Sessions.get(uuid).setFrozenAt(to);
        return moved;
    }

    /** Remembers where the player stands now for {@code /back}. */
    public static void recordBack(String uuid) {
        gameplay().getLocation(uuid).ifPresent(here -> recordBack(uuid, Loc.of(here)));
    }

    public static void recordBack(String uuid, Loc loc) {
        TacoDatabase.push(uuid, TacoDatabase.BACK, loc, TacoEssentials.getConfig().backHistorySize());
    }
}
