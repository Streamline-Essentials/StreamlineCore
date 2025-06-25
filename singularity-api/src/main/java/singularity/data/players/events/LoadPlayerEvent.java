package singularity.data.players.events;

import singularity.data.players.CosmicPlayer;

public class LoadPlayerEvent extends LoadSenderEvent {
    public LoadPlayerEvent(CosmicPlayer player) {
        super(player);
    }

    public CosmicPlayer getPlayer() {
        return (CosmicPlayer) super.getSender();
    }
}
