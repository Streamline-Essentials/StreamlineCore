package host.plas.data.cause;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;

@Getter @Setter
public class HazardCause {
    private CosmicPlayer player;
    private HazardCauseType type;
    private String value;

    public HazardCause(CosmicPlayer player, HazardCauseType type, String value) {
        this.player = player;
        this.type = type;
        this.value = value;
    }
}
