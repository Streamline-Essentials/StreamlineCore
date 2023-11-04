package net.streamline.api.registries;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class AbstractRegistry<I, T> implements Identifiable {
    @Setter
    private String identifier;
    private final Class<I> type;
    private final boolean isMaster;

    @Setter
    private List<I> registry = new ArrayList<>();

    public AbstractRegistry(String identifier, Class<I> type, boolean isMaster) {
        this.identifier = identifier;
        this.type = type;
        this.isMaster = isMaster;

        if (! isMaster) {
            load();
        }
    }

    public AbstractRegistry(String identifier, Class<I> type) {
        this(identifier, type, false);
    }

    public AbstractRegistry(RegistryKeys registryKey) {
        this(registryKey.name(), (Class<I>) registryKey.getType(), registryKey.isMaster());
    }

    public void register(I object) {
        registry.add(object);
    }

    public void unregister(I object) {
        registry.remove(object);
    }

    public abstract ItemGetter<T, AbstractRegistry<I, T>, I> getGetter();

    public I get(T thing) {
        return getGetter().get(thing, this);
    }

    public boolean isRegistered(T thing) {
        return get(thing) != null;
    }

    public void load() {
        MasterRegistry master = MasterRegistry.getInstance();
        if (! isMaster) {
            master.register(this);
        }
    }

    public void unload() {
        MasterRegistry master = MasterRegistry.getInstance();
        if (! isMaster) {
            master.unregister(this);
        }
    }
}
