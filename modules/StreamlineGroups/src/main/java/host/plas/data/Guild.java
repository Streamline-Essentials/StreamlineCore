package host.plas.data;


import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.loading.Loadable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Getter @Setter
public class Guild extends AbstractGroup implements Loadable<Guild> {
    private boolean loaded = false;
    private boolean fullyLoaded = false;

    public Guild(String uuid, CosmicSender owner, boolean load) {
        super(GroupType.GUILD, uuid, owner, load);
    }

    public Guild(String uuid, CosmicSender owner) {
        super(GroupType.GUILD, uuid, owner);
    }

    public Guild(CosmicSender owner, boolean load) {
        super(GroupType.GUILD, owner, load);
    }

    public Guild(CosmicSender owner) {
        super(GroupType.GUILD, owner);
    }

    public Guild(String uuid, boolean load) {
        super(GroupType.GUILD, uuid, load);
    }

    public Guild(String uuid) {
        super(GroupType.GUILD, uuid);
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

    @Override
    public void grabFromDatabase() {
        // Load from database...
    }

    @Override
    public void save() {

    }

    @Override
    public void save(boolean async) {

    }

    @Override
    public Guild augment(CompletableFuture<Optional<Guild>> loader, boolean isGet) {
        fullyLoaded = false;

        loader.whenComplete((guild, throwable) -> {
            if (throwable != null) {
                // Handle error...
                fullyLoaded = true;
                return;
            }

            if (guild.isPresent()) {
                // do augmentation...
            } else {
                if (isGet) {
                    save();
                }
            }

            fullyLoaded = true;
        });

        return this;
    }
}

