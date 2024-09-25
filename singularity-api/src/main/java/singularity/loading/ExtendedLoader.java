package singularity.loading;

import java.util.Optional;

public abstract class ExtendedLoader<L extends Loadable<L>, E extends L> extends Loader<L> {
    public Optional<E> getExtended(String identifier) {
        Optional<L> optional = get(identifier);
        if (optional.isEmpty()) return Optional.empty();
        L loadable = optional.get();

        try {
            return Optional.of((E) loadable);
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    public abstract E instantiateExtended(String identifier);

    public E createNewExtended(String identifier) {
        E created = instantiateExtended(identifier);
        created.save();

        fireCreateEvents(created);

        return (E) load(created);
    }
}
