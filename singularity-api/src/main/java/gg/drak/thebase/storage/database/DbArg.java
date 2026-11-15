package gg.drak.thebase.storage.database;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

@Getter @Setter
public class DbArg {
    /**
     * The start index of the argument
     */
    private int start;
    /**
     * The current argument index
     */
    private AtomicInteger i;

    /**
     * Constructor with specified start index
     * Note: when calling {@link #next()}, {@link #next()} is incremented and then returned, so it will be 1 more than the start value when being called.
     * @param start The start index of the argument
     */
    public DbArg(int start) {
        this.start = start;
        this.i = new AtomicInteger(start);
    }

    /**
     * Constructor with default start index of 0
     * Note: when calling {@link #next()}, {@link #next()} is incremented and then returned, so it will be 1 more than the start value when being called.
     */
    public DbArg() {
        this(0);
    }

    /**
     * Increment and get the next argument index
     * @return The next argument index
     */
    public int next() {
        return i.incrementAndGet();
    }

    /**
     * Set the argument index to the specified value
     * @param i The new argument index
     */
    public void set(int i) {
        this.i.set(i);
    }

    /**
     * Reset the argument index to the start value
     */
    public void reset() {
        this.set(this.start);
    }
}
