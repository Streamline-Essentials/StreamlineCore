package net.streamline.api.data.players;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.events.SenderSaveEvent;
import net.streamline.api.data.players.location.PlayerLocation;
import net.streamline.api.data.players.location.PlayerWorld;
import net.streamline.api.data.server.StreamServer;
import net.streamline.api.database.CoreDBOperator;
import net.streamline.api.interfaces.audiences.real.RealPlayer;
import net.streamline.api.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class StreamPlayer extends StreamSender {
    @Getter
    private String currentIp;
    @Setter
    private PlayerLocation location;

    public StreamPlayer(String uuid) {
        super(uuid);

        setServerName("");

        this.currentIp = "";

        this.location = new PlayerLocation(this);
    }

    public PlayerLocation getLocation() {
        if (location == null) {
            location = new PlayerLocation(this);
        }

        return location;
    }

    public StreamSender setCurrentIp(String currentIP) {
        String processed = currentIP;

        if (processed == null || processed.isBlank() || processed.isEmpty()) {
            processed = SLAPI.getInstance().getUserManager().parsePlayerIP(getUuid());
        }

        this.currentIp = processed;
        return this;
    }

    @Override
    public void augmentMore(StreamPlayer player) {
        setCurrentIp(player.getCurrentIp());
        setLocation(player.getLocation());

        setCurrentIpAsProper(); // might need to be forced... need to check this...
        setCurrentServerAsProper(); // might need to be forced... need to check this...
    }

    public void setCurrentIpAsProper() {
        setCurrentIp(SLAPI.getInstance().getUserManager().parsePlayerIP(getUuid()));
    }

    public CoreDBOperator getDatabase() {
        return SLAPI.getMainDatabase();
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
        setServerName(SLAPI.getInstance().getUserManager().getServerPlayerIsOn(getUuid()));
    }

    @Override
    public void save() {
        ensureCorrect();

        getDatabase().savePlayer(this);

        new SenderSaveEvent(this).fire();
    }

    @Override
    public boolean isOnline() {
        return SLAPI.getInstance().getUserManager().isOnline(this.getUuid());
    }

    public RealPlayer<?> asReal() {
        return SLAPI.getPlayerFromUuid(this.getUuid());
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
            Optional<StreamPlayer> optional = SLAPI.getMainDatabase().loadPlayer(getUuid()).join();
            if (optional.isEmpty()) return;
            StreamPlayer streamPlayer = optional.get();

            setFirstJoinMillis(streamPlayer.getFirstJoinDate().getTime());
            setLastJoinMillis(streamPlayer.getLastJoinDate().getTime());
            setLastQuitMillis(streamPlayer.getLastQuitDate().getTime());
            setPlaySeconds(streamPlayer.getPlaySeconds());
            setPoints(streamPlayer.getPoints());
            setMeta(streamPlayer.getMeta());
            setPermissions(streamPlayer.getPermissions());
            setLeveling(streamPlayer.getLeveling());

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
        return SLAPI.getInstance().getUserManager().getServerPlayerIsOn(getUuid());
    }

    public void setServerToRealServer() {
        setServerName(getRealServer());
    }

    @Override
    public void setServer(StreamServer server) {
        getLocation().setServer(server);
    }

    @Override
    public void setServerName(String serverName) {
        getLocation().setServerName(serverName);
    }

    @Override
    public StreamServer getServer() {
        return getLocation().getServer();
    }

    @Override
    public String getServerName() {
        return getLocation().getServerName();
    }
}
