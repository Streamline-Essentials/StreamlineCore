package singularity.data.console;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.CreatePlayerEvent;
import singularity.data.players.events.CreateSenderEvent;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.meta.SenderMeta;
import singularity.data.players.permissions.SenderPermissions;
import singularity.data.server.CosmicServer;
import singularity.interfaces.audiences.real.RealSender;
import singularity.loading.Loadable;
import singularity.modules.ModuleUtils;
import singularity.text.UsersReplacements;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Getter
public class CosmicSender implements Loadable<CosmicSender> {
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
    private SenderMeta meta;
    @Setter
    private SenderPermissions permissions;

    @Setter
    private boolean proxyTouched;

    @Setter
    private boolean fullyLoaded = false;

    @Setter
    private boolean temporary = false;

    @Setter
    private UsersReplacements replacements;

    public CosmicSender(String uuid, boolean temporary) {
        this.uuid = uuid;

        this.temporary = temporary;

        this.firstJoinDate = new Date();
        this.lastJoinDate = new Date();

        this.currentName = "";

        this.playSeconds = 0;

        this.meta = new SenderMeta(this);
        this.permissions = new SenderPermissions(this);

        this.replacements = new UsersReplacements(getUuid());

        this.proxyTouched = Singularity.isProxy();
    }

    public CosmicSender(String uuid) {
        this(uuid, false);
    }

    public CosmicSender() {
        this(
                GivenConfigs.getMainConfig().getConsoleDiscriminator()
        );

        this.setCurrentName(GivenConfigs.getMainConfig().getConsoleName());
        this.augment(Singularity.getMainDatabase().loadPlayer(getIdentifier()), false);
    }

    public CosmicSender setCurrentName(String currentName) {
        String processed = currentName;

        if (processed == null || processed.isBlank() || processed.isEmpty()) {
            processed = Singularity.getInstance().getUserManager().getUsername(getUuid());
        }

        this.currentName = processed;
        return this;
    }

    public void setCurrentNameAsProper() {
        setCurrentName(Singularity.getInstance().getUserManager().getUsername(getUuid()));
    }

    @Override
    public void save(boolean async) {
        UserUtils.saveSender(this, async);
    }

    @Override
    public void load() {
        if (this instanceof CosmicPlayer) {
            UserUtils.loadPlayer((CosmicPlayer) this);
            return;
        }

        UserUtils.loadSender(this);
    }

    @Override
    public void unload() {
        UserUtils.unloadSender(this);
    }

    @Override
    public void saveAndUnload(boolean async) {
        save(async);
        unload();
    }

    public boolean isLoaded() {
        return UserUtils.isLoaded(getUuid());
    }

    public void ensureLoaded() {
        if (! isLoaded()) load();
    }

    @Override
    public CosmicSender augment(CompletableFuture<Optional<CosmicSender>> future, boolean isGet) {
        fullyLoaded = false;

        future.whenComplete((optional, error) -> {
            if (error != null) {
                MessageUtils.logWarning("Failed to augment CosmicSender for UUID: " + getUuid(), error);
                this.fullyLoaded = true;
                return;
            }

            if (optional.isPresent()) {
                CosmicSender sender = optional.get();

                setUuid(sender.getUuid());
                setFirstJoinMillis(sender.getFirstJoinMillis());
                setLastJoinMillis(sender.getLastJoinMillis());
                setLastQuitMillis(sender.getLastQuitMillis());
                setCurrentName(sender.getCurrentName());
                addPlaySeconds(sender.getPlaySeconds());
                setServerName(sender.getServerName());
                setMeta(sender.getMeta());
                setPermissions(sender.getPermissions());

                if (this instanceof CosmicPlayer && sender instanceof CosmicPlayer) augmentMore((CosmicPlayer) sender);

                setCurrentNameAsProper(); // might need to be forced... need to check this...
            } else {
                if (! isGet) {
                    this.temporary = false;
                    if (this instanceof CosmicPlayer) new CreatePlayerEvent((CosmicPlayer) this).fire();
                    else new CreateSenderEvent(this).fire();
                    MessageUtils.logInfo("Created new CosmicPlayer for UUID: " + getUuid() + " (Console: " + isConsole() + ")");

                    this.save();
                } else {
                    unload();
                    fullyLoaded = true;
                    return;
                }
            }

            ensureLoaded();
            fullyLoaded = true;
        });

        return this;
    }

    public void augmentMore(CosmicPlayer sender) {
        // nothing
    }

    public boolean isConsole() {
        return getUuid().equals(getConsoleDiscriminator());
    }

    public void sendMessage(String message, boolean format) {
        if (format) asReal().sendMessage(ModuleUtils.replacePlaceholders(this, message));
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
        if (this instanceof CosmicPlayer) {
            return ((CosmicPlayer) this).asReal();
        }

        if (getUuid().equals(getConsoleDiscriminator())) {
            return Singularity.getConsole();
        }

        return Singularity.getPlayerFromUuid(this.getUuid());
    }

    public CosmicServer getServer() {
        if (isConsole()) return new CosmicServer(getConsoleServer());

        try {
            CosmicPlayer player = (CosmicPlayer) this;
            return player.getLocation().getServer();
        } catch (ClassCastException e) {
            return new CosmicServer("");
        }
    }

    public void setServer(CosmicServer server) {
        if (isConsole()) return;

        try {
            CosmicPlayer player = (CosmicPlayer) this;
            player.getLocation().setServer(server);
        } catch (ClassCastException e) {
            // Do nothing
        }
    }

    public String getServerName() {
        return getServer().getIdentifier();
    }

    public void setServerName(String serverName) {
        if (isConsole()) return;

        try {
            CosmicPlayer player = (CosmicPlayer) this;
            player.getLocation().setServerName(serverName);
        } catch (ClassCastException e) {
            // Do nothing
        }
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
                        UserUtils.getPrefix(this) +
                                (
                                        (getMeta().getNickname().isEmpty() || getMeta().getNickname().isBlank()) ?
                                                getCurrentName() :
                                                getMeta().getNickname()
                                ) +
                                UserUtils.getSuffix(this)
                );
    }

    public void addPlaySeconds(long amount) {
        this.playSeconds += amount;
    }

    public void removePlaySecond(long amount) {
        this.playSeconds -= amount;
    }

    public void addTag(String tag) {
        getMeta().addTag(tag);
    }

    public void removeTag(String tag) {
        getMeta().removeTag(tag);
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

    public void teleport(CosmicPlayer player) {
        ModuleUtils.teleport(this, player);
    }

    public void teleport(CosmicLocation location) {
        ModuleUtils.teleport(this, location);
    }

    public void connect(CosmicServer server) {
        if (server == null) {
            MessageUtils.logWarning("Cannot connect to null server for player " + getUuid() + ".");
            return;
        }

        ModuleUtils.connect(this, server);
    }

    public void connect(CosmicSender other) {
        connect(other.getServer());
    }

    public void connect(String serverName) {
        if (serverName == null || serverName.isEmpty()) {
            MessageUtils.logWarning("Cannot connect to null server for player " + getUuid() + ".");
            return;
        }

        ModuleUtils.connect(this, serverName);
    }
}
