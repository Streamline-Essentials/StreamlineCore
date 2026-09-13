package host.plas.data;


import host.plas.StreamlineGroups;
import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.loading.Loadable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A persistent group. Unlike a {@link Party}, a guild outlives the session its members
 * are online for and is stored by {@code GuildKeeper}.
 */
@Getter @Setter
public class Guild extends AbstractGroup implements Loadable<Guild> {
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

    /**
     * Pulls this guild's stored state in the background, leaving the in-memory defaults in
     * place until the load resolves.
     */
    @Override
    public void grabFromDatabase() {
        if (StreamlineGroups.getGuildKeeper() == null) return;

        augment(StreamlineGroups.getGuildKeeper().load(getUuid()), true);
    }

    /**
     * Builds a guild whose fields the caller fills in from storage, without a database
     * fetch of its own. Used by {@code GuildKeeper} while reading a query result.
     */
    public static Guild hydrated(String uuid, CosmicSender owner) {
        return new Guild(uuid, owner, false, true);
    }

    private Guild(String uuid, CosmicSender owner, boolean load, boolean hydrating) {
        super(GroupType.GUILD, uuid, owner, load, hydrating);
    }

    @Override
    public void save(boolean async) {
        StreamlineGroups.getGuildKeeper().save(this, async);
    }

    @Override
    public Guild augment(CompletableFuture<Optional<Guild>> loader, boolean isGet) {
        fullyLoaded = false;

        loader.whenComplete((guild, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                fullyLoaded = true;
                return;
            }

            if (guild.isPresent()) {
                Guild stored = guild.get();

                setMuted(stored.isMuted());
                setPublic(stored.isPublic());
                setCreateDate(stored.getCreateDate());
                // The owner is adopted first because updateOwner re-derives the size cap
                // from their permissions; the stored cap is applied over that.
                updateOwner(stored.getOwner());
                setMaxSize(stored.getMaxSize());
            } else {
                // Nothing stored yet -- persist the defaults so later loads find a row.
                if (! isGet) save();
            }

            fullyLoaded = true;
        });

        return this;
    }
}

