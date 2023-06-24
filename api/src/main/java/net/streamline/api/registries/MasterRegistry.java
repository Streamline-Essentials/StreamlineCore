package net.streamline.api.registries;

public class MasterRegistry extends AbstractRegistry<AbstractRegistry<?, ?>, String> {
    private static MasterRegistry INSTANCE;

    public static MasterRegistry getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MasterRegistry();
        }
        return INSTANCE;
    }

    public MasterRegistry() {
        super(RegistryKeys.MASTER);
    }

    @Override
    public AbstractRegistry<?, String> get(String identifier) {
        return null;
    }

    @Override
    public ItemGetter<String, AbstractRegistry<AbstractRegistry<?, ?>, String>, AbstractRegistry<?, ?>> getGetter() {
        return (thing, abstractRegistry) -> abstractRegistry.getRegistry().stream().filter(r -> r.getIdentifier().equals(thing)).findFirst().orElse(null);
    }
}
