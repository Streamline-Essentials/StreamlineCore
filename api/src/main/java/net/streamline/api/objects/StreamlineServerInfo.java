package net.streamline.api.objects;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class StreamlineServerInfo {
    @Getter
    private final String identifier;
    @Getter @Setter
    private String name;
    @Getter @Setter
    private String motd;
    @Getter @Setter
    private String address;
    @Getter @Setter
    private ConcurrentSkipListMap<String, StreamlineUser> onlineUsers;

    public StreamlineServerInfo(String identifier, String name, String motd, String address, ConcurrentSkipListMap<String, StreamlineUser> onlineUsers) {
        this.identifier = identifier;
        this.name = name;
        this.motd = motd;
        this.address = address;
        this.onlineUsers = onlineUsers;
    }

    public void updateUsersTo(List<StreamlineUser> users) {
        onlineUsers = new ConcurrentSkipListMap<>();
        users.forEach(a -> onlineUsers.put(a.getUUID(), a));
    }

    public void updateUsersTo(StreamlineUser... users) {
        updateUsersTo(Arrays.stream(users).toList());
    }
}
