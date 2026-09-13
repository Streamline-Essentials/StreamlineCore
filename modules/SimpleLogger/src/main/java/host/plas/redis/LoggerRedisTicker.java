package host.plas.redis;

import singularity.scheduler.BaseRunnable;

public class LoggerRedisTicker extends BaseRunnable {
    public LoggerRedisTicker() {
        super(0, 20); // 1 second period
    }

    @Override
    public void run() {
        try {
            LoggerRedisManager.tickQueue();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
