package net.streamline.api.utils;

import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.uuid.UuidManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.data.players.events.LoadStreamSenderEvent;
import org.jetbrains.annotations.NotNull;
import tv.quaint.utils.MathUtils;

import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

public class UserUtils {
    @Getter @Setter
    private static ConcurrentSkipListMap<String, StreamSender> loadedSenders = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static StreamSender console;

    public static void ensureLoadedUsers() {
        if (getLoadedSenders() == null) loadedSenders = new ConcurrentSkipListMap<>();

        if (! hasConsole()) {
            loadConsole();
        }

        ConcurrentSkipListMap<String, StreamPlayer> ensured = SLAPI.getInstance().getUserManager().ensurePlayers();
        loadedSenders.putAll(ensured);
    }

    public static void loadConsole() {
        if (console == null) console = new StreamSender();
    }

    public static boolean hasConsole() {
        return console != null;
    }

    public static ConcurrentSkipListMap<String, StreamPlayer> getLoadedPlayers() {
        ConcurrentSkipListMap<String, StreamPlayer> r = new ConcurrentSkipListMap<>();

        getLoadedSenders().forEach((s, user) -> {
            if (user instanceof StreamPlayer) r.put(s, (StreamPlayer) user);
        });

        return r;
    }

    public static ConcurrentSkipListSet<StreamSender> getLoadedSendersSet() {
        ConcurrentSkipListSet<StreamSender> r = new ConcurrentSkipListSet<>();

        getLoadedSenders().forEach((s, user) -> {
            if (user != null) r.add(user);
        });

        return r;
    }

    public static ConcurrentSkipListSet<StreamPlayer> getLoadedPlayersSet() {
        ConcurrentSkipListSet<StreamPlayer> r = new ConcurrentSkipListSet<>();

        getLoadedPlayers().forEach((s, user) -> {
            if (user != null) r.add(user);
        });

        return r;
    }

    public static StreamSender loadSender(StreamSender sender) {
        getLoadedSenders().put(sender.getUuid(), sender);
        ModuleUtils.fireEvent(new LoadStreamSenderEvent(sender));
        return sender;
    }

    public static void unloadSender(StreamSender user) {
        unloadSender(user.getUuid());
    }

    public static void unloadSender(String uuid) {
        getLoadedSenders().remove(uuid);
    }

    public static boolean isLoaded(String uuid) {
        return getSender(uuid).isPresent();
    }

    public static ConcurrentSkipListMap<String, StreamSender> getOnlineUsers() {
        ConcurrentSkipListMap<String, StreamSender> r = new ConcurrentSkipListMap<>();

        getLoadedSenders().forEach((s, user) -> {
            if (user.isOnline()) r.put(user.getUuid(), user);
        });

        return r;
    }

    public static ConcurrentSkipListMap<String, StreamPlayer> getOnlinePlayers() {
        ConcurrentSkipListMap<String, StreamPlayer> r = new ConcurrentSkipListMap<>();

        getLoadedPlayers().forEach((s, user) -> {
            if (user.isOnline()) r.put(user.getUuid(), user);
        });

        return r;
    }

    public static boolean userExists(String uuid) {
        return SLAPI.getMainDatabase().exists(uuid).join();
    }

    public static Optional<StreamSender> getSender(String uuid) {
        if (uuid == null) return Optional.empty();
        if (uuid.equals(StreamSender.getConsoleDiscriminator())) return Optional.of(getConsole());

        StreamSender sender = getLoadedSenders().get(uuid);
        if (sender == null) return Optional.empty();

        return Optional.of(sender);
    }

    public static CompletableFuture<Optional<StreamPlayer>> loadPlayerAsync(String uuid) {
        return SLAPI.getMainDatabase().loadPlayer(uuid).whenComplete((user, throwable) -> {
            if (throwable != null) {
                MessageUtils.logWarning("Error:", throwable);
                return;
            }
            if (user.isEmpty()) return;

            StreamSender sender1 = user.get();
            loadSender(sender1);
        }).orTimeout(5, TimeUnit.SECONDS).exceptionally(throwable -> {
            MessageUtils.logWarning("Timed out:", throwable);
            return Optional.empty();
        });
    }

