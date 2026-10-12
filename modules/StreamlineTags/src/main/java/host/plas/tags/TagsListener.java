package host.plas.tags;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.tags.data.TagStore;
import singularity.data.players.CosmicPlayer;
import singularity.events.server.LoginCompletedEvent;
import singularity.events.server.LogoutEvent;
import singularity.modules.ModuleUtils;

import java.util.concurrent.CompletableFuture;

/**
 * Caches a player's grants and active tag while they are online.
 */
public class TagsListener implements BaseEventListener {

    public TagsListener() {
        ModuleUtils.listen(this, StreamlineTags.getInstance());
    }

    @BaseProcessor
    public void onJoin(LoginCompletedEvent event) {
        CosmicPlayer player = event.getPlayer();
        if (player == null) return;
        String uuid = player.getUuid();
        CompletableFuture.runAsync(() -> TagStore.loadPlayer(uuid)).exceptionally(error -> {
            StreamlineTags.getInstance().logWarning("Could not load tags for " + uuid + ": " + error.getMessage());
            return null;
        });
    }

    @BaseProcessor
    public void onQuit(LogoutEvent event) {
        CosmicPlayer player = event.getPlayer();
        if (player == null) return;
        TagStore.unloadPlayer(player.getUuid());
    }
}
