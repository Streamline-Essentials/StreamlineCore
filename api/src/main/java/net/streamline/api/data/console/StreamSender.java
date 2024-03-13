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
import net.streamline.api.loading.Loadable;
import net.streamline.api.utils.UserUtils;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

@Getter
public class StreamSender implements Loadable {
    public String getIdentifier() {
        return getUuid();
    }

    public void setIdentifier(String identifier) {
        setUuid(identifier);
    }

    @Setter
    private String uuid;

    @Setter
    private Date firstJoinDate;
    @Setter
    private Date lastJoinDate;
    @Setter
    private Date lastQuitDate;

    private String currentName;
    @Setter
    private long playSeconds;
    @Setter
    private double points;

    @Setter
    private StreamServer server;

    @Setter
    private SenderMeta meta;
    @Setter
    private SenderLeveling leveling;
    @Setter
    private SenderPermissions permissions;

    @Setter
    private boolean proxyTouched;

    @Setter
    private boolean loadComplete = false;

    public StreamSender(String uuid) {
        this.uuid = uuid;

        this.firstJoinDate = new Date();
        this.lastJoinDate = new Date();

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

    public StreamSender setCurrentName(String currentName) {
        String processed = currentName;

        if (processed == null || processed.isBlank() || processed.isEmpty()) {
            processed = SLAPI.getInstance().getUserManager().getUsername(getUuid());
        }

        this.currentName = processed;
        return this;
    }

    public void setCurrentNameAsProper() {
        setCurrentName(SLAPI.getInstance().getUserManager().getUsername(getUuid()));
    }

    public void save() {
        // Do nothing
    }

    public <S extends StreamSender> S thenPopulate(CompletableFuture<S> future) {
        loadComplete = false;

        CompletableFuture.runAsync(() -> {
            S sender = future.join();

            setUuid(sender.getUuid());
            setFirstJoinMillis(sender.getFirstJoinMillis());
            setLastJoinMillis(sender.getLastJoinMillis());
            setLastQuitMillis(sender.getLastQuitMillis());
            setCurrentName(sender.getCurrentName());
            setPlaySeconds(sender.getPlaySeconds());
            setPoints(sender.getPoints());
            setServerName(sender.getServerName());
            setMeta(sender.getMeta());
            setLeveling(sender.getLeveling());
            setPermissions(sender.getPermissions());

            thenPopulateMore(sender);

            setCurrentNameAsProper(); // might need to be forced... need to check this...

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

    public void setFirstJoinMillis(long millis) {
        if (millis == -1) {
            this.firstJoinDate = null;
            return;
        }
        this.firstJoinDate = new Date(millis);
    }

    public void setLastJoinMillis(long millis) {
        if (millis == -1) {
            this.lastJoinDate = null;
            return;
        }
        this.lastJoinDate = new Date(millis);
    }

    public void setLastQuitMillis(long millis) {
        if (millis == -1) {
            this.lastQuitDate = null;
            return;
        }
        this.lastQuitDate = new Date(millis);
    }

    public long getFirstJoinMillis() {
        if (this.firstJoinDate == null) return -1;
        return this.firstJoinDate.getTime();
    }

    public void setFirstJoinNull() {
        this.firstJoinDate = null;
    }

    public long getLastJoinMillis() {
        if (this.lastJoinDate == null) return -1;
        return this.lastJoinDate.getTime();
    }

    public void setLastJoinNull() {
        this.lastJoinDate = null;
    }

    public long getLastQuitMillis() {
        if (this.lastQuitDate == null) return -1;
        return this.lastQuitDate.getTime();
    }

    public void setLastQuitNull() {
        this.lastQuitDate = null;
    }

    public String getDisplayName() {
        if (! getMeta().getFull().isEmpty() && ! getMeta().getFull().isBlank()) {
            return getMeta().getFull();
        }

        return isConsole() ?
                getConsoleDisplayName() :
                (
                        UserUtils.getLuckPermsPrefix(getCurrentName()) +
                                (
                                        (getMeta().getNickname().isEmpty() || getMeta().getNickname().isBlank()) ?
                                                getCurrentName() :
                                                getMeta().getNickname()
                                ) +
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