    @Deprecated
    public static Optional<StreamSender> getOrGetSender(String uuid) {
        return getOrGetOrCreateSender(uuid);
    }

    @Deprecated
    public static Optional<StreamPlayer> getOrGetPlayer(String uuid) {
        return getOrGetOrCreatePlayer(uuid);
    }

    public static StreamPlayer loadPlayer(StreamPlayer player) {
        return (StreamPlayer) loadSender(player);
    }

    public static CompletableFuture<StreamSender> getOrGetOrCreateSenderAsync(String uuid) {
        Optional<StreamSender> user = getSender(uuid);
        if (user.isPresent()) return CompletableFuture.completedFuture(user.get());

        if (uuid.equals(StreamSender.getConsoleDiscriminator())) {
            StreamSender sender = getConsole();
            if (sender == null) {
                sender = new StreamSender();
                sender.save();
                loadSender(sender);
            }
            return CompletableFuture.completedFuture(sender);
        }

        return loadPlayerAsync(uuid).thenApply(user1 -> user1.orElseGet(() -> {
            StreamPlayer sender = new StreamPlayer(uuid);
            sender.save();
            loadPlayer(sender);

            return sender;
        }));
    }

    public static CompletableFuture<StreamPlayer> getOrGetOrCreatePlayerAsync(String uuid) {
        return getOrGetOrCreateSenderAsync(uuid).thenApply(user -> (StreamPlayer) user);
    }

    public static Optional<StreamPlayer> getOrGetPlayer(StreamSender sender) {
        if (sender instanceof StreamPlayer) return Optional.of((StreamPlayer) sender);
        return Optional.empty();
    }

    public static Optional<StreamSender> getOrGetOrCreateSender(String uuid) {
        Optional<StreamSender> user = getSender(uuid);
        if (user.isPresent()) return user;

        getOrGetOrCreateSenderAsync(uuid);

        return user;
    }

    public static Optional<StreamPlayer> getOrGetOrCreatePlayer(String uuid) {
        Optional<StreamSender> user = getOrGetOrCreateSender(uuid);
        if (user.isEmpty()) return Optional.empty();

        if (user.get() instanceof StreamPlayer) return user.map(u -> (StreamPlayer) u);

        return Optional.empty();
    }

    public static boolean isConsole(String uuid) {
        return uuid.equals(GivenConfigs.getMainConfig().getConsoleDiscriminator());
    }

