package host.plas.commands;

import lombok.Getter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;
import host.plas.StreamlineUtilities;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class ConnectCommand extends ModuleCommand {
    private final String messageResultConnecting;
    private final String messageResultSending;
    private final String messageResultNotAServer;

    public ConnectCommand() {
        super(StreamlineUtilities.getInstance(),
                "connect",
                "streamline.command.connect.default",
                "conn", "cnct"
        );

        messageResultConnecting = getCommandResource().getOrSetDefault("messages.result.connecting", "&eAttempting to &a&lconnect &eto &7'&c%this_input%&7' &d(&b%utils_server_pretty_name_%this_input%%&d)&8!");
        messageResultSending = getCommandResource().getOrSetDefault("messages.result.sending", "&eAttempting to &a&lconnect &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%" +
                " &eto &7'&c%this_input%&7' &d(&b%utils_server_pretty_name_%this_input%%&d)&8!");
        messageResultNotAServer = getCommandResource().getOrSetDefault("messages.result.not_a_server", "&7'&a%this_input%&7' &cis not a valid server!");
    }

    @Override
    public void run(CosmicSender sender, String[] strings) {
        if (strings.length < 1) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }
        if (strings.length > 3) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_MANY.get());
            return;
        }

        String serverName = strings[0];
        boolean silent = false;

        CosmicPlayer target = null;
        if (! (sender instanceof CosmicPlayer) && strings.length == 1) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_SELF.get());
            return;
        } else {
            if (sender instanceof CosmicPlayer) target = (CosmicPlayer) sender;
        }

        if (strings.length >= 2) {
            target = UserUtils.getOrCreatePlayerByName(strings[1]).orElse(null);
            if (target == null) {
                ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_OTHER.get());
                return;
            }
        }

        if (strings.length == 3) {
            try {
                silent = Boolean.parseBoolean(strings[2]);
            } catch (Exception e) {
                ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                return;
            }
        }

        String targetServer = StreamlineUtilities.getServersConfig().getActualName(serverName);
        if (targetServer == null) {
            ModuleUtils.sendMessage(sender, getWithOther(sender, messageResultNotAServer
                    .replace("%this_input%", serverName)
                    , target));
            return;
        }

        ModuleUtils.connect(target, targetServer);
        if (! silent) ModuleUtils.sendMessage(target, getWithOther(sender, messageResultConnecting
                        .replace("%this_input%", serverName)
                , target));
        if (target != sender) ModuleUtils.sendMessage(sender, getWithOther(sender, messageResultSending
                        .replace("%this_input%", serverName)
                , target));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        if (strings.length <= 1) {
            return StreamlineUtilities.getServersConfig().getPossibleNames();
        }
        if (strings.length == 2) {
            return ModuleUtils.getOnlinePlayerNames();
        }
        if (strings.length == 3) {
            return new ConcurrentSkipListSet<>(List.of("true", "false"));
        }
        return new ConcurrentSkipListSet<>();
    }
}
