package host.plas.collections.timers;

import host.plas.collections.StreamlineCollections;
import singularity.data.players.CosmicPlayer;
import singularity.scheduler.ModuleRunnable;
import singularity.utils.UserUtils;

import java.util.function.Consumer;

/**
 * Reminds online players who have levels waiting to be claimed, through {@code remind}.
 */
public class RemindTimer extends ModuleRunnable {
    private final Consumer<CosmicPlayer> remind;

    public RemindTimer(int minutes, Consumer<CosmicPlayer> remind) {
        super(StreamlineCollections.getInstance(), minutes * 60L * 20L, minutes * 60L * 20L);
        this.remind = remind;
    }

    @Override
    public void run() {
        for (CosmicPlayer player : UserUtils.getOnlinePlayers().values()) remind.accept(player);
    }
}