    public static String getOffOnFormatted(StreamSender stat) {
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat.isOnline()) {
            return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOnlineName());
        } else {
            return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOfflineName());
        }
    }

    public static String getOffOnAbsolute(StreamSender stat) {
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat.isOnline()) {
            return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOnlineName());
        } else {
            return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOfflineName());
        }
    }

    public static String getFormatted(StreamSender stat) {
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }
        return stat.getDisplayName();
    }

    public static String getAbsolute(StreamSender stat) {
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }
        return stat.getCurrentName();
    }

    public static boolean isValidUuid(String possibleUUID) {
        try {
            UUID.fromString(possibleUUID);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getLuckPermsPrefix(String username) {
        User user;
        if (isValidUuid(username)) user = SLAPI.getLuckPerms().getUserManager().getUser(UUID.fromString(username));
        else user = SLAPI.getLuckPerms().getUserManager().getUser(username);
        if (user == null) {
            return "";
        }

        String prefix;

        Group group = SLAPI.getLuckPerms().getGroupManager().getGroup(user.getPrimaryGroup());
        if (group == null) {
            TreeMap<Integer, String> preWeight = new TreeMap<>();

            for (PrefixNode node : user.getNodes(NodeType.PREFIX)) {
                preWeight.put(node.getPriority(), node.getMetaValue());
            }

            prefix = preWeight.get(MathUtils.getCeilingInt(preWeight.keySet()));

            if (prefix == null) {
                prefix = "";
            }

            return prefix;
        }


        TreeMap<Integer, String> preWeight = new TreeMap<>();

        for (PrefixNode node : group.getNodes(NodeType.PREFIX)) {
            preWeight.put(node.getPriority(), node.getMetaValue());
        }

        for (PrefixNode node : user.getNodes(NodeType.PREFIX)) {
            preWeight.put(node.getPriority(), node.getMetaValue());
        }

        prefix = preWeight.get(MathUtils.getCeilingInt(preWeight.keySet()));

        if (prefix == null) {
            prefix = "";
        }

        return prefix;
    }

    public static String getLuckPermsSuffix(String username){
        User user;
        if (isValidUuid(username)) user = SLAPI.getLuckPerms().getUserManager().getUser(UUID.fromString(username));
        else user = SLAPI.getLuckPerms().getUserManager().getUser(username);
        if (user == null) return "";

        String suffix;

        Group group = SLAPI.getLuckPerms().getGroupManager().getGroup(user.getPrimaryGroup());
        if (group == null){
            TreeMap<Integer, String> preWeight = new TreeMap<>();

            for (PrefixNode node : user.getNodes(NodeType.PREFIX)) {
                preWeight.put(node.getPriority(), node.getMetaValue());
            }

            suffix = preWeight.get(MathUtils.getCeilingInt(preWeight.keySet()));

            if (suffix == null) {
                suffix = "";
            }

            return suffix;
        }


        TreeMap<Integer, String> sufWeight = new TreeMap<>();

        for (SuffixNode node : group.getNodes(NodeType.SUFFIX)) {
            sufWeight.put(node.getPriority(), node.getMetaValue());
        }

        for (SuffixNode node : user.getNodes(NodeType.SUFFIX)) {
            sufWeight.put(node.getPriority(), node.getMetaValue());
        }

        suffix = sufWeight.get(MathUtils.getCeilingInt(sufWeight.keySet()));

        if (suffix == null) suffix = "";

        return suffix;
    }

    public static String getFormattedDefaultNickname(StreamPlayer user) {
        return ModuleUtils.replacePlaceholders(user, user.getMeta().getFull());
    }

    public static void addPermission(User user, String permission) {
        // Add the permission
        user.data().add(Node.builder(permission).build());

        // Now we need to save changes.
        SLAPI.getLuckPerms().getUserManager().saveUser(user);
    }

    public static void removePermission(User user, String permission) {
        // Add the permission
        user.data().remove(Node.builder(permission).build());

        // Now we need to save changes.
        SLAPI.getLuckPerms().getUserManager().saveUser(user);
    }

//    public static boolean runAs(OperatorUser user, String command) {
//        return SLAPI.getInstance().getUserManager().runAs(user.getParent(), true, command);
//    }

    public static boolean runAs(StreamSender user, String command) {
        try {
            user.runCommand(command);
            return true;
        } catch (Exception e) {
            MessageUtils.logWarning(e);
            return false;
        }
    }

    public static Optional<String> getUUIDFromName(String name) {
        return UuidManager.getUuidFromName(name);
    }

    public static Optional<StreamSender> getOrGetUserByName(String name) {
        Optional<String> uuid = getUUIDFromName(name);
        return uuid.flatMap(UserUtils::getOrGetOrCreateSender);

    }

    public static Optional<StreamPlayer> getOrGetPlayerByName(String name) {
        Optional<String> uuid = getUUIDFromName(name);
        if (uuid.isEmpty()) return Optional.empty();

        return getOrGetPlayer(uuid.get());
    }

    public static ConcurrentSkipListSet<StreamPlayer> getPlayersOn(String server) {
        ConcurrentSkipListSet<StreamPlayer> r = new ConcurrentSkipListSet<>();

        getOnlinePlayers().forEach((s, user) -> {
            if (user.getServer().getIdentifier().equals(server)) r.add(user);
        });

        return r;
    }

    public static boolean isGeyserPlayer(StreamPlayer user) {
        return isGeyserPlayer(user.getUuid());
    }

    public static boolean isGeyserPlayer(String uuid) {
        return uuid.startsWith("0000");
    }

    public static void syncAllUsers() {
        getLoadedSenders().forEach((s, user) -> {
            user.save();
        });
    }
}
