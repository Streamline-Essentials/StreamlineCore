package net.streamline.api.loading;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.database.CoreDBOperator;
import net.streamline.api.database.modules.DBKeeper;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public abstract class Loader<L extends Loadable> {
    public ConcurrentSkipListSet<L> loaded;
    
    public Loader() {
        loaded = new ConcurrentSkipListSet<>();
    }
    
    public abstract DBKeeper<L> getKeeper();
    
    public static CoreDBOperator getOperator() {
        return SLAPI.getMainDatabase();
    }
    
    public CompletableFuture<Boolean> userExists(String identifier) {
        return getKeeper().exists(identifier);
    }
    
    public abstract L getConsole();

    public Optional<L> get(String identifier) {
        if (identifier == null) return Optional.empty();
        if (identifier.equals(StreamSender.getConsoleDiscriminator())) return Optional.of(getConsole());

        return getLoaded().stream().filter(a -> a.getIdentifier().equals(identifier)).findFirst();
    }
    
    public L load(L toLoad) {
        if (isLoaded(toLoad.getIdentifier())) return get(toLoad.getIdentifier()).get();
        
        getLoaded().add(toLoad);
        
        fireLoadEvents(toLoad);
        
        return toLoad;
    }
    
    public abstract void fireLoadEvents(L loaded);

    public abstract L instantiate(String identifier);
    
    public L createNew(String identifier) {
        L created = instantiate(identifier);
        created.save();

        fireCreateEvents(created);

        return load(created);
    }

    public abstract void fireCreateEvents(L created);

    public CompletableFuture<L> getOrCreateAsync(String identifier) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<L> optional = getKeeper().load(identifier).join();
            if (optional.isPresent()) {
                return load(optional.get());
            } else {
                return createNew(identifier);
            }
        });
    }

    public boolean isLoaded(String identifier) {
        return get(identifier).isPresent();
    }

    public Optional<L> getOrLoad(String identifier) {
        CompletableFuture.runAsync(() -> {
            if (isLoaded(identifier)) return;

            L created = getOrCreateAsync(identifier).join();
            load(created);
        });

        return get(identifier);
    }

    public L getOrCreate(L sender) {
        return getOrCreate(sender.getIdentifier());
    }

    public L getOrCreate(String identifier) {
        Optional<L> optional = getOrLoad(identifier);
        if (optional.isPresent()) return optional.get();

        if (identifier.equals(StreamSender.getConsoleDiscriminator())) {
            return getConsole();
        }

        CompletableFuture<L> loader = getOrCreateAsync(identifier);

        return augment(instantiate(identifier));
    }

    public abstract L augment(L sender);

    public void unload(String identifier) {
        Optional<L> optional = get(identifier);
        if (optional.isEmpty()) return;
        L loadable = optional.get();

        loadable.save();
        getLoaded().remove(loadable);
    }

    public void unload(L loadable) {
        unload(loadable.getIdentifier());
    }
}
