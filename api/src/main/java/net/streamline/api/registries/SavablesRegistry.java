package net.streamline.api.registries;

import lombok.Getter;
import lombok.Setter;
import tv.quaint.savables.SavableResource;

public class SavablesRegistry extends AbstractRegistry<SavableResource, SavablesRegistry.SavableIdentifier> {
    @Setter
    @Getter
    public static class SavableIdentifier {
        private String identifier;
        private Class<? extends SavableResource> type;

        public SavableIdentifier(String identifier, Class<? extends SavableResource> type) {
            this.identifier = identifier;
            this.type = type;
        }
    }

    public SavablesRegistry() {
        super(RegistryKeys.SAVABLES);
    }

    @Override
    public ItemGetter<SavableIdentifier, AbstractRegistry<SavableResource, SavableIdentifier>, SavableResource> getGetter() {
        return (thing, abstractRegistry) ->
                abstractRegistry.getRegistry().stream()
                        .filter(r ->
                                r.getIdentifier().equals(thing.getIdentifier()) &&
                                        r.getClass().equals(thing.getType())
                        ).findFirst().orElse(null);
    }
}
