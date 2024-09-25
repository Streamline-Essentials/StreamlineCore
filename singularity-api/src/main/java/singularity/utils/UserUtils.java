package singularity.utils;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.CreateSenderEvent;
import singularity.data.uuid.UuidManager;
import singularity.modules.ModuleUtils;
import singularity.data.players.events.LoadStreamSenderEvent;
import singularity.permissions.MetaValue;
import singularity.permissions.PermissionUtil;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class UserUtils {
    @Getter @Setter
    private static ConcurrentSkipListMap<String, CosmicSender> loadedSenders = new ConcurrentSkipListMap<>();
    @Setter
    private static CosmicSender console;

    public static CosmicSender getConsole() {
        if (! hasConsole()) loadConsole();

        return console;
    }

    public static void ensureLoadedUsers() {
        if (getLoadedSenders() == null) loadedSenders = new ConcurrentSkipListMap<>();

        if (! hasConsole()) {
            loadConsole();
        }

        ConcurrentSkipListMap<String, CosmicPlayer> ensured = Singularity.getInstance().getUserManager().ensurePlayers();
        loadedSenders.putAll(ensured);
    }

    public static void loadConsole() {
        if (! hasConsole()) console = createSender();
    }

    public static boolean hasConsole() {
        return console != null;
    }

    public static ConcurrentSkipListMap<String, CosmicPlayer> getLoadedPlayers() {
        ConcurrentSkipListMap<String, CosmicPlayer> r = new ConcurrentSkipListMap<>();

        getLoadedSenders().forEach((s, user) -> {
            if (user instanceof CosmicPlayer) r.put(s, (CosmicPlayer) user);
        });

        return r;
    }

    public static ConcurrentSkipListSet<CosmicSender> getLoadedSendersSet() {
        ConcurrentSkipListSet<CosmicSender> r = new ConcurrentSkipListSet<>();

        getLoadedSenders().forEach((s, user) -> {
            if (user != null) r.add(user);
        });

        return r;
    }

    public static ConcurrentSkipListSet<CosmicPlayer> getLoadedPlayersSet() {
        ConcurrentSkipListSet<CosmicPlayer> r = new ConcurrentSkipListSet<>();

        getLoadedPlayers().forEach((s, user) -> {
            if (user != null) r.add(user);
        });

        return r;
    }

    public static CosmicSender loadSender(CosmicSender sender) {
        getLoadedSenders().put(sender.getUuid(), sender);
        ModuleUtils.fireEvent(new LoadStreamSenderEvent(sender));
        return sender;
    }

    public static void unloadSender(CosmicSender user) {
        unloadSender(user.getUuid());
    }

    public static void unloadSender(String uuid) {
        getLoadedSenders().remove(uuid);
    }

    public static boolean isLoaded(String uuid) {
        return getSender(uuid).isPresent();
    }

    public static ConcurrentSkipListMap<String, CosmicSender> getOnlineSenders() {
        ConcurrentSkipListMap<String, CosmicSender> r = new ConcurrentSkipListMap<>();

        getLoadedSenders().forEach((s, user) -> {
            if (user.isOnline()) r.put(user.getUuid(), user);
        });

        return r;
    }

    public static ConcurrentSkipListMap<String, CosmicPlayer> getOnlinePlayers() {
        ConcurrentSkipListMap<String, CosmicPlayer> r = new ConcurrentSkipListMap<>();

        getLoadedPlayers().forEach((s, user) -> {
            if (user.isOnline()) r.put(user.getUuid(), user);
        });

        return r;
    }

    public static boolean userExists(String uuid) {
        return Singularity.getMainDatabase().exists(uuid).join();
    }

    public static Optional<CosmicSender> getSender(String uuid) {
        if (uuid == null) return Optional.empty();
        if (uuid.equals(CosmicSender.getConsoleDiscriminator())) return Optional.of(getConsole());

        CosmicSender sender = getLoadedSenders().get(uuid);
        if (sender == null) return Optional.empty();

        if (sender instanceof CosmicPlayer) {
            CosmicPlayer player = (CosmicPlayer) sender;
            return Optional.of(player);
        }

        return Optional.of(sender);
    }

    public static CosmicPlayer loadPlayer(CosmicPlayer player) {
        return (CosmicPlayer) loadSender(player);
    }

    public static CosmicPlayer getOrCreatePlayer(CosmicSender sender) {
        return getOrCreatePlayer(sender.getUuid());
    }

    public static CosmicSender createSender() {
        CosmicSender console = new CosmicSender();
        ModuleUtils.fireEvent(new CreateSenderEvent(console));
        return console;
    }

    public static CosmicSender createSender(String uuid) {
        CosmicSender sender = new CosmicSender(uuid);
        ModuleUtils.fireEvent(new CreateSenderEvent(sender));
        return sender;
    }

    public static CosmicPlayer createPlayer(String uuid) {
        CosmicPlayer player = new CosmicPlayer(uuid);
        ModuleUtils.fireEvent(new CreateSenderEvent(player));
        return player;
    }

    public static CosmicSender getOrCreateSender(String uuid) {
        Optional<CosmicSender> user = getSender(uuid);
        if (user.isPresent()) return user.get();

        if (uuid.equals(CosmicSender.getConsoleDiscriminator())) {
            return getConsole();
        }

        CompletableFuture<Optional<CosmicPlayer>> loader = Singularity.getMainDatabase().loadPlayer(uuid);

        CosmicPlayer player = createPlayer(uuid).augment(loader);

        return loadPlayer(player);
    }

    public static CosmicPlayer getOrCreatePlayer(String uuid) {
        CosmicSender sender = getOrCreateSender(uuid);
        if (sender instanceof CosmicPlayer) return (CosmicPlayer) sender;

        CompletableFuture<Optional<CosmicPlayer>> loader = Singularity.getMainDatabase().loadPlayer(uuid);

        CosmicPlayer player = createPlayer(uuid).augment(loader);

        return loadPlayer(player);
    }

    public static boolean isConsole(String uuid) {
        return uuid.equals(GivenConfigs.getMainConfig().getConsoleDiscriminator());
    }

    public static String getOffOnFormatted(CosmicSender stat) {
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat.isOnline()) {
            return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOnlineName());
        } else {
            return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOfflineName());
        }
    }

    public static String getOffOnAbsolute(CosmicSender stat) {
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat.isOnline()) {
            return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOnlineName());
        } else {
            return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOfflineName());
        }
    }

    public static String getFormatted(CosmicSender stat) {
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }
        return stat.getDisplayName();
    }

    public static String getAbsolute(CosmicSender stat) {
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

    public static String getPrefix(CosmicSender user) {
        if (! (user instanceof CosmicPlayer)) return user.getMeta().getPrefix();
        CosmicPlayer player = (CosmicPlayer) user;

        Optional<MetaValue> optional = PermissionUtil.getPrefix(player);
        if (optional.isEmpty()) return user.getMeta().getPrefix();

        return optional.get().getValue();
    }

    public static String getSuffix(CosmicSender user) {
        if (! (user instanceof CosmicPlayer)) return user.getMeta().getSuffix();
        CosmicPlayer player = (CosmicPlayer) user;

        Optional<MetaValue> optional = PermissionUtil.getSuffix(player);
        if (optional.isEmpty()) return user.getMeta().getSuffix();

        return optional.get().getValue();
    }

    public static String formatName(CosmicPlayer user) {
        return ModuleUtils.replacePlaceholders(user, user.getMeta().getFull());
    }

    public static String getFormattedDefaultNickname(CosmicPlayer user) {
        return ModuleUtils.replacePlaceholders(user, user.getMeta().getFull());
    }

    public static boolean runAs(CosmicSender user, String command) {
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

    public static Optional<CosmicSender> getOrCreateSenderByName(String name) {
        Optional<String> uuid = getUUIDFromName(name);
        if (uuid.isEmpty()) return Optional.empty();

        return Optional.of(getOrCreateSender(uuid.get()));

    }

    public static Optional<CosmicPlayer> getOrCreatePlayerByName(String name) {
        Optional<String> uuid = getUUIDFromName(name);
        if (uuid.isEmpty()) return Optional.empty();

        return Optional.of(getOrCreatePlayer(uuid.get()));
    }

    public static CosmicPlayer getOrCreatePlayerByNameNullable(String name) {
        return getOrCreatePlayerByName(name).orElse(null);
    }

    public static CosmicSender getOrCreateSenderByNameNullable(String name) {
        return getOrCreateSenderByName(name).orElse(null);
    }

    public static ConcurrentSkipListSet<CosmicPlayer> getPlayersOn(String server) {
        ConcurrentSkipListSet<CosmicPlayer> r = new ConcurrentSkipListSet<>();

        getOnlinePlayers().forEach((s, user) -> {
            if (user.getServer().getIdentifier().equals(server)) r.add(user);
        });

        return r;
    }

    public static boolean isGeyserPlayer(CosmicPlayer user) {
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
