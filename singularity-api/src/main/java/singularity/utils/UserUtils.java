package singularity.utils;

import gg.drak.thebase.async.AsyncUtils;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.*;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.*;
import singularity.data.players.location.CosmicLocation;
import singularity.data.teleportation.TPTicket;
import singularity.data.uuid.UuidManager;
import singularity.modules.ModuleUtils;
import singularity.permissions.MetaValue;
import singularity.permissions.PermissionUtil;

import java.util.Optional;
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
        if (isLoaded(sender.getUuid())) unloadSender(sender.getUuid()); // unload the sender if it's already loaded

        getLoadedSenders().put(sender.getUuid(), sender);

        if (sender instanceof CosmicPlayer) new LoadPlayerEvent((CosmicPlayer) sender).fire();
        else ModuleUtils.fireEvent(new LoadSenderEvent(sender));

        return sender;
    }

    public static void unloadSender(CosmicSender user) {
        unloadSender(user.getUuid());
    }

    public static void saveSender(CosmicSender sender, boolean async) {
        if (sender == null) return;

        Singularity.getMainDatabase().saveSender(sender, async);
    }

    public static void unloadSender(String uuid) {
        CosmicSender sender = getLoadedSenders().remove(uuid);
        if (sender == null) return;

        if (sender instanceof CosmicPlayer) new UnloadPlayerEvent((CosmicPlayer) sender).fire();
        else new UnloadSenderEvent(sender).fire();
    }

    public static void deleteSender(String uuid) {
        CosmicSender sender = getOrGetSender(uuid).orElse(null);
        if (sender == null) return;

        AsyncUtils.executeAsync(() -> {
            Singularity.getMainDatabase().delete(uuid, false);

            if (sender instanceof CosmicPlayer) new DeletePlayerEvent((CosmicPlayer) sender).fire();
            else new DeleteSenderEvent(sender).fire();
        });
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
        if (UuidUtils.isConsole(uuid)) return Optional.of(getConsole());

        CosmicSender sender = getLoadedSenders().get(uuid);
        if (sender == null) return Optional.empty();

        if (sender instanceof CosmicPlayer) {
            CosmicPlayer player = (CosmicPlayer) sender;
            return Optional.of(player);
        }

        return Optional.of(sender);
    }

    public static Optional<CosmicPlayer> getPlayer(String uuid) {
        Optional<CosmicSender> optional = getSender(uuid);
        if (optional.isPresent()) {
            if (optional.get() instanceof CosmicPlayer) return optional.map(s -> (CosmicPlayer) s);
        }

        return Optional.empty();
    }

    public static CosmicPlayer loadPlayer(CosmicPlayer player) {
        return (CosmicPlayer) loadSender(player);
    }

    public static Optional<CosmicPlayer> getOrCreatePlayer(CosmicSender sender) {
        return getOrCreatePlayer(sender.getUuid());
    }

    public static CosmicSender createSender() {
        return new CosmicSender();
    }

    public static CosmicSender createSender(String uuid) {
        return new CosmicSender(uuid);
    }

    public static CosmicPlayer createPlayer(String uuid) {
        return new CosmicPlayer(uuid);
    }

    public static Optional<CosmicSender> getOrCreateSender(String uuid) {
        Optional<CosmicSender> optional = getOrGetSender(uuid);
        if (optional.isPresent()) return optional;

        if (isConsole(uuid)) return Optional.ofNullable(getConsole());
        if (! UuidUtils.isValidPlayerUUID(uuid)) return Optional.empty();

        CosmicPlayer player = createPlayer(uuid);
        player.load();

        player.augment(Singularity.getMainDatabase().loadPlayer(uuid), false);

        return Optional.of(player);
    }

    public static Optional<CosmicPlayer> getOrCreatePlayer(String uuid) {
        Optional<CosmicSender> sender = getOrCreateSender(uuid);
        if (sender.isPresent()) {
            if (sender.get() instanceof CosmicPlayer) return sender.map(s -> (CosmicPlayer) s);
        }

        Optional<CosmicPlayer> optional = getOrGetPlayer(uuid);
        if (optional.isPresent()) return optional;

        if (! UuidUtils.isValidPlayerUUID(uuid)) return Optional.empty();

        CosmicPlayer player = createPlayer(uuid);
        player.load();

        player.augment(Singularity.getMainDatabase().loadPlayer(uuid), false);

        return Optional.of(player);
    }

    public static CosmicSender createTemporarySender(String uuid) {
        return new CosmicSender(uuid, true);
    }

    public static CosmicPlayer createTemporaryPlayer(String uuid) {
        return new CosmicPlayer(uuid, true);
    }

    public static Optional<CosmicSender> getOrGetSender(String uuid) {
        if (uuid == null || uuid.isEmpty()) return Optional.empty();

        if (UuidUtils.isConsole(uuid)) return Optional.of(getConsole());

        Optional<CosmicSender> optional = getSender(uuid);
        if (optional.isPresent()) return optional;

        if (! UuidUtils.isValidPlayerUUID(uuid)) return Optional.empty();

        CosmicPlayer player = createTemporaryPlayer(uuid);
        player.load();

        player.augment(Singularity.getMainDatabase().loadPlayer(uuid), true);

        return Optional.of(player);
    }

    public static Optional<CosmicPlayer> getOrGetPlayer(String uuid) {
        Optional<CosmicSender> optional = getOrGetSender(uuid);
        if (optional.isPresent()) {
            if (optional.get() instanceof CosmicPlayer) return optional.map(s -> (CosmicPlayer) s);
        }

        return Optional.empty();
    }

    public static boolean isConsole(String uuid) {
        if (getConsole() != null) return getConsole().getIdentifier().equals(uuid);

        return UuidUtils.isConsole(uuid);
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

    public static Optional<CosmicSender> getOrGetSenderByName(String name) {
        Optional<String> uuid = getUUIDFromName(name);
        if (uuid.isEmpty()) return Optional.empty();

        return getOrGetSender(uuid.get());
    }

    public static Optional<CosmicPlayer> getOrGetPlayerByName(String name) {
        Optional<CosmicSender> sender = getOrGetSenderByName(name);
        if (sender.isEmpty()) return Optional.empty();
        if (sender.get() instanceof CosmicPlayer) {
            return Optional.of((CosmicPlayer) sender.get());
        }

        return Optional.empty();
    }

    public static Optional<CosmicSender> getOrCreateSenderByName(String name) {
        Optional<CosmicSender> optional = getOrGetSenderByName(name);
        if (optional.isPresent()) return optional;

        Optional<String> uuid = getUUIDFromName(name);
        if (uuid.isEmpty()) return Optional.empty();

        CosmicSender sender = createSender(uuid.get());
        sender.load();

        sender.augment(Singularity.getMainDatabase().loadPlayer(uuid.get()), false);

        return Optional.of(sender);
    }

    public static Optional<CosmicPlayer> getOrCreatePlayerByName(String name) {
        Optional<CosmicPlayer> optional = getOrGetPlayerByName(name);
        if (optional.isPresent()) return optional;

        Optional<String> uuid = getUUIDFromName(name);
        if (uuid.isEmpty()) return Optional.empty();

        CosmicPlayer player = createPlayer(uuid.get());
        player.load();

        player.augment(Singularity.getMainDatabase().loadPlayer(uuid.get()), false);

        return Optional.of(player);
    }

    public static CosmicPlayer getOrGetPlayerByNameNullable(String name) {
        return getOrGetPlayerByName(name).orElse(null);
    }

    public static CosmicSender getOrGetSenderByNameNullable(String name) {
        return getOrGetSenderByName(name).orElse(null);
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

    public static void teleport(CosmicSender sender, CosmicPlayer target) {
        teleport(sender, target.getLocation());
    }

    public static void teleport(CosmicSender sender, CosmicLocation location) {
        if (sender.isConsole()) {
            MessageUtils.logWarning("Console attempted to teleport to " + location.asString());
            return;
        }

        TPTicket ticket = new TPTicket(sender.getIdentifier(), location);
        ticket.post();
    }
}
