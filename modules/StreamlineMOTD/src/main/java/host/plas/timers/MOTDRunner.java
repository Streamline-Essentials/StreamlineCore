package host.plas.timers;

import gg.drak.thebase.async.AsyncUtils;
import host.plas.data.UpdateType;
import lombok.Getter;
import lombok.Setter;
import singularity.scheduler.ModuleRunnable;
import host.plas.StreamlineMOTD;

@Setter @Getter
public class MOTDRunner extends ModuleRunnable {
    public static final long OTHER_TICKS_INTERVAL = 20;

    long runningTicks = 0;
    long motdTicks = 0;
    long sampleTicks = 0;
    long otherTicks = 0;

    public MOTDRunner() {
        super(StreamlineMOTD.getInstance(), 0, getTicks());

        setRunningTicks(0);
    }

    public static long getTicks() {
        long ticks = StreamlineMOTD.getConfig().getUpdateTicks();

        if (ticks <= 0) {
            ticks = 1;
        }

        return ticks;
    }

    @Override
    public void run() {
        if (getPeriod() != getTicks()) {
            setPeriod(getTicks());
        }

        AsyncUtils.executeAsync(() -> {
            StreamlineMOTD.getConfig().updatePlayers(); // Update player counts.

            if (otherTicks >= OTHER_TICKS_INTERVAL) {
                StreamlineMOTD.getConfig().updateVersion();

                StreamlineMOTD.getConfig().updateFavicon();

                setOtherTicks(0);
            }

            computeConfig();
        });

        add();
    }

    public void add() {
        runningTicks ++;
        motdTicks ++;
        sampleTicks ++;
        otherTicks ++;
    }

    public void computeConfig() {
        if (StreamlineMOTD.getConfig().getUpdateType() != UpdateType.ON_TICK) return;

        computeUpdateMotd();
        computeUpdateSample();
    }

    public void computeUpdateMotd() {
        if (StreamlineMOTD.getConfig().getMotdTicks() == -1) return;

        if (getMotdTicks() >= StreamlineMOTD.getConfig().getMotdTicks()) {
            StreamlineMOTD.getConfig().updateMotd();
            setMotdTicks(0);
        }
    }

    public void computeUpdateSample() {
        if (StreamlineMOTD.getConfig().getSampleTicks() == -1) return;

        if (getSampleTicks() >= StreamlineMOTD.getConfig().getSampleTicks()) {
            StreamlineMOTD.getConfig().updateSample();
            setSampleTicks(0);
        }
    }
}
