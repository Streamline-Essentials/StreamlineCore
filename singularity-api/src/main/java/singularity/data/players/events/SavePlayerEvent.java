package singularity.data.players.events;

import singularity.data.players.CosmicPlayer;

public class SavePlayerEvent extends SaveSenderEvent {
    public SavePlayerEvent(CosmicPlayer player) {
        super(player);
    }

    public CosmicPlayer getPlayer() {
        return (CosmicPlayer) super.getSender();
    }
}
