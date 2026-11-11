package gg.drak.thebase.async;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TaskThread extends Thread {
    @Getter @Setter
    public static AtomicLong lastTick = new AtomicLong(System.currentTimeMillis());
    @Getter @Setter
    public static AtomicBoolean run = new AtomicBoolean(false);
    @Getter @Setter
    public static AtomicBoolean running = new AtomicBoolean(true);
    @Getter @Setter
    public static AtomicLong tickingFrequency = new AtomicLong(50);

    public TaskThread() {
        super(getRunTask());
    }

    public void pauseTask() {
        running.set(false);
    }

    public void resumeTask() {
        running.set(true);
    }

    public void stopTask() {
        pauseTask();
        run.set(false);

        try {
            this.interrupt();
        } catch (Exception e) {
//            System.out.println("An error occurred while stopping the task thread: " + e.getMessage());
//            e.printStackTrace();

            // Ignore
        }
    }

    public void startTask() {
        run.set(true);
        resumeTask();

        this.start();
    }

    public void updateTickingFrequency(long frequency) {
        tickingFrequency.set(frequency);
    }

    public static Runnable getRunTask() {
        return () -> {
            try {
                while (true) {
                    if (! run.get()) return;

                    try {
                        tick();
                    } catch (Throwable t) {
                        System.out.println("An error occurred while executing the task thread: " + t.getMessage());
                        t.printStackTrace();
                    }

                    try {
                        Thread.sleep(tickingFrequency.get());
                    } catch (Throwable t) {
                        System.out.println("An error occurred while sleeping the task thread: " + t.getMessage());
                        t.printStackTrace();
                    }
                }
            } catch (Throwable t) {
                System.out.println("An error occurred while executing the task thread: " + t.getMessage());
                t.printStackTrace();
            }
        };
    }

    public static void tick() {
        try {
            if (! running.get()) {
                return;
            }

            AsyncUtils.tickTasks();
            lastTick.set(System.currentTimeMillis());
        } catch (Throwable t) {
            System.out.println("An error occurred while executing a task: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
