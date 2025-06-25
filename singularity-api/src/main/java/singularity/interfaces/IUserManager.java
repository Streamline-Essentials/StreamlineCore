package singularity.interfaces;

import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.objects.CosmicResourcePack;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public interface IUserManager<C, P extends C> {
    Optional<CosmicPlayer> getOrCreatePlayer(P player);

    Optional<CosmicSender> getOrCreateSender(C sender);

    String getUsername(String uuid);

    boolean isOnline(String uuid);

    boolean runAs(CosmicSender user, boolean bypass, String command);

    ConcurrentSkipListSet<CosmicPlayer> getUsersOn(String server);

    void connect(CosmicPlayer user, String server);

    void kick(CosmicPlayer user, String message);

    void sendUserResourcePack(CosmicPlayer user, CosmicResourcePack pack);

    String parsePlayerIP(String uuid);

    double getPlayerPing(String uuid);

    String getServerPlayerIsOn(String uuid);

    String getServerPlayerIsOn(P player);

    String getDisplayName(String uuid);

    P getPlayer(String uuid);

    ConcurrentSkipListMap<String, CosmicPlayer> ensurePlayers();

    void teleport(CosmicPlayer player, CosmicLocation location);
}
