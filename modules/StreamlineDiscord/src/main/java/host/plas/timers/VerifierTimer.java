package host.plas.timers;

import host.plas.StreamlineDiscord;
import host.plas.config.VerifiedUsers;
import lombok.Getter;
import lombok.Setter;
import singularity.scheduler.ModuleRunnable;

import java.util.concurrent.atomic.AtomicBoolean;

public class VerifierTimer extends ModuleRunnable {
    @Getter @Setter
    private static AtomicBoolean running = new AtomicBoolean(false);

    public VerifierTimer() {
        super(StreamlineDiscord.getInstance(), 0L, 20L);
    }

    @Override
    public void run() {
        if (running.get()) return;
        running.set(true);

        VerifiedUsers.validateAllUsers();

        running.set(false);
    }
}
