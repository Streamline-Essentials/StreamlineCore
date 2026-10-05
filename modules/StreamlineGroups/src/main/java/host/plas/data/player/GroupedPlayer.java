package host.plas.data.player;

import host.plas.StreamlineGroups;
import host.plas.data.chats.ChatType;
import host.plas.database.PlayerLoader;
import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.loading.Loadable;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The per-player state this module owns: which group chat the player's normal chat is
 * routed into.
 *
 * <p>Persisted by {@code PlayerKeeper} and held in memory by {@link PlayerLoader}.</p>
 */
@Getter @Setter
public class GroupedPlayer implements Loadable<GroupedPlayer> {
    private String identifier;
    private boolean fullyLoaded;

    /**
     * Set while {@link #augment} waits on the stored record; a save in that window would
     * replace the stored chat type with the default, so it is held until the load ends.
     */
    private volatile boolean loadInFlight = false;
    private volatile boolean savePendingAfterLoad = false;

    private ChatType chatType;

    public String getUuid() {
        return getIdentifier();
    }

    public void setUuid(String uuid) {
        setIdentifier(uuid);
    }

    public GroupedPlayer(String uuid) {
        this.identifier = uuid;

        this.fullyLoaded = false;
        this.chatType = ChatType.NOT_SET;
    }

    /** Resolves this record's backing sender, or {@code null} if it cannot be found. */
    public CosmicSender asUser() {
        return UserUtils.getOrCreateSender(getUuid()).orElse(null);
    }

    @Override
    public void save(boolean async) {
        if (loadInFlight) {
            savePendingAfterLoad = true;
            return;
        }

        StreamlineGroups.getPlayerKeeper().save(this, async);
    }

    @Override
    public GroupedPlayer augment(CompletableFuture<Optional<GroupedPlayer>> loader, boolean isGet) {
        fullyLoaded = false;
        loadInFlight = true;

        loader.whenComplete((optional, throwable) -> {
            boolean saveNow = savePendingAfterLoad;

            if (throwable != null) {
                throwable.printStackTrace();
            } else if (optional.isPresent()) {
                // A chat type chosen before the load finished is newer than the stored one.
                if (this.chatType == ChatType.NOT_SET) this.chatType = optional.get().getChatType();
            } else if (! isGet) {
                // Nothing stored yet -- write the freshly created defaults so later loads
                // find a row.
                saveNow = true;
            }

            loadInFlight = false;
            savePendingAfterLoad = false;
            fullyLoaded = true;

            if (saveNow) save();
        });

        return this;
    }

    @Override
    public boolean isLoaded() {
        return PlayerLoader.getInstance().isLoaded(this);
    }

    @Override
    public void load() {
        PlayerLoader.getInstance().load(this);
    }

    @Override
    public void unload() {
        PlayerLoader.getInstance().unload(this);
    }
}
