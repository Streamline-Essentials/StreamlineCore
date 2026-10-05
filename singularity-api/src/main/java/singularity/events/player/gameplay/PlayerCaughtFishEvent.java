package singularity.events.player.gameplay;

import lombok.Getter;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicItem;

import java.util.ArrayList;
import java.util.List;

/**
 * A player reeled in a catch with a fishing rod. {@link #getCaught()} holds the caught items with
 * their ids and amounts.
 */
@Getter
public class PlayerCaughtFishEvent extends PlayerGameplayEvent {
    private final List<CosmicItem> caught;

    public PlayerCaughtFishEvent(CosmicPlayer player, String gameMode, List<CosmicItem> caught) {
        super(player, gameMode);
        this.caught = caught == null ? new ArrayList<>() : caught;
    }
}
