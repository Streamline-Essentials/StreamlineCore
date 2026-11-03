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

import java.util.Arrays;
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

        CosmicSender user = s;
        int start = 0;

        if (strings.length > 0 && strings[0].startsWith("-p:")) {
            if (! s.hasPermission(getPermissionSetOthers())) {
                ctx.sendMessage(MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                return;
            }
            user = UserUtils.getOrGetPlayerByNameNullable(strings[0].substring("-p:".length()));
            start = 1;
        }

        if (user == null) {
            ctx.sendMessage(MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        String[] nickArgs = Arrays.copyOfRange(strings, start, strings.length);
        String message = ModuleUtils.argsToString(nickArgs);

        // "-clear", no nickname at all, or a bare "!" all reset the nickname to the username.
        boolean clear = message.isBlank() || message.equals("-clear") || message.equals("!");

        String nickname;
        if (clear) {
            nickname = "";
        } else if (message.startsWith("!") && s.hasPermission(getPermissionSetNonFormatted())) {
            nickname = message.substring("!".length());
        } else {
            nickname = StreamlineUtilities.getConfigs().getNicknamesFormat().replace("%this_input%", message);
        }

        String previous = NickUtils.currentNickname(user);
        String applied = NickUtils.handleNickChange(user, nickname, previous);

        if (applied == null) {
            ctx.sendMessage(getWithOther(s, getMessageResultCancelled(), user)
                    .replace("%this_new%", nickname)
                    .replace("%this_previous%", previous)
            );
            return;
        }

        ctx.sendMessage(getWithOther(s, applied.isBlank() ? getMessageResultCleared() : getMessageResultChanged(), user)
                .replace("%this_new%", NickUtils.currentNickname(user))
                .replace("%this_previous%", previous)
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
