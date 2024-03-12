package net.streamline.api.data.players;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.location.PlayerLocation;
import net.streamline.api.data.players.location.PlayerWorld;
import net.streamline.api.database.CoreDBOperator;
import net.streamline.api.interfaces.audiences.real.RealPlayer;
import net.streamline.api.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Getter @Setter
public class StreamPlayer extends StreamSender {
    private String currentIP;
    private PlayerLocation location;

    public StreamPlayer(String uuid) {
        super(uuid);

        setServerName("");

        this.currentIP = "";

        this.location = new PlayerLocation(this);
    }

    @Override
    public <S extends StreamSender> void thenPopulateMore(S sender) {
        if (! (sender instanceof StreamPlayer)) return;
        StreamPlayer player = (StreamPlayer) sender;

        setCurrentIP(player.getCurrentIP());
        setLocation(player.getLocation());
    }

    public CoreDBOperator getDatabase() {
        return SLAPI.getMainDatabase();
    }

    public boolean exists() {
        return UserUtils.userExists(this.getUuid());
    }

    @Override
    public void save() {
        CompletableFuture.runAsync(() -> getDatabase().savePlayer(this));
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
            Optional<StreamPlayer> optional = UserUtils.getOrCreatePlayerAsync(getUuid()).join();
            if (optional.isEmpty()) return;
            StreamPlayer streamPlayer = optional.get();

            setFirstJoin(streamPlayer.getFirstJoin().getTime());
            setLastJoin(streamPlayer.getLastJoin().getTime());
            setLastQuit(streamPlayer.getLastQuit().getTime());
            setPlaySeconds(streamPlayer.getPlaySeconds());
            setPoints(streamPlayer.getPoints());
            setMeta(streamPlayer.getMeta());
            setPermissions(streamPlayer.getPermissions());
            setLeveling(streamPlayer.getLeveling());

            setCurrentIP(streamPlayer.getCurrentIP());
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
}
