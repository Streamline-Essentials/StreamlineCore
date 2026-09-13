package host.plas.commands;

import lombok.Getter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.ModuleUtils;
import singularity.data.console.CosmicSender;
import host.plas.StreamlineUtilities;
import host.plas.configs.obj.PermissionGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentSkipListSet;

public class OnlineCommand extends ModuleCommand {
    @Getter
    private final String messageResultGlobalBase;
    @Getter
    private final String messageResultGlobalModified;
    @Getter
    private final String messageResultExactBase;
    @Getter
    private final String messageResultExactModified;
    @Getter
    private final String messageResultPermissionedBase;
    @Getter
    private final String messageResultPermissionedModified;
    @Getter
    private final String messageErrorInvalidServer;
    @Getter
    private final String messageErrorInvalidGroup;
    @Getter
    private final String permissionServers;
    @Getter
    private final String permissionGroups;

    public OnlineCommand() {
        super(StreamlineUtilities.getInstance(),
                "online",
                "streamline.command.online.default",
                "on"
        );

        messageResultGlobalBase = getCommandResource().getOrSetDefault("messages.result.global.base",
                "&eOnline &7(&cTotal&7: &a%streamline_players_online%&7)&8: &r%this_online_global%");
        messageResultGlobalModified = getCommandResource().getOrSetDefault("messages.result.global.player.modified",
                "%streamline_parse_%this_username%:::*/*streamline_user_formatted*/* &7(&c*/*streamline_user_server*/*&7)%");

        messageResultExactBase = getCommandResource().getOrSetDefault("messages.result.exact.base",
                "&eOnline on &7'&c%this_identifier%&7'&8: &r%this_online_exact%");
        messageResultExactModified = getCommandResource().getOrSetDefault("messages.result.exact.player.modified",
                "%streamline_parse_%this_username%:::*/*streamline_user_formatted*/*%");

        messageResultPermissionedBase = getCommandResource().getOrSetDefault("messages.result.grouped.base",
                "&eOnline players for group &7'%utils_group_%this_identifier%_name%&7'&8: &r%this_online_permission%");
        messageResultPermissionedModified = getCommandResource().getOrSetDefault("messages.result.grouped.player.modified",
                "%streamline_parse_%this_username%:::*/*streamline_user_formatted*/* &7(&c*/*streamline_user_server*/*&7)%");

        messageErrorInvalidServer = getCommandResource().getOrSetDefault("messages.error.not-found.server",
                "&cThe server '%this_identifier%' does not exist!");
        messageErrorInvalidGroup = getCommandResource().getOrSetDefault("messages.error.not-found.group",
                "&cThe group '%this_identifier%' does not exist!");

        permissionServers = getCommandResource().getOrSetDefault("permissions.servers.base",
                "streamline.command.online.server.");
        permissionGroups = getCommandResource().getOrSetDefault("permissions.groups.base",
                "streamline.command.online.group.");
    }

    @Override
    public void run(CosmicSender CosmicSender, String[] strings) {
        if (strings[0].equals("")) {
            ModuleUtils.sendMessage(CosmicSender, getWithOther(CosmicSender,
                    messageResultGlobalBase
                            .replace("%this_online_global%", getOnlineGlobal())
                            .replace("%this_online_exact%", getOnlineExact(CosmicSender.getServerName()))
                            .replace("%this_online_permission%", getOnlineByPermission(CosmicSender.getServerName()))
                    ,
                    CosmicSender
            ));
            return;
        }
        if (strings.length > 2) {
            ModuleUtils.sendMessage(CosmicSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_MANY.get());
            return;
        }
        if (strings.length <= 1) {
            ModuleUtils.sendMessage(CosmicSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String option = strings[0];
        switch (option.toLowerCase(Locale.ROOT)) {
            case "server":
                String serverName = strings[1];
                if (! ModuleUtils.equalsAnyServer(serverName)) {
                    ModuleUtils.sendMessage(CosmicSender, messageErrorInvalidServer.replace("%this_identifier%", serverName));
                    return;
                }

                ModuleUtils.sendMessage(CosmicSender, getWithOther(CosmicSender,
                        messageResultExactBase
                                .replace("%this_identifier%", serverName)
                                .replace("%this_online_global%", getOnlineGlobal())
                                .replace("%this_online_exact%", getOnlineExact(serverName))
                                .replace("%this_online_permission%", getOnlineByPermission(serverName))
                        ,
                        CosmicSender
                ));
                break;
            case "group":
                String group = strings[1];
                if (! StreamlineUtilities.getGroupedPermissionConfig().getPermissionGroups().containsKey(group)) {
                    ModuleUtils.sendMessage(CosmicSender, messageErrorInvalidGroup.replace("%this_identifier%", group));
                    return;
                }

                ModuleUtils.sendMessage(CosmicSender, getWithOther(CosmicSender,
                        messageResultPermissionedBase
                                .replace("%this_identifier%", group)
                                .replace("%this_online_global%", getOnlineGlobal())
                                .replace("%this_online_exact%", getOnlineExact(group))
                                .replace("%this_online_permission%", getOnlineByPermission(group))
                        ,
                        CosmicSender
                ));
                break;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        if (strings.length <= 1) return new ConcurrentSkipListSet<>(List.of("server", "group"));
        if (strings.length > 2) return new ConcurrentSkipListSet<>();
        if (strings[0].equals("server")) {
            ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();
            ModuleUtils.getServerNames().forEach(a -> {
                if (ModuleUtils.hasPermission(CosmicSender, permissionServers + a)) r.add(a);
            });
            return r;
        }
        if (strings[0].equals("group")) {
            ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();
            new ArrayList<>(StreamlineUtilities.getGroupedPermissionConfig().getPermissionGroups().keySet()).forEach(a -> {
                if (ModuleUtils.hasPermission(CosmicSender, permissionServers + a)) r.add(a);
            });
            return r;
        }
        return new ConcurrentSkipListSet<>();
    }

    public String getOnlineGlobal() {
        List<String> modified = new ArrayList<>();
        ModuleUtils.getOnlinePlayerNames().forEach(a -> {
            modified.add(messageResultGlobalModified.replace("%this_username%", a));
        });

        return ModuleUtils.getListAsFormattedString(modified);
    }

    public String getOnlineExact(String serverName) {
        List<String> modified = new ArrayList<>();
        ModuleUtils.getUsersOn(serverName).forEach(a -> {
            modified.add(messageResultExactModified.replace("%this_username%", a.getCurrentName()));
        });

        return ModuleUtils.getListAsFormattedString(modified);
    }

    public String getOnlineByPermission(String identifier) {
        if (identifier == null) return "";
        PermissionGroup group = StreamlineUtilities.getGroupedPermissionConfig().getPermissionGroups().get(identifier);
        if (group == null) return "";

        List<String> modified = new ArrayList<>();
        ModuleUtils.getLoadedSendersSet().forEach(a -> {
            if (! a.isOnline()) return;
            if (! ModuleUtils.hasPermission(a, group.getPermission())) return;
            modified.add(messageResultPermissionedModified.replace("%this_username%", a.getCurrentName()));
        });

        return ModuleUtils.getListAsFormattedString(modified);
    }
}
