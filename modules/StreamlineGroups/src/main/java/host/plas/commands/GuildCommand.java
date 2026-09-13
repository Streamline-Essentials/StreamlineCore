package host.plas.commands;

import host.plas.StreamlineGroups;
import host.plas.data.GroupManager;
import host.plas.data.Guild;
import host.plas.data.flags.GroupFlag;
import lombok.Getter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.modules.CosmicModule;
import singularity.modules.ModuleUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@code /guild <action> (<player>) (<player>)}, the persistent counterpart to
 * {@link PartyCommand}.
 *
 * <p>Every action operates on the sender's own guild. Supplying trailing player arguments
 * targets someone else's guild instead and requires the "other" permission.</p>
 */
public class GuildCommand extends ModuleCommand {
    @Getter
    private final String useOther;
    @Getter
    private final String messageMuted;
    @Getter
    private final String messageUnmuted;

    public GuildCommand(CosmicModule module) {
        super(module,
                "guild",
                "streamline.command.guild.default",
                "g"
        );

        this.useOther = getCommandResource().getOrSetDefault("permissions.use.other", "streamline.command.guild.others");
        this.messageMuted = getCommandResource()
                .getOrSetDefault("messages.mute.on", "&7Your guild's chat is now &cmuted&7.");
        this.messageUnmuted = getCommandResource()
                .getOrSetDefault("messages.mute.off", "&7Your guild's chat is now &aunmuted&7.");
    }

    @Override
    public void run(CosmicSender sender, String[] strings) {
        if (strings.length < 1 || strings[0].isEmpty()) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        switch (strings[0].toLowerCase(Locale.ROOT)) {
            case "create":
                GroupManager.createGuild(sender, sender);
                break;
            case "list": {
                CosmicSender target = resolveTarget(sender, strings, 1);
                if (target == null) return;

                GroupManager.listGuild(sender, target);
                break;
            }
            case "invite": {
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                Optional<CosmicSender> toInvite = ModuleUtils.getOrGetUserByName(strings[1]);
                if (toInvite.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                CosmicSender owner = resolveTarget(sender, strings, 2);
                if (owner == null) return;

                GroupManager.invitePlayerGuild(sender, owner, toInvite.get());
                break;
            }
            case "accept":
            case "deny": {
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                // The first argument names whose guild the invite came from.
                Optional<CosmicSender> inviter = ModuleUtils.getOrGetUserByName(strings[1]);
                if (inviter.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                CosmicSender invited = resolveTarget(sender, strings, 2);
                if (invited == null) return;

                if (strings[0].equalsIgnoreCase("accept")) {
                    GroupManager.acceptInviteGuild(sender, inviter.get(), invited);
                } else {
                    GroupManager.denyInviteGuild(sender, inviter.get(), invited);
                }
                break;
            }
            case "disband": {
                CosmicSender target = resolveTarget(sender, strings, 1);
                if (target == null) return;

                GroupManager.disbandGuild(sender, target);
                break;
            }
            case "leave": {
                CosmicSender target = resolveTarget(sender, strings, 1);
                if (target == null) return;

                GroupManager.leaveGuild(sender, target);
                break;
            }
            case "promote":
            case "demote": {
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                Optional<CosmicSender> subject = ModuleUtils.getOrGetUserByName(strings[1]);
                if (subject.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                CosmicSender owner = resolveTarget(sender, strings, 2);
                if (owner == null) return;

                if (strings[0].equalsIgnoreCase("promote")) {
                    GroupManager.promoteGuild(sender, owner, subject.get());
                } else {
                    GroupManager.demoteGuild(sender, owner, subject.get());
                }
                break;
            }
            case "chat": {
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                GroupManager.chatGuild(sender, sender, ModuleUtils.argsToStringMinus(strings, 0));
                break;
            }
            case "mute": {
                CosmicSender target = resolveTarget(sender, strings, 1);
                if (target == null) return;

                Optional<Guild> optional = GroupManager.getGuild(target);
                if (optional.isEmpty()) {
                    ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
                    return;
                }
                Guild guild = optional.get();

                if (guild.hasMember(sender) && ! guild.userHasFlag(sender, GroupFlag.MUTE)) {
                    ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorWithoutFlag(GroupFlag.MUTE));
                    return;
                }

                guild.toggleMute();
                guild.save();

                ModuleUtils.sendMessage(sender, guild.isMuted()
                        ? getMessageMuted()
                        : getMessageUnmuted());
                break;
            }
            default:
                ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                break;
        }
    }

    /**
     * Resolves whose guild an action applies to.
     *
     * <p>With no argument at {@code index} the sender is the target. Naming someone else
     * requires the "other" permission; a failure is reported to the sender and reported
     * here as {@code null}.</p>
     */
    private CosmicSender resolveTarget(CosmicSender sender, String[] strings, int index) {
        if (strings.length <= index) return sender;

        Optional<CosmicSender> other = ModuleUtils.getOrGetUserByName(strings[index]);
        if (other.isEmpty()) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return null;
        }

        if (! ModuleUtils.hasPermission(sender, getUseOther())) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
            return null;
        }

        return other.get();
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender sender, String[] strings) {
        if (strings.length <= 1) {
            return new ConcurrentSkipListSet<>(List.of(
                    "create", "list", "invite", "accept", "deny",
                    "disband", "leave", "promote", "demote", "chat", "mute"
            ));
        }

        if (strings.length == 2) {
            String action = strings[0].toLowerCase(Locale.ROOT);

            if (action.equals("promote") || action.equals("demote")) {
                Optional<Guild> optional = GroupManager.getGuild(sender);
                if (optional.isEmpty()) return new ConcurrentSkipListSet<>();
                Guild guild = optional.get();

                ConcurrentSkipListSet<String> names = new ConcurrentSkipListSet<>();
                guild.getAllUsers().forEach(a -> names.add(a.getCurrentName()));
                return names;
            }

            if (action.equals("invite") || action.equals("accept") || action.equals("deny")) {
                return ModuleUtils.getOnlinePlayerNames();
            }

            if (action.equals("list") || action.equals("disband") || action.equals("leave") || action.equals("mute")) {
                if (ModuleUtils.hasPermission(sender, getUseOther())) {
                    return ModuleUtils.getOnlinePlayerNames();
                }
            }
        }

        return new ConcurrentSkipListSet<>();
    }
}
