package net.streamline.api.data.console;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.data.players.leveling.SenderLeveling;
import net.streamline.api.data.players.meta.SenderMeta;
import net.streamline.api.data.players.permissions.SenderPermissions;
import net.streamline.api.data.server.StreamServer;
import net.streamline.api.interfaces.audiences.real.RealSender;
import net.streamline.api.utils.UserUtils;
import tv.quaint.objects.Identifiable;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Getter @Setter
public class StreamSender implements Identifiable {
    public String getIdentifier() {
        return getUuid();
    }

    public void setIdentifier(String identifier) {
        setUuid(identifier);
    }

    private String uuid;

    private Date firstJoin;
    private Date lastJoin;
    private Date lastQuit;

    private String currentName;
    private long playSeconds;
    private double points;

    private StreamServer server;

    private SenderMeta meta;
    private SenderLeveling leveling;
    private SenderPermissions permissions;

    private boolean proxyTouched;

    private boolean loadComplete = false;

    public StreamSender(String uuid) {
        this.uuid = uuid;

        this.firstJoin = new Date();
        this.lastJoin = new Date();

        this.currentName = "";

        this.playSeconds = 0;
        this.points = 0;

        this.server = new StreamServer(getConsoleServer());

        this.meta = new SenderMeta(this);
        this.leveling = new SenderLeveling(this);
        this.permissions = new SenderPermissions(this);

        this.proxyTouched = SLAPI.isProxy();
    }

    public StreamSender() {
        this(
                GivenConfigs.getMainConfig().getConsoleDiscriminator()
        );
    }

    public void save() {
        // Do nothing
    }

    public <S extends StreamSender> S thenPopulate(CompletableFuture<Optional<S>> future) {
        loadComplete = false;

        future.whenComplete((optional, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                return;
            }
            if (optional.isEmpty()) return;
            StreamSender sender = optional.get();

            setUuid(sender.getUuid());
            setFirstJoin(sender.getFirstJoinMillis());
            setLastJoin(sender.getLastJoinMillis());
            setLastQuit(sender.getLastQuitMillis());
            setCurrentName(sender.getCurrentName());
            setPlaySeconds(sender.getPlaySeconds());
            setPoints(sender.getPoints());
            setServerName(sender.getServerName());
            setMeta(sender.getMeta());
            setLeveling(sender.getLeveling());
            setPermissions(sender.getPermissions());

            thenPopulateMore(sender);

            loadComplete = true;
        });

        return (S) this;
    }

    public <S extends StreamSender> void thenPopulateMore(S sender) {
        // nothing
    }

    public boolean isConsole() {
        return getUuid().equals(getConsoleDiscriminator());
    }

    public void sendMessage(String message, boolean format) {
        if (format) asReal().sendMessage(message);
        else asReal().sendMessageRaw(message);
    }

    public void sendMessage(String message) {
        sendMessage(message, true);
    }

    public void runCommand(String command) {
        asReal().runCommand(command);
    }

    public void chatAs(String message) {
//        asRealConsole().chatAs(message);
    }

    public boolean hasPermission(String permission) {
        return asReal().hasPermission(permission);
    }

    public boolean isOnline() {
        return true;
    }

    public RealSender<?> asReal() {
        return SLAPI.getConsole();
    }

    public String getServerName() {
        return getServer().getIdentifier();
    }

    public void setServerName(String serverName) {
        server = new StreamServer(serverName);
    }

    public static String getConsoleDiscriminator() {
        return GivenConfigs.getMainConfig().getConsoleDiscriminator();
    }

    public static String getConsoleDisplayName() {
        return GivenConfigs.getMainConfig().getConsoleDisplayName();
    }

    public static String getConsoleName() {
        return GivenConfigs.getMainConfig().getConsoleName();
    }

    public static String getConsoleServer() {
        return GivenConfigs.getMainConfig().getConsoleServer();
    }

    public void setFirstJoin(long millis) {
        if (millis == -1) {
            this.firstJoin = null;
            return;
        }
        this.firstJoin = new Date(millis);
    }

    public void setLastJoin(long millis) {
        if (millis == -1) {
            this.lastJoin = null;
            return;
        }
        this.lastJoin = new Date(millis);
    }

    public void setLastQuit(long millis) {
        if (millis == -1) {
            this.lastQuit = null;
            return;
        }
        this.lastQuit = new Date(millis);
    }

    public long getFirstJoinMillis() {
        if (this.firstJoin == null) return -1;
        return this.firstJoin.getTime();
    }

    public void setFirstJoinNull() {
        this.firstJoin = null;
    }

    public long getLastJoinMillis() {
        if (this.lastJoin == null) return -1;
        return this.lastJoin.getTime();
    }

    public void setLastJoinNull() {
        this.lastJoin = null;
    }

    public long getLastQuitMillis() {
        if (this.lastQuit == null) return -1;
        return this.lastQuit.getTime();
    }

    public void setLastQuitNull() {
        this.lastQuit = null;
    }

    public String getDisplayName() {
        if (! getMeta().getFull().isEmpty() && ! getMeta().getFull().isBlank()) {
            return getMeta().getFull();
        }

        return isConsole() ?
                getConsoleDisplayName() :
                (
                        UserUtils.getLuckPermsPrefix(getCurrentName()) +
                        getCurrentName() +
                        UserUtils.getLuckPermsSuffix(getCurrentName())
                );
    }

    public void addPlaySecond(long amount) {
        this.playSeconds += amount;
    }

    public void removePlaySecond(long amount) {
        this.playSeconds -= amount;
    }

    public void addPoints(double amount) {
        this.points += amount;
    }

    public void removePoints(double amount) {
        this.points -= amount;
    }

    public void addTag(String tag) {
        getMeta().addTag(tag);
    }

    public void removeTag(String tag) {
        getMeta().removeTag(tag);
    }

    public void setLevel(int level) {
        getLeveling().setLevel(level);
    }

    public void addLevel(int amount) {
        getLeveling().addExperience(amount);
    }

    public void removeLevel(int amount) {
        getLeveling().removeExperience(amount);
    }

    public void addExperience(double amount) {
        getLeveling().addExperience(amount);
    }

    public void removeExperience(double amount) {
        getLeveling().removeExperience(amount);
    }

    public void setExperience(double amount) {
        getLeveling().setExperience(amount);
    }

    public void setPermission(String permission) {
        getPermissions().addPermission(permission);
    }

    public void removePermission(String permission) {
        getPermissions().removePermission(permission);
    }

    public void reload() {
        // Do nothing
    }
}
