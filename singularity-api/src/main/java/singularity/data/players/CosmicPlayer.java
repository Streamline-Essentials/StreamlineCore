package singularity.data.players;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.console.CosmicSender;
import singularity.data.players.events.SaveSenderEvent;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.server.CosmicServer;
import singularity.database.CoreDBOperator;
import singularity.interfaces.audiences.real.RealPlayer;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CosmicPlayer extends CosmicSender {
    @Getter
    private String currentIp;
    @Setter
    private CosmicLocation location;

    public CosmicPlayer(String uuid) {
        super(uuid);

        setServerName("");

        this.currentIp = "";

        this.location = new CosmicLocation(this);
    }

    public CosmicLocation getLocation() {
        if (location == null) {
            location = new CosmicLocation(this);
        }

        return location;
    }

    public CosmicSender setCurrentIp(String currentIP) {
        String processed = currentIP;

        if (processed == null || processed.isBlank() || processed.isEmpty()) {
            processed = Singularity.getInstance().getUserManager().parsePlayerIP(getUuid());
        }

        this.currentIp = processed;
        return this;
    }

    @Override
    public void augmentMore(CosmicPlayer player) {
        setCurrentIp(player.getCurrentIp());
        setLocation(player.getLocation());

        setCurrentIpAsProper(); // might need to be forced... need to check this...
        setCurrentServerAsProper(); // might need to be forced... need to check this...
    }

    public void setCurrentIpAsProper() {
        setCurrentIp(Singularity.getInstance().getUserManager().parsePlayerIP(getUuid()));
    }

    public CoreDBOperator getDatabase() {
        return Singularity.getMainDatabase();
    }

    public boolean exists() {
        return UserUtils.userExists(this.getUuid());
    }

    public void ensureCorrect() {
        setCurrentNameAsProper();
        setCurrentIpAsProper();
        setCurrentServerAsProper();
    }

    public void setCurrentServerAsProper() {
        setServerName(Singularity.getInstance().getUserManager().getServerPlayerIsOn(getUuid()));
    }

    @Override
    public void save() {
        ensureCorrect();

        getDatabase().savePlayer(this);

        new SaveSenderEvent(this).fire();
    }

    @Override
    public boolean isOnline() {
        return Singularity.getInstance().getUserManager().isOnline(this.getUuid());
    }

    @Override
    public RealPlayer<?> asReal() {
        return Singularity.getPlayerFromUuid(this.getUuid());
    }

    public PlayerWorld getWorld() {
        return getLocation().getWorld();
    }

    public void setWorld(PlayerWorld world) {
        getLocation().setWorld(world);
    }

    public String getWorldName() {
        return getLocation().getWorldName();
    }

    public void setWorldName(String worldName) {
        getLocation().setWorldName(worldName);
    }

    public double getX() {
        return getLocation().getX();
    }

    public void setX(double x) {
        getLocation().setX(x);
    }

    public double getY() {
        return getLocation().getY();
    }

    public void setY(double y) {
        getLocation().setY(y);
    }

    public double getZ() {
        return getLocation().getZ();
    }

    public void setZ(double z) {
        getLocation().setZ(z);
    }

    public float getYaw() {
        return getLocation().getYaw();
    }

    public void setYaw(float yaw) {
        getLocation().setYaw(yaw);
    }

    public float getPitch() {
        return getLocation().getPitch();
    }

    public void setPitch(float pitch) {
        getLocation().setPitch(pitch);
    }

    @Override
    public void reload() {
        CompletableFuture.runAsync(() -> {
            Optional<CosmicPlayer> optional = Singularity.getMainDatabase().loadPlayer(getUuid()).join();
            if (optional.isEmpty()) return;
            CosmicPlayer streamPlayer = optional.get();

            setFirstJoinMillis(streamPlayer.getFirstJoinDate().getTime());
            setLastJoinMillis(streamPlayer.getLastJoinDate().getTime());
            setLastQuitMillis(streamPlayer.getLastQuitDate().getTime());
            setPlaySeconds(streamPlayer.getPlaySeconds());
            setMeta(streamPlayer.getMeta());
            setPermissions(streamPlayer.getPermissions());

            setCurrentIp(streamPlayer.getCurrentIp());
            setLocation(streamPlayer.getLocation());
        });
    }

    public double getPlayMinutes() {
        return getPlaySeconds() / 60d;
    }

    public String getPlayMinutesAsString() {
        return String.format("%.2f", getPlayMinutes());
    }

    public double getPlayHours() {
        return getPlayMinutes() / 60d;
    }

    public String getPlayHoursAsString() {
        return String.format("%.2f", getPlayHours());
    }

    public double getPlayDays() {
        return getPlayHours() / 24d;
    }

    public String getPlayDaysAsString() {
        return String.format("%.2f", getPlayDays());
    }

    public String getPlaySecondsAsString() {
        return String.valueOf(getPlaySeconds());
    }

    public String getRealServer() {
        return Singularity.getInstance().getUserManager().getServerPlayerIsOn(getUuid());
    }

    public void setServerToRealServer() {
        setServerName(getRealServer());
    }

    @Override
    public void setServer(CosmicServer server) {
        getLocation().setServer(server);
    }

    @Override
    public void setServerName(String serverName) {
        getLocation().setServerName(serverName);
    }

    @Override
    public CosmicServer getServer() {
        return getLocation().getServer();
    }

    @Override
    public String getServerName() {
        return getLocation().getServerName();
    }
}
