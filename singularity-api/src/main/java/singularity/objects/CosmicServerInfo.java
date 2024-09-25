package singularity.objects;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Getter
@Setter
public class CosmicServerInfo {
    private final String identifier;
    private String name;
    private String motd;
    private String address;
    private ConcurrentSkipListSet<String> onlineUsers;

    public CosmicServerInfo(String identifier, String name, String motd, String address, ConcurrentSkipListSet<String> onlineUsers) {
        this.identifier = identifier;
        this.name = name;
        this.motd = motd;
        this.address = address;
        this.onlineUsers = onlineUsers;
    }

    public void updateUsersTo(List<CosmicPlayer> users) {
        onlineUsers = new ConcurrentSkipListSet<>();
        users.forEach(a -> onlineUsers.add(a.getUuid()));
    }

    public void updateUsersTo(CosmicPlayer... users) {
        updateUsersTo(Arrays.stream(users).collect(Collectors.toList()));
    }
}
