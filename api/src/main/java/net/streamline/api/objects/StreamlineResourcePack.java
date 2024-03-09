package net.streamline.api.objects;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.data.players.StreamPlayer;

@Setter
@Getter
public class StreamlineResourcePack {
    private String url;
    private byte[] hash;
    private String prompt;
    private boolean force;

    public StreamlineResourcePack(String url, byte[] hash, String prompt, boolean force) {
        this.url = url;
        this.hash = hash;
        this.prompt = prompt;
        this.force = force;
    }

    public void sendPlayer(StreamPlayer player) {
        SLAPI.getInstance().getPlatform().sendResourcePack(this, player);
    }
}
