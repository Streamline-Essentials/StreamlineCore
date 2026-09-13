package host.plas.commands;

import host.plas.database.MyLoader;
import lombok.Getter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;
import host.plas.StreamlineUtilities;
import host.plas.essentials.EssentialsManager;
import host.plas.essentials.users.StreamlineHome;
import host.plas.essentials.users.UtilitiesUser;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class DeleteHomeCommand extends ModuleCommand {
    private final String messageResultRemove;

    private final String messageResultRemoveOther;

    private final String messageResultHomeNotExists;

    private final String permissionRemoveOther;

    public DeleteHomeCommand() {
        super(StreamlineUtilities.getInstance(),
                "streamline-delhome",
                "streamline.command.delhome.default",
                "sdelhome", "stdh"
        );

        messageResultRemove = getCommandResource().getOrSetDefault("messages.result.remove", "&eRemoved set home &7'&c%this_input%&7'&8!");

        messageResultRemoveOther = getCommandResource().getOrSetDefault("messages.result.remove-other", "&eRemoved set home &7'&c%this_input%&7' &efor &7'&c%this_other%&7'&8!");

        messageResultHomeNotExists = getCommandResource().getOrSetDefault("messages.result.home-not-exists", "&cHome &7'&c%this_input%&7' &cdoes not exists&8!");

        permissionRemoveOther = getCommandResource().getOrSetDefault("permissions.delhome.other", "streamline.command.delhome.other");
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
            if (ModuleUtils.hasPermission(sender, permissionRemoveOther)) {
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

        CosmicPlayer player = (CosmicPlayer) sender;

        Optional<StreamlineHome> optional = target.getHome(homeName);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, getWithOther(player, messageResultHomeNotExists
                    .replace("%this_input%", homeName), targetUser)
            );
            return;
        }

        target.removeHome(homeName);

        if (targetUser.equals(sender)) {
            ModuleUtils.sendMessage(sender, getWithOther(player, messageResultRemove
                    .replace("%this_input%", homeName), targetUser)
            );
        } else {
            ModuleUtils.sendMessage(sender, getWithOther(player, messageResultRemoveOther
                    .replace("%this_input%", homeName)
                    .replace("%this_other%", targetUser.getCurrentName()), targetUser)
            );
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        if (strings.length == 2) {
            if (ModuleUtils.hasPermission(CosmicSender, permissionRemoveOther)) {
                return ModuleUtils.getOnlinePlayerNames();
            }
        }

        return new ConcurrentSkipListSet<>();
    }
}
