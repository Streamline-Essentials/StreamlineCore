package singularity.events.player.gameplay;

import lombok.Getter;
import singularity.data.players.CosmicPlayer;

/**
 * A player filled a bucket. {@link #getFilledItem()} is the id of the filled bucket, such as
 * {@code minecraft:water_bucket}.
 */
@Getter
public class PlayerFilledBucketEvent extends PlayerGameplayEvent {
    private final String filledItem;

    public PlayerFilledBucketEvent(CosmicPlayer player, String gameMode, String filledItem) {
        super(player, gameMode);
        this.filledItem = filledItem;
    }
}
