package host.plas.data.player;

import host.plas.data.chats.ChatType;
import lombok.Getter;
import lombok.Setter;
import singularity.loading.Loadable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Getter @Setter
public class GroupedPlayer implements Loadable<GroupedPlayer> {
    private String identifier;
    private boolean loaded;
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

        this.loaded = false;
        this.fullyLoaded = false;
        this.chatType = ChatType.NOT_SET;
    }

    @Override
    public void save(boolean b) {

    }

    @Override
    public GroupedPlayer augment(CompletableFuture<Optional<GroupedPlayer>> completableFuture, boolean b) {
        return null;
    }

    @Override
    public void unload() {

    }

    @Override
    public void load() {

    }
}
