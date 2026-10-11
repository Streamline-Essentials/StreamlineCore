package net.streamline.platform.savables;

import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.interfaces.IUserManager;
import singularity.objects.CosmicResourcePack;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Users of a 1.8.9 client. It never hosts a server, so the console is the only sender and
 * no player is ever online.
 */
public class UserManager implements IUserManager<Object, Object> {

    private static UserManager instance;

    public UserManager() {
        instance = this;
    }

    public static UserManager getInstance() {
        return instance;
    }

    @Override
    public Optional<CosmicPlayer> getOrCreatePlayer(Object player) {
        return Optional.empty();
    }

    @Override
    public Optional<CosmicSender> getOrCreateSender(Object sender) {
        return Optional.ofNullable(UserUtils.getConsole());
    }

    @Override
    public String getUsername(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().getConsoleDiscriminator()))
            return GivenConfigs.getMainConfig().getConsoleName();
        return null;
    }

    @Override
    public boolean isOnline(String uuid) {
        return UserUtils.isConsole(uuid);
    }

    @Override
    public String parsePlayerIP(String uuid) {
        return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
    }

    @Override
    public boolean runAs(CosmicSender player, boolean bypass, String command) {
        return false;
    }

    @Override
    public ConcurrentSkipListSet<CosmicPlayer> getUsersOn(String server) {
        return new ConcurrentSkipListSet<>();
    }

    @Override
    public void connect(CosmicPlayer user, String server) {
    }

    @Override
    public void sendUserResourcePack(CosmicPlayer user, CosmicResourcePack pack) {
    }

    @Override
    public double getPlayerPing(String uuid) {
        return 0;
    }

    @Override
    public void kick(CosmicPlayer user, String message) {
    }

    @Override
    public Object getPlayer(String uuid) {
        return null;
    }

    @Override
    public ConcurrentSkipListMap<String, CosmicPlayer> ensurePlayers() {
        return new ConcurrentSkipListMap<>();
    }

    @Override
    public String getServerPlayerIsOn(Object player) {
        return "--null";
    }

    @Override
    public String getServerPlayerIsOn(String uuid) {
        return "--null";
    }

    @Override
    public String getDisplayName(String uuid) {
        return null;
    }

    @Override
    public void teleport(CosmicPlayer player, CosmicLocation location) {
    }
}
