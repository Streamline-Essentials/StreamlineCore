package host.plas.commands;

import host.plas.database.MyLoader;
import lombok.Getter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.ModuleUtils;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.CosmicPlayer;
import singularity.data.console.CosmicSender;
import singularity.utils.UserUtils;
import host.plas.StreamlineUtilities;
import host.plas.essentials.configured.ConfiguredBlacklist;
import host.plas.essentials.configured.ConfiguredPermissionsList;
import host.plas.essentials.users.StreamlineHome;
import host.plas.essentials.users.UtilitiesUser;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class SetHomeCommand extends ModuleCommand {
    private final String messageResultSet;
    private final String messageResultReplaced;

    private final String messageResultSetOther;
    private final String messageResultReplacedOther;

    private final String messageResultTooManyHomes;
    private final String messageResultBlacklistedWorld;
    private final String messageResultBlacklistedServer;

    private final String permissionSetHomeOther;

    public SetHomeCommand() {
        super(StreamlineUtilities.getInstance(),
                "streamline-sethome",
                "streamline.command.sethome.default",
                "ssethome", "stsh"
        );

        messageResultSet = getCommandResource().getOrSetDefault("messages.result.set", "&eSet home &7'&c%this_input%&7'&8!");
        messageResultReplaced = getCommandResource().getOrSetDefault("messages.result.replaced", "&eReplaced home &7'&c%this_input%&7'&8!");

        messageResultSetOther = getCommandResource().getOrSetDefault("messages.result.set-other", "&eSet home &7'&c%this_input%&7' &efor &7'&c%this_other%&7'&8!");
        messageResultReplacedOther = getCommandResource().getOrSetDefault("messages.result.replaced-other", "&eReplaced home &7'&c%this_input%&7' &efor &7'&c%this_other%&7'&8!");

        messageResultTooManyHomes = getCommandResource().getOrSetDefault("messages.result.too-many-homes", "&cYou have too many homes, so you cannot set another one&8!");
        messageResultBlacklistedWorld = getCommandResource().getOrSetDefault("messages.result.blocked.world", "&cYou cannot set a home in this world&8!");
        messageResultBlacklistedServer = getCommandResource().getOrSetDefault("messages.result.blocked.server", "&cYou cannot set a home on this server&8!");

        permissionSetHomeOther = getCommandResource().getOrSetDefault("permissions.sethome.other", "streamline.command.sethome.other");
    }

    @Override
    public void run(CosmicSender sender, String[] strings) {
        String homeName = "home";
        CosmicSender targetUser = null;
        UtilitiesUser target = null;

        if (strings.length <= 1) {
            target = MyLoader.getInstance().getOrCreate(sender.getUuid());
            targetUser = sender;
        }

        if (strings.length == 1) {
            homeName = strings[0];
        }

        if (strings.length == 2) {
            if (sender.hasPermission(permissionSetHomeOther)) {
                targetUser = UserUtils.getOrCreateSenderByNameNullable(strings[0]);
                if (targetUser == null) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                target = MyLoader.getInstance().getOrCreate(targetUser.getUuid());
            } else {
                ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                return;
            }
        }

        if (strings.length > 2) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_MANY.get());
            return;
        }

        if (target == null) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        if (! (sender instanceof CosmicPlayer)) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_SELF.get());
            return;
        }

        ConfiguredPermissionsList configuredPermissionsList = StreamlineUtilities.getConfigs().getHomesPermissions();

        Optional<StreamlineHome> optional = target.getHome(homeName);

        if (configuredPermissionsList != null) {
            if (! configuredPermissionsList.hasPermission(targetUser, target.getHomesCount() + (optional.isEmpty() ? 1 : 0))) {
                ModuleUtils.sendMessage(sender, getWithOther(sender, messageResultTooManyHomes, targetUser));
                return;
            }
        }

        CosmicPlayer player = (CosmicPlayer) sender;
        CosmicLocation location = player.getLocation();

        ConfiguredBlacklist configuredBlacklist = StreamlineUtilities.getConfigs().getHomesBlacklist();

        if (configuredBlacklist != null) {
            AtomicBoolean isServerBlacklisted = new AtomicBoolean(false);
            configuredBlacklist.getServers().forEach(server -> {
                if (configuredBlacklist.isAsWhitelist()) {
                    if (! server.equals(player.getServerName())) {
                        isServerBlacklisted.set(true);
                    }
                } else {
                    if (server.equals(player.getServerName())) {
                        isServerBlacklisted.set(true);
                    }
                }
            });
            AtomicBoolean isWorldBlacklisted = new AtomicBoolean(false);
            configuredBlacklist.getWorlds().forEach(world -> {
                if (configuredBlacklist.isAsWhitelist()) {
                    if (! world.equals(location.getWorldName())) {
                        isWorldBlacklisted.set(true);
                    }
                } else {
                    if (world.equals(location.getWorldName())) {
                        isWorldBlacklisted.set(true);
                    }
                }
            });

            if (isServerBlacklisted.get()) {
                ModuleUtils.sendMessage(sender, getWithOther(sender, messageResultBlacklistedServer, targetUser));
                return;
            }

            if (isWorldBlacklisted.get()) {
                ModuleUtils.sendMessage(sender, getWithOther(sender, messageResultBlacklistedWorld, targetUser));
                return;
            }
        }

        StreamlineHome streamlineHome = new StreamlineHome(homeName, player.getServerName(), location.getWorldName(),
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        if (optional.isPresent()) {
            target.removeHome(homeName);
            target.addHome(streamlineHome);

            if (! target.getUuid().equals(player.getUuid())) {
                ModuleUtils.sendMessage(sender, getWithOther(player, messageResultReplacedOther
                        .replace("%this_input%", homeName), targetUser)
                );
            } else {
                ModuleUtils.sendMessage(sender, getWithOther(player, messageResultReplaced
                        .replace("%this_input%", homeName), targetUser)
                );
            }

            target.save();
        } else {
            target.addHome(streamlineHome);

            if (! target.getUuid().equals(player.getUuid())) {
                ModuleUtils.sendMessage(sender, getWithOther(player, messageResultSetOther
                        .replace("%this_input%", homeName), targetUser)
                );
            } else {
                ModuleUtils.sendMessage(sender, getWithOther(player, messageResultSet
                        .replace("%this_input%", homeName), targetUser)
                );
            }

            target.save();
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        if (strings.length == 2) {
            if (ModuleUtils.hasPermission(CosmicSender, permissionSetHomeOther)) {
                return ModuleUtils.getOnlinePlayerNames();
            }
        }

        return new ConcurrentSkipListSet<>();
    }
}
