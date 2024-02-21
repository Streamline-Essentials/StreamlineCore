package net.streamline.api.objects;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.data.players.StreamPlayer;

@Getter
public class StreamlineResourcePack {
    @Setter
    private String url;
    @Setter
    private byte[] hash;
    @Setter
    private String prompt;
    @Setter
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
