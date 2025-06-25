package singularity.data.players.events;

import singularity.data.players.CosmicPlayer;

public class UnloadPlayerEvent extends UnloadSenderEvent {
    public UnloadPlayerEvent(CosmicPlayer player) {
        super(player);
    }

    public CosmicPlayer getPlayer() {
        return (CosmicPlayer) super.getSender();
    }
}
