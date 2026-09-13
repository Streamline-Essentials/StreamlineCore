package host.plas.events;

import gg.drak.thebase.events.BaseEventHandler;
import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.StreamersUnite;
import host.plas.data.LiveManager;

import singularity.data.players.CosmicPlayer;
import singularity.events.server.LogoutEvent;

public class MainListener implements BaseEventListener {
    public MainListener() {
        BaseEventHandler.bake(this, StreamersUnite.getInstance());

        StreamersUnite.getInstance().logInfo("Registered MainListener!");
    }

    @BaseProcessor
    public void onPlayerLogout(LogoutEvent event) {
        CosmicPlayer player = event.getPlayer();

        if (StreamersUnite.getStreamerConfig().getSetup(player.getUuid()).isPresent()) {
            if (LiveManager.isLive(player)) {
                LiveManager.goOffline(player);
            }
        }
    }
}
