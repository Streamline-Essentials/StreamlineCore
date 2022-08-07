package net.streamline.api.profile;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.objects.StreamlineServerInfo;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

public class APIProfile {
    @Getter @Setter
    private String token;
    @Getter @Setter
    private ConcurrentSkipListMap<String, StreamlineServerInfo> servers = new ConcurrentSkipListMap<>();

    public APIProfile(ConcurrentSkipListMap<String, StreamlineServerInfo> servers) {
        this.token = UUID.randomUUID().toString();
        this.servers = servers;
    }

    public APIProfile(StreamlineServerInfo... servers) {
        this.token = UUID.randomUUID().toString();
        this.servers = new ConcurrentSkipListMap<>();
        Arrays.stream(servers).forEach(a -> {
            this.servers.put(a.getIdentifier(), a);
        });
    }

    public void verifyToken() {
//        SLAPI.getInstance().getProxyMessenger().sendMessage(Serv);
    }
}
