package singularity.scheduler;

public abstract class BaseDelayedRunnable extends BaseRunnable {
    public BaseDelayedRunnable(long delay) {
        super(delay, 0);
    }

    @Override
    public void run() {
        runDelayed();

        this.cancel();
    }

    public abstract void runDelayed();
}
