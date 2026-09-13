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
    /**
     * Set while {@link #hydrated(String, CosmicSender)} is building a guild from a query
     * result, so that the constructor does not fetch it back out of the database.
     */
    private static final ThreadLocal<Boolean> hydrating = ThreadLocal.withInitial(() -> false);

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
        // Guilds built by the keeper itself are already being populated from a result set;
        // re-entering the keeper here would recurse.
        if (hydrating.get()) return;

        augment(StreamlineGroups.getGuildKeeper().load(getUuid()), true);
    }

    /**
     * Builds a guild without triggering a database fetch, for use by
     * {@code GuildKeeper} while it populates one from a query result.
     */
    public static Guild hydrated(String uuid, CosmicSender owner) {
        hydrating.set(true);
        try {
            return owner == null ? new Guild(uuid, false) : new Guild(uuid, owner, false);
        } finally {
            hydrating.set(false);
        }
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
                setMaxSize(stored.getMaxSize());
                setCreateDate(stored.getCreateDate());
                updateOwner(stored.getOwner());
            } else {
                // Nothing stored yet -- persist the defaults so later loads find a row.
                if (! isGet) save();
            }

            fullyLoaded = true;
        });

        return this;
    }
}

