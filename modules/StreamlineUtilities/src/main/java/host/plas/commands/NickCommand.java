package host.plas.commands;

import host.plas.utils.NickUtils;
import lombok.Getter;
import net.streamline.api.SLAPI;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.players.meta.SenderMeta;
import singularity.interfaces.ISingularityExtension;
import singularity.messages.proxied.ProxiedMessage;
import singularity.modules.ModuleUtils;
import singularity.data.players.CosmicPlayer;
import singularity.data.console.CosmicSender;
import host.plas.StreamlineUtilities;
import host.plas.accessors.SpigotAccessor;
import host.plas.events.NicknameUpdateEvent;
import singularity.utils.UserUtils;

import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class NickCommand extends ModuleCommand {
    private final String messageResultCleared;
    private final String messageResultChanged;
    private final String messageResultCancelled;
    private final String permissionSetOthers;
    private final String permissionSetNonFormatted;

    public NickCommand() {
        super(StreamlineUtilities.getInstance(),
                "proxynickname",
                "streamline.command.nickname.default",
                "pn", "proxyn", "pnick", "pnickname"
        );

        messageResultCleared = getCommandResource().getOrSetDefault("messages.result.cleared",
                "&eChanged &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&8'&es nickname to &b'%this_new%&b' &7(&efrom &b'%this_previous%&b'&7)");
        messageResultChanged = getCommandResource().getOrSetDefault("messages.result.changed",
                "&eChanged &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&8'&es nickname to &b'%this_new%&b' &7(&efrom &b'%this_previous%&b'&7)");
        messageResultCancelled = getCommandResource().getOrSetDefault("messages.result.cancelled",
                "&cDid not change your nickname because a plugin / module cancelled it.");
        permissionSetOthers = getCommandResource().getOrSetDefault("permission.set.others", "streamline.command.nickname.set.others");
        permissionSetNonFormatted = getCommandResource().getOrSetDefault("permission.set.non-formatted", "streamline.command.nickname.set.non-formatted");
    }

    @Override
    public void run(CommandContext<CosmicCommand> ctx) {
        String[] strings = ctx.getArgsArray();
        CosmicSender s = ctx.getSender();

//        if (! (s instanceof CosmicPlayer)) {
//            ctx.sendMessage("&cOnly players can use this command.");
//            return;
//        }
//        CosmicPlayer sender = (CosmicPlayer) s;

        String message = ModuleUtils.argsToString(strings);

        CosmicSender user = s;

        if (message.startsWith("-p:")) {
            if (user.hasPermission(getPermissionSetOthers())) {
                String name = strings[0].substring("-p:".length());
                user = UserUtils.getOrGetPlayerByNameNullable(name);
                message = message.substring(message.indexOf(' ') + 1);
            }
        }

        if (user == null) {
            ctx.sendMessage(MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        if (message.isBlank()) {
            strings = new String[]{"-clear"};
            message = ModuleUtils.argsToString(strings);
        }

        if (strings.length <= 2) {
            if (strings[strings.length - 1].equals("-clear")) {
                String current = user.getDisplayName();
                final CosmicSender finalUser = user;
                NickUtils.handleNickChange(user, message, current, (newNick, previous) -> {
                    ctx.sendMessage(getWithOther(s, getMessageResultCancelled(), finalUser)
                            .replace("%this_new%", newNick)
                            .replace("%this_previous%", previous)
                    );
                });

                ctx.sendMessage(getWithOther(s, getMessageResultCleared(), user)
                        .replace("%this_new%", ModuleUtils.replaceAllPlayerBungee(user, "%streamline_user_formatted%"))
                        .replace("%this_previous%", current)
                );
                return;
            }
        }

        if (message.startsWith("!") && s.hasPermission(getPermissionSetNonFormatted())) {
            message = message.substring("!".length());
        } else {
            message = StreamlineUtilities.getConfigs().getNicknamesFormat().replace("%this_input%", message);
        }

        String current = user.getDisplayName();
        final CosmicSender finalUser = user;
        NickUtils.handleNickChange(user, message, current, (newNick, previous) -> {
            ctx.sendMessage(getWithOther(s, getMessageResultCancelled(), finalUser)
                    .replace("%this_new%", newNick)
                    .replace("%this_previous%", previous)
            );
        });

        ctx.sendMessage(getWithOther(s, getMessageResultChanged(), user)
                .replace("%this_new%", message)
                .replace("%this_previous%", current)
        );
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        if (strings.length == 1) {
            if (strings[0].startsWith("-p:")) {
                ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

                ModuleUtils.getOnlinePlayerNames().forEach(s -> {
                    r.add("-p:" + s);
                    r.add("!" + s);
                });
                r.add("-clear");
                r.add("!");

                return r;
            }

            ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

            r.addAll(ModuleUtils.getOnlinePlayerNames());

            ModuleUtils.getOnlinePlayerNames().forEach(s -> {
                r.add("!" + s);
            });

            r.add("-clear");
            r.add("!");

            return r;
        }

        if (strings.length == 2) {
            if (strings[0].startsWith("-p:")) {
                ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

                r.addAll(ModuleUtils.getOnlinePlayerNames());
                r.add("-clear");

                return r;
            }
        }

        if (strings.length >= 2) {
            if (strings[strings.length - 2].equals("-clear")) {
                return new ConcurrentSkipListSet<>();
            }
        }

        return ModuleUtils.getOnlinePlayerNames();
    }
}
