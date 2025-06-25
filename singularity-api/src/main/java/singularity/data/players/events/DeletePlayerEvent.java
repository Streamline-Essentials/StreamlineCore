package singularity.data.players.events;

import singularity.data.players.CosmicPlayer;

public class DeletePlayerEvent extends DeleteSenderEvent {
    public DeletePlayerEvent(CosmicPlayer player) {
        super(player);
    }

    public CosmicPlayer getPlayer() {
        return (CosmicPlayer) super.getSender();
    }
}
