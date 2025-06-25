package singularity.data.players.events;

import singularity.data.players.CosmicPlayer;

import java.util.Date;

public class CreatePlayerEvent extends CreateSenderEvent {
    public CreatePlayerEvent(CosmicPlayer player) {
        super(player);
    }

    public CosmicPlayer getPlayer() {
        return (CosmicPlayer) super.getSender();
    }

    public Date getCreationDate() {
        return getPlayer().getFirstJoinDate();
    }
}
