package singularity.loading;

import tv.quaint.objects.Identifiable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Loadable<L> extends Identifiable {
    void save();

    L augment(CompletableFuture<Optional<L>> loader);
}
