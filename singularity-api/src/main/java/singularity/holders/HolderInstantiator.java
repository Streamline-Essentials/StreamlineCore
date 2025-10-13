package singularity.holders;

import java.util.function.Supplier;

public interface HolderInstantiator<T extends CosmicHolder> extends Supplier<T> {
}
