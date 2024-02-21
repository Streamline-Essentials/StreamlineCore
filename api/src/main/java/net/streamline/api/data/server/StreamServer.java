package net.streamline.api.data.server;

import lombok.Getter;
import lombok.Setter;
import tv.quaint.objects.Identifiable;

@Getter @Setter
public class StreamServer implements Identifiable {
    private String identifier;

    public StreamServer(String identifier) {
        this.identifier = identifier;
    }
}
