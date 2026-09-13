package host.plas.data;


import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;

@Getter @Setter
public class Party extends AbstractGroup {
    public Party(String uuid, CosmicSender owner, boolean load) {
        super(GroupType.PARTY, uuid, owner, load);
    }

    public Party(String uuid, CosmicSender owner) {
        super(GroupType.PARTY, uuid, owner);
    }

    public Party(CosmicSender owner, boolean load) {
        super(GroupType.PARTY, owner, load);
    }

    public Party(CosmicSender owner) {
        super(GroupType.PARTY, owner);
    }

    public Party(String uuid, boolean load) {
        super(GroupType.PARTY, uuid, load);
    }

    public Party(String uuid) {
        super(GroupType.PARTY, uuid);
    }

    @Override
    public void loadMore() {

    }

    @Override
    public void unloadMore() {

    }

    @Override
    public void populateDefaultsMore() {
        // Settings.
    }
}

