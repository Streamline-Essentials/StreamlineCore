package host.plas.commands;

import host.plas.StreamlineGroups;
import host.plas.data.chats.ChatType;
import host.plas.data.player.GroupedPlayer;
import host.plas.database.PlayerLoader;
import lombok.Getter;
import singularity.command.ModuleCommand;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Routes a player's ordinary chat into a group.
 *
 * <p>{@code /groupchat (<party|guild|off>)} with no argument cycles between party chat and
 * normal chat; with an argument it selects that destination explicitly.</p>
 */
public class GroupChatCommand extends ModuleCommand {
    @Getter
    private final String messageSetParty;
    @Getter
    private final String messageSetGuild;
    @Getter
    private final String messageSetOff;
    @Getter
    private final String messageUnknownTarget;

    public GroupChatCommand() {
        super(StreamlineGroups.getInstance(),
                "groupchat",
                "streamline.command.groupchat.default",
                "gc", "gchat"
        );

        this.messageSetParty = getCommandResource().getOrSetDefault("messages.set.party",
                "&7Your chat is now going to your &dparty&7.");
        this.messageSetGuild = getCommandResource().getOrSetDefault("messages.set.guild",
                "&7Your chat is now going to your &dguild&7.");
        this.messageSetOff = getCommandResource().getOrSetDefault("messages.set.off",
                "&7Your chat is now going to &dnormal chat&7.");
        this.messageUnknownTarget = getCommandResource().getOrSetDefault("messages.errors.unknown-target",
                "&cUnknown chat target! Use one of: &dparty&c, &dguild&c, &doff&c.");
    }

    @Override
    public void run(CosmicSender sender, String[] strings) {
        GroupedPlayer player = PlayerLoader.getInstance().getOrCreate(sender.getUuid());

        ChatType wanted;
        if (strings.length < 1 || strings[0].isEmpty()) {
            // Bare command toggles party chat on and off.
            wanted = player.getChatType() == ChatType.PARTY ? ChatType.NOT_SET : ChatType.PARTY;
        } else {
            switch (strings[0].toLowerCase(Locale.ROOT)) {
                case "party":
                case "p":
                    wanted = ChatType.PARTY;
                    break;
                case "guild":
                case "g":
                    wanted = ChatType.GUILD;
                    break;
                case "off":
                case "none":
                case "normal":
                    wanted = ChatType.NOT_SET;
                    break;
                default:
                    ModuleUtils.sendMessage(sender, getMessageUnknownTarget());
                    return;
            }
        }

        player.setChatType(wanted);
        player.save();

        switch (wanted) {
            case PARTY:
                ModuleUtils.sendMessage(sender, getMessageSetParty());
                break;
            case GUILD:
                ModuleUtils.sendMessage(sender, getMessageSetGuild());
                break;
            default:
                ModuleUtils.sendMessage(sender, getMessageSetOff());
                break;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender sender, String[] strings) {
        if (strings.length <= 1) {
            return new ConcurrentSkipListSet<>(List.of("party", "guild", "off"));
        }

        return new ConcurrentSkipListSet<>();
    }
}
