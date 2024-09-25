package singularity.interfaces;

import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;

public interface IBackendHandler {
    public void teleport(CosmicPlayer player, CosmicLocation location);
}
