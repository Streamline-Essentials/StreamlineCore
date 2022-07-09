package net.streamline.api.savables.users;

import de.leonhard.storage.internal.FlatFile;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.base.Streamline;
import net.streamline.api.configs.FlatFileResource;
import net.streamline.api.configs.StorageUtils;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.api.savables.SavableResource;
import net.streamline.api.savables.UserManager;
import net.streamline.utils.MessagingUtils;

import java.util.List;
import java.util.Optional;

public abstract class SavableUser extends SavableResource {
    private SavableUser savableUser;
    public String latestName;
    public String displayName;
    public List<String> tagList;
    public double points;
    public String lastMessage;
    public boolean online;
    public String latestServer;

    public SavableUser getSavableUser() {
        return this.savableUser;
    }

    public String findServer() {
        if (this.uuid.equals("%")) {
            return Streamline.getMainConfig().userConsoleServer();
        } else {
            try {
                ProxiedPlayer player = Streamline.getInstance().getPlayer(this.uuid);

                if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

                if (player.getServer() == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

                return player.getServer().getInfo().getName();
            } catch (Exception e) {
                return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
            }
        }
    }

    public Optional<CommandSender> findSenderOptional() {
        if (this.uuid == null) return Optional.empty();
        if (this.uuid.equals("")) return Optional.empty();
        if (this.uuid.equals("%")) return Optional.ofNullable(Streamline.getInstance().getProxy().getConsole());
        else return Optional.ofNullable(Streamline.getInstance().getPlayer(this.uuid));
    }

    public CommandSender findSender() {
        if (findSenderOptional().isPresent()) {
            return findSenderOptional().get();
        }
        return null;
    }

    public String grabServer() {
        if (this.uuid.equals("%")) return Streamline.getMainConfig().userConsoleServer();
        else {
            if (findSenderOptional().isPresent()) {
                ProxiedPlayer player = (ProxiedPlayer) (findSenderOptional().get());
                if (player.getServer() != null) {
                    return player.getServer().getInfo().getName();
                }
            }
        }

        return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
    }

    public boolean updateOnline() {
        if (uuid.equals("%")) this.online = false;

        this.online = UserManager.isOnline(this.uuid);
        return this.online;
    }

    public SavableUser(String uuid) {
        super(uuid, UserManager.newStorageResource(uuid));

        populateDefaults();

        this.savableUser = this;
    }

    @Override
    public void populateDefaults() {
        // Profile.
        latestName = getOrSetDefault("profile.latest.name", UserManager.getUsername(findSender()));
        latestServer = getOrSetDefault("profile.latest.server", MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get());
        displayName = getOrSetDefault("profile.display-name", latestName);
        tagList = getOrSetDefault("profile.tags", getTagsFromConfig());
        points = getOrSetDefault("profile.points", Streamline.getMainConfig().userCombinedPointsDefault());

        populateMoreDefaults();
    }

    abstract public List<String> getTagsFromConfig();

    abstract public void populateMoreDefaults();

    @Override
    public void loadValues() {
        // Profile.
        latestName = getOrSetDefault("profile.latest.name", latestName);
        latestServer = getOrSetDefault("profile.latest.server", latestServer);
        displayName = getOrSetDefault("profile.display-name", displayName);
        tagList = getOrSetDefault("profile.tags", tagList);
        points = getOrSetDefault("profile.points", points);
        // Online.
        online = updateOnline();
        // More.
        loadMoreValues();
    }

    abstract public void loadMoreValues();

    public void saveAll() {
        // Profile.
        set("profile.latest.name", latestName);
        set("profile.latest.server", latestServer);
        set("profile.display-name", latestName);
        set("profile.tags", tagList);
        set("profile.points", points);
        // More.
        saveMore();
        storageResource.push();
    }

    abstract public void saveMore();

    public void addTag(String tag) {
        //        loadValues();
        if (tagList.contains(tag)) return;

        tagList.add(tag);
        //        saveAll();
    }

    public void removeTag(String tag) {
        //        loadValues();
        if (! tagList.contains(tag)) return;

        tagList.remove(tag);
        //        saveAll();
    }

    public void setPoints(double amount) {
        points = amount;
    }

    public void addPoints(double amount) {
        setPoints(points + amount);
    }

    public void removePoints(double amount) {
        setPoints(points - amount);
    }

    public void updateLastMessage(String message) {
        lastMessage = message;
    }

    public void setLatestServer(String server) {
        latestServer = server;
    }

    public void setLatestName(String name) {
        latestName = name;
    }

    public void setDisplayName(String name) {
        displayName = name;
    }

    public String getName() {
        return latestName;
    }

    public boolean hasPermission(String permission) {
        return findSender().hasPermission(permission);
    }

    public void setPermission(String permission, boolean value) {
        findSender().setPermission(permission, value);
    }

    public void sendMessage(BaseComponent message) {
        if (this.findSenderOptional().isPresent()) {
            this.findSenderOptional().get().sendMessage(message);
        }
    }

    public void sendMessage(String message) {
        if (this.findSenderOptional().isPresent()) {
            this.findSenderOptional().get().sendMessage(MessagingUtils.codedText(message));
        }
    }

    public void dispose() throws Throwable {
        try {
            UserManager.unloadUser(this);
            this.uuid = null;
            if (StorageUtils.areUsersFlatFiles()) {
                FlatFileResource<? extends FlatFile> resource = (FlatFileResource<? extends FlatFile>) this.storageResource;
                resource.delete();
            }
        } finally {
            super.finalize();
        }
    }
}
