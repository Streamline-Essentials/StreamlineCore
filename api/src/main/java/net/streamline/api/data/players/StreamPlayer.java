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
        SLAPI.getMainDatabase().loadPlayer(getUuid()).whenComplete((playerOptional, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                return;
            }

            if (playerOptional.isEmpty()) return;
            StreamPlayer player = playerOptional.get();

            setFirstJoin(player.getFirstJoin().getTime());
            setLastJoin(player.getLastJoin().getTime());
            setLastQuit(player.getLastQuit().getTime());
            setPlaySeconds(player.getPlaySeconds());
            setPoints(player.getPoints());
            setMeta(player.getMeta());
            setPermissions(player.getPermissions());
            setLeveling(player.getLeveling());

            setCurrentIP(player.getCurrentIP());
            setLocation(player.getLocation());
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
