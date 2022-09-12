package net.streamline.api.objects;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.savables.users.StreamlinePlayer;

public class StreamlineResourcePack {
    @Getter @Setter
    private String url;
    @Getter @Setter
    private byte[] hash;
    @Getter @Setter
    private String prompt;
    @Getter @Setter
    private boolean force;

    public StreamlineResourcePack(String url, byte[] hash, String prompt, boolean force) {
        this.url = url;
        this.hash = hash;
        this.prompt = prompt;
        this.force = force;
    }

    public void sendPlayer(StreamlinePlayer player) {
        SLAPI.getInstance().getPlatform().sendResourcePack(this, player);
    }
}
