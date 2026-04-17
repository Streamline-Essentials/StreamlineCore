package singularity.holders;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract lifecycle manager for a {@link CosmicHolder} integration.
 *
 * <p>On construction, this class registers itself in {@link HoldersHolder}, calls the
 * subclass-provided {@link #onLoad()} hook, and then attempts to instantiate the holder via the
 * supplied {@link HolderInstantiator}.  If instantiation fails or returns {@code null},
 * {@link #enabled} is set to {@code false} and {@link #holder} is set to {@code null}.</p>
 *
 * @param <T> the concrete {@link CosmicHolder} type managed by this initialiser
 */
@Getter @Setter
public abstract class HolderInit<T extends CosmicHolder> implements Identifiable {

    /**
     * Unique identifier for this holder initialiser, used to register and look up entries
     * in {@link HoldersHolder}.
     */
    private String identifier;

    /**
     * Whether the holder was successfully instantiated and is currently active.
     * {@code false} if instantiation failed or produced a {@code null} result.
     */
    private boolean enabled;

    /**
     * The live holder instance, or {@code null} if not yet successfully initialised.
     */
    private T holder;

    /**
     * Constructs a new {@code HolderInit}, registers it with {@link HoldersHolder},
     * invokes {@link #onLoad()}, and attempts to enable the holder using the provided
     * instantiator.
     *
     * @param identifier   a unique string used to identify this holder within {@link HoldersHolder}
     * @param instantiator a factory that supplies the concrete holder instance
     */
    public HolderInit(String identifier, HolderInstantiator<T> instantiator) {
        this.identifier = identifier;
        this.enabled = false;

        load();

        tryEnable(instantiator);
    }

    /**
     * Attempts to obtain a holder instance from the given instantiator.
     *
     * <p>If the instantiator returns a non-{@code null} value, {@link #enabled} is set to
     * {@code true}. Any {@link Throwable} thrown during instantiation causes {@link #enabled}
     * to be set to {@code false} and {@link #holder} to be cleared.</p>
     *
     * @param instantiator the factory used to create the holder instance
     */
    public void tryEnable(HolderInstantiator<T> instantiator) {
        try {
            this.holder = instantiator.get();
            this.enabled = this.holder != null;
        } catch (Throwable e) {
            this.enabled = false;
            this.holder = null;
        }
    }

    /**
     * Registers this initialiser in {@link HoldersHolder} (replacing any prior registration
     * under the same identifier) and then delegates to {@link #onLoad()}.
     */
    public void load() {
        HoldersHolder.load(this);

        onLoad();
    }

    /**
     * Called during {@link #load()} after this initialiser is registered in
     * {@link HoldersHolder}.  Subclasses should perform any pre-instantiation setup here
     * (e.g., checking for required plugins, reading configuration).
     */
    public abstract void onLoad();
}
