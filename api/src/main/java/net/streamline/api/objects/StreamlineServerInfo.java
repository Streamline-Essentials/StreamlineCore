package net.streamline.api.objects;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.data.players.StreamPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Getter
public class StreamlineServerInfo {
    private final String identifier;
    @Setter
    private String name;
    @Setter
    private String motd;
    @Setter
    private String address;
    @Setter
    private ConcurrentSkipListSet<String> onlineUsers;

    public StreamlineServerInfo(String identifier, String name, String motd, String address, ConcurrentSkipListSet<String> onlineUsers) {
        this.identifier = identifier;
        this.name = name;
        this.motd = motd;
        this.address = address;
        this.onlineUsers = onlineUsers;
    }

    public void updateUsersTo(List<StreamPlayer> users) {
        onlineUsers = new ConcurrentSkipListSet<>();
        users.forEach(a -> onlineUsers.add(a.getUuid()));
    }

    public void updateUsersTo(StreamPlayer... users) {
        updateUsersTo(Arrays.stream(users).collect(Collectors.toList()));
    }
}
