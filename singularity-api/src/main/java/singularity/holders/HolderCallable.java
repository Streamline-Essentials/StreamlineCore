package singularity.holders;

import lombok.Getter;

import java.util.concurrent.Callable;

/**
 * A named wrapper around a {@link Callable}{@code <Boolean>} for use within the holder system.
 *
 * <p>Intended to carry an initialisation or validation callable that returns a success flag.
 * The {@link #call()} implementation currently returns {@code null} — subclasses or future
 * revisions should delegate to the wrapped {@link #callable} as needed.</p>
 */
@Getter
public class HolderCallable implements Callable<Boolean> {

    /**
     * The underlying callable wrapped by this holder.
     */
    private final Callable<Boolean> callable;

    /**
     * Constructs a new {@code HolderCallable} wrapping the supplied callable.
     *
     * @param callable the callable to wrap; must not be {@code null}
     */
    public HolderCallable(Callable<Boolean> callable) {
        this.callable = callable;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Currently returns {@code null}. Callers should invoke {@link #getCallable()} directly
     * if immediate execution of the wrapped callable is required.</p>
     *
     * @return {@code null}
     * @throws Exception if the underlying callable throws during execution
     */
    @Override
    public Boolean call() throws Exception {
        return null;
    }
}
