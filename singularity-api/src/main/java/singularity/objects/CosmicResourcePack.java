package singularity.objects;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;

@Setter
@Getter
public class CosmicResourcePack {
    private String url;
    private byte[] hash;
    private String prompt;
    private boolean force;

    public CosmicResourcePack(String url, byte[] hash, String prompt, boolean force) {
        this.url = url;
        this.hash = hash;
        this.prompt = prompt;
        this.force = force;
    }

    public void sendPlayer(CosmicPlayer player) {
        Singularity.getInstance().getPlatform().sendResourcePack(this, player);
    }
}
