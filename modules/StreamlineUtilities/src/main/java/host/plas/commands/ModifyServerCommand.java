package host.plas.commands;

import gg.drak.thebase.utils.StringUtils;
import lombok.Getter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.ModuleUtils;
import singularity.data.console.CosmicSender;
import host.plas.StreamlineUtilities;
import host.plas.configs.obj.ConfiguredServer;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class ModifyServerCommand extends ModuleCommand {
    private final String messageResultAddedAlias;
    private final String messageResultRemovedAlias;
    private final String messageResultRemovedServer;
    private final String messageResultSetPrettyName;

    private final String messageResultNotAServer;
    private final String messageResultAlreadyHasAlias;
    private final String messageResultDoesNotHaveAlias;

    private final String permissionAddAlias;
    private final String permissionRemoveAlias;
    private final String permissionRemoveServer;
    private final String permissionSetPrettyName;

    public ModifyServerCommand() {
        super(StreamlineUtilities.getInstance(),
                "modify-server",
                "streamline.command.modify-server.default",
                "mserver", "updateserver", "modserver", "modifysrv", "modifysrvr", "modifysrvr"
        );

        messageResultAddedAlias = getCommandResource().getOrSetDefault("messages.result.alias.added", "&eAdded alias &a%this_other% &eto &7'&c%this_input%&7'&8!");
        messageResultRemovedAlias = getCommandResource().getOrSetDefault("messages.result.alias.removed", "&eRemoved alias &a%this_other% &efrom &7'&c%this_input%&7'&8!");
        messageResultRemovedServer = getCommandResource().getOrSetDefault("messages.result.removed", "&eRemoved server &7'&c%this_input%&7'&8!");
        messageResultSetPrettyName = getCommandResource().getOrSetDefault("messages.result.pretty_name.set", "&eSet pretty name of &7'&c%this_input%&7' &eto&8:&r %this_other%");

        messageResultNotAServer = getCommandResource().getOrSetDefault("messages.result.not_a_server", "&7'&a%this_input%&7' &cis not a valid server!");
        messageResultAlreadyHasAlias = getCommandResource().getOrSetDefault("messages.result.already_has_alias", "&7'&a%this_input%&7' &cis already an alias for &7'&a%this_other%&7'&8!");
        messageResultDoesNotHaveAlias = getCommandResource().getOrSetDefault("messages.result.does_not_have_alias", "&7'&a%this_input%&7' &cis not an alias for &7'&a%this_other%&7'&8!");

        permissionAddAlias = getCommandResource().getOrSetDefault("permissions.add-alias", "streamline.command.modify-server.add-alias");
        permissionRemoveAlias = getCommandResource().getOrSetDefault("permissions.remove-alias", "streamline.command.modify-server.remove-alias");
        permissionRemoveServer = getCommandResource().getOrSetDefault("permissions.remove-server", "streamline.command.modify-server.remove-server");
        permissionSetPrettyName = getCommandResource().getOrSetDefault("permissions.set-pretty-name", "streamline.command.modify-server.set-pretty-name");
    }

    @Override
    public void run(CosmicSender sender, String[] strings) {
        if (strings.length < 2) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String action = strings[0];

        String serverName = strings[1];
        ConfiguredServer server = StreamlineUtilities.getServersConfig().getServer(serverName);
        if (server == null) {
            ModuleUtils.sendMessage(sender,
                    getWithOther(sender, messageResultNotAServer.replace("%this_input%", serverName),
                            sender));
            return;
        }

        switch (action) {
            case "add-alias":
                if (! ModuleUtils.hasPermission(sender, permissionAddAlias)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                    return;
                }

                if (strings.length < 3) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                String alias = strings[2];

                if (server.getAliases().contains(alias)) {
                    ModuleUtils.sendMessage(sender, getWithOther(sender,
                            messageResultAlreadyHasAlias.replace("%this_input%", alias).replace("%this_other%", serverName),
                            sender));
                    return;
                }

                server.addAlias(alias);
                server.save();

                ModuleUtils.sendMessage(sender, getWithOther(sender,
                        messageResultAddedAlias.replace("%this_input%", serverName).replace("%this_other%", alias),
                        sender));
                break;
            case "remove-alias":
                if (! ModuleUtils.hasPermission(sender, permissionRemoveAlias)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                    return;
                }

                if (strings.length < 3) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                String alias2 = strings[2];

                if (!server.getAliases().contains(alias2)) {
                    ModuleUtils.sendMessage(sender, getWithOther(sender,
                            messageResultDoesNotHaveAlias.replace("%this_input%", alias2).replace("%this_other%", serverName),
                            sender));
                    return;
                }

                server.removeAlias(alias2);
                server.save();

                ModuleUtils.sendMessage(sender, getWithOther(sender,
                        messageResultRemovedAlias.replace("%this_input%", serverName).replace("%this_other%", alias2),
                        sender));
                break;
            case "remove":
                if (! ModuleUtils.hasPermission(sender, permissionRemoveServer)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                    return;
                }

                server.remove();
                ModuleUtils.sendMessage(sender, getWithOther(sender,
                        messageResultRemovedServer.replace("%this_input%", serverName),
                        sender));
                break;
            case "set-pretty-name":
                if (! ModuleUtils.hasPermission(sender, permissionSetPrettyName)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                    return;
                }

                if (strings.length < 3) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                String prettyName = StringUtils.argsToStringMinus(strings, 0, 1);

                server.setPrettyName(prettyName);
                server.save();

                ModuleUtils.sendMessage(sender, getWithOther(sender,
                        messageResultSetPrettyName.replace("%this_input%", serverName).replace("%this_other%", prettyName),
                        sender));
                break;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        if (strings.length <= 1) {
            return new ConcurrentSkipListSet<>(List.of("add-alias", "remove-alias", "remove", "set-pretty-name"));
        }
        if (strings.length == 2) {
            return new ConcurrentSkipListSet<>(StreamlineUtilities.getServersConfig().getLoadedServers().keySet());
        }
        return new ConcurrentSkipListSet<>();
    }
}
