package net.streamline.api.registries;

import lombok.Getter;
import net.streamline.api.savables.SavableResource;

@Getter
public enum RegistryKeys {
    MASTER(AbstractRegistry.class, true),
    SAVABLES(SavableResource.class),
    ;

    private final Class<? extends Identifiable> type;
    private final boolean isMaster;

    RegistryKeys(Class<? extends Identifiable> type, boolean isMaster) {
        this.type = type;
        this.isMaster = isMaster;
    }

    RegistryKeys(Class<? extends Identifiable> type) {
        this(type, false);
    }

    public <R extends AbstractRegistry<?, ?>> R getRegistry() {
        return (R) MasterRegistry.getInstance().get(name());
    }
}
