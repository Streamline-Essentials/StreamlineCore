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
        StreamlineGroups.getPlayerKeeper().save(this, async);
    }

    @Override
    public GroupedPlayer augment(CompletableFuture<Optional<GroupedPlayer>> loader, boolean isGet) {
        fullyLoaded = false;

        loader.whenComplete((optional, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                fullyLoaded = true;
                return;
            }

            if (optional.isPresent()) {
                this.chatType = optional.get().getChatType();
            } else {
                // Nothing stored yet -- write the freshly created defaults so later loads
                // find a row.
                if (! isGet) save();
            }

            fullyLoaded = true;
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
