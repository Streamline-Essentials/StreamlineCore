package host.plas.commands;

import host.plas.database.MyLoader;
import lombok.Getter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.ModuleUtils;
import singularity.data.players.CosmicPlayer;
import singularity.data.console.CosmicSender;
import singularity.utils.UserUtils;
import host.plas.StreamlineUtilities;
import host.plas.essentials.EssentialsManager;
import host.plas.essentials.configured.ConfiguredBlacklist;
import host.plas.essentials.users.StreamlineHome;
import host.plas.essentials.users.UtilitiesUser;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class HomeCommand extends ModuleCommand {
    private final String messageResult;

    private final String messageResultOther;

    private final String messageResultHomeNotExists;
    private final String messageResultBlacklistedServer;
    private final String messageResultBlacklistedWorld;

    private final String permissionOther;

    public HomeCommand() {
        super(StreamlineUtilities.getInstance(),
                "streamline-home",
                "streamline.command.home.default",
                "shome", "sth"
        );

        messageResult = getCommandResource().getOrSetDefault("messages.result.home", "&eTeleporting to home &7'&c%this_input%&7'&8!");

        messageResultOther = getCommandResource().getOrSetDefault("messages.result.home-other", "&eTeleporting to home &7'&c%this_input%&7' &efor &7'&c%this_other%&7'&8!");

        messageResultHomeNotExists = getCommandResource().getOrSetDefault("messages.result.home-not-exists", "&cHome &7'&c%this_input%&7' &cdoes not exists&8!");

        messageResultBlacklistedServer = getCommandResource().getOrSetDefault("messages.result.blocked.server", "&cYou cannot teleport to this server&8!");

        messageResultBlacklistedWorld = getCommandResource().getOrSetDefault("messages.result.blocked.world", "&cYou cannot teleport to this world&8!");

        permissionOther = getCommandResource().getOrSetDefault("permissions.home.other", "streamline.command.home.other");
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
            if (ModuleUtils.hasPermission(sender, permissionOther)) {
                targetUser = UserUtils.getOrCreatePlayerByName(strings[0]).orElse(null);
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

        if (! target.hasHome(homeName)) {
            ModuleUtils.sendMessage(sender, getWithOther(sender, messageResultHomeNotExists
                    .replace("%this_input%", homeName), targetUser)
            );
            return;
        }

        CosmicPlayer player = (CosmicPlayer) sender;

        Optional<StreamlineHome> optional = target.getHome(homeName);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, getWithOther(player, messageResultHomeNotExists
                    .replace("%this_input%", homeName), targetUser)
            );
            return;
        }
        StreamlineHome home = optional.get();

        ConfiguredBlacklist configuredBlacklist = StreamlineUtilities.getConfigs().getHomesBlacklist();

        if (configuredBlacklist != null) {
            AtomicBoolean isServerBlacklisted = new AtomicBoolean(false);
            configuredBlacklist.getServers().forEach(server -> {
                if (configuredBlacklist.isAsWhitelist()) {
                    if (! server.equals(home.getServer().getIdentifier())) {
                        isServerBlacklisted.set(true);
                    }
                } else {
                    if (server.equals(home.getServer().getIdentifier())) {
                        isServerBlacklisted.set(true);
                    }
                }
            });
            AtomicBoolean isWorldBlacklisted = new AtomicBoolean(false);
            configuredBlacklist.getWorlds().forEach(world -> {
                if (configuredBlacklist.isAsWhitelist()) {
                    if (!world.equals(home.getWorld())) {
                        isWorldBlacklisted.set(true);
                    }
                } else {
                    if (world.equals(home.getWorld())) {
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

        CosmicPlayer targetPlayer = (CosmicPlayer) sender;

        home.teleport(targetPlayer);

        if (targetUser.equals(sender)) {
            ModuleUtils.sendMessage(sender, getWithOther(player, messageResult
                    .replace("%this_input%", homeName), targetUser)
            );
        } else {
            ModuleUtils.sendMessage(sender, getWithOther(player, messageResultOther
                    .replace("%this_input%", homeName)
                    .replace("%this_other%", targetUser.getCurrentName()), targetUser)
            );
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        if (strings.length == 2) {
            if (ModuleUtils.hasPermission(CosmicSender, permissionOther)) {
                return ModuleUtils.getOnlinePlayerNames();
            }
        }

        return new ConcurrentSkipListSet<>();
    }
}
