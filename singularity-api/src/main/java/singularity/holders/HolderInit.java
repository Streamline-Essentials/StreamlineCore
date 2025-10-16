package singularity.holders;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class HolderInit<T extends CosmicHolder> implements Identifiable {
    private String identifier;
    private boolean enabled;
    private T holder;

    public HolderInit(String identifier, HolderInstantiator<T> instantiator) {
        this.identifier = identifier;
        this.enabled = false;

        load();

        tryEnable(instantiator);
    }

    public void tryEnable(HolderInstantiator<T> instantiator) {
        try {
            this.holder = instantiator.get();
            this.enabled = this.holder != null;
        } catch (Throwable e) {
            this.enabled = false;
            this.holder = null;
        }
    }

    public void load() {
        HoldersHolder.load(this);

        onLoad();
    }

    public abstract void onLoad();
}
