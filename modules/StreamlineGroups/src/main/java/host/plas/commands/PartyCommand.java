package host.plas.commands;

import host.plas.data.Party;
import lombok.Getter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.CosmicModule;
import singularity.modules.ModuleUtils;
import singularity.data.console.CosmicSender;
import host.plas.StreamlineGroups;
import host.plas.data.GroupManager;
import host.plas.data.flags.GroupFlag;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

public class PartyCommand extends ModuleCommand {
    @Getter
    private final String useOther;

    public PartyCommand(CosmicModule module) {
        super(module,
                "party",
                "streamline.command.party.default",
                "p"
        );

        this.useOther = this.getCommandResource().getOrSetDefault("permissions.use.other", "streamline.command.party.others");
    }

    @Override
    public void run(CosmicSender sender, String[] strings) {
        if (strings[0].equals("")) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String action = strings[0].toLowerCase(Locale.ROOT);

        switch (action) {
            case "create":
                if (strings.length > 1) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_MANY.get());
                    return;
                }

                GroupManager.createParty(sender, sender);
                break;
            case "list":
                if (strings.length < 2) {
                    GroupManager.listParty(sender, sender);
                    return;
                }

                Optional<CosmicSender> other = ModuleUtils.getOrGetUserByName(strings[1]);
                if (other.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (! ModuleUtils.hasPermission(sender, this.useOther)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                    return;
                }

                GroupManager.listParty(sender, other.get());
                break;
            case "invite":
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                Optional<CosmicSender> otherInvite = ModuleUtils.getOrGetUserByName(strings[1]);

                if (otherInvite.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (strings.length == 2) {
                    GroupManager.invitePlayerParty(sender, sender, otherInvite.get());
                    return;
                }

                Optional<CosmicSender> otherOther = ModuleUtils.getOrGetUserByName(strings[2]);

                if (otherOther.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (! ModuleUtils.hasPermission(sender, this.useOther)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                    return;
                }

                Optional<Party> optional = GroupManager.get(otherOther.get());
                if (optional.isEmpty()) {
                    ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInOther());
                    return;
                }

                GroupManager.invitePlayerParty(sender, otherInvite.get(), otherOther.get());
                break;
            case "accept":
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                Optional<CosmicSender> otherAccept = ModuleUtils.getOrGetUserByName(strings[1]);

                if (otherAccept.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (strings.length == 2) {
                    GroupManager.acceptInviteParty(sender, otherAccept.get(), sender);
                    return;
                }

                Optional<CosmicSender> otherOtherAccept = ModuleUtils.getOrGetUserByName(strings[2]);

                if (otherOtherAccept.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (! ModuleUtils.hasPermission(sender, this.useOther)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                    return;
                }
                
                Optional<Party> otherOptional = GroupManager.get(otherOtherAccept.get());
                if (otherOptional.isEmpty()) {
                    ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInOther());
                    return;
                }

                GroupManager.acceptInviteParty(sender, otherAccept.get(), otherOtherAccept.get());
                break;
            case "deny":
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                Optional<CosmicSender> otherDeny = ModuleUtils.getOrGetUserByName(strings[1]);

                if (otherDeny.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (strings.length == 2) {
                    GroupManager.denyInviteParty(sender, otherDeny.get(), sender);
                    return;
                }

                Optional<CosmicSender> otherOtherDeny = ModuleUtils.getOrGetUserByName(strings[2]);

                if (otherOtherDeny.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (! ModuleUtils.hasPermission(sender, this.useOther)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                    return;
                }
                
                Optional<Party> otherOtherOptional = GroupManager.get(otherOtherDeny.get());
                if (otherOtherOptional.isEmpty()) {
                    ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInOther());
                    return;
                }

                GroupManager.denyInviteParty(sender, otherDeny.get(), otherOtherDeny.get());
                break;
            case "disband":
                if (strings.length < 2) {
                    GroupManager.disbandParty(sender, sender);
                    return;
                }

                Optional<CosmicSender> otherDisband = ModuleUtils.getOrGetUserByName(strings[1]);

                if (otherDisband.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (! ModuleUtils.hasPermission(sender, this.useOther)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                    return;
                }
                
                Optional<Party> partyDisband = GroupManager.get(otherDisband.get());
                if (partyDisband.isEmpty()) {
                    ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInOther());
                    return;
                }

                GroupManager.disbandParty(sender, partyDisband.get().getOwner());
                break;
            case "promote":
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                Optional<CosmicSender> otherPromote = ModuleUtils.getOrGetUserByName(strings[1]);

                if (otherPromote.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (strings.length == 2) {
                    GroupManager.promoteParty(sender, sender, otherPromote.get());
                    return;
                }

                Optional<CosmicSender> otherOtherPromote = ModuleUtils.getOrGetUserByName(strings[2]);

                if (otherOtherPromote.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (! ModuleUtils.hasPermission(sender, this.useOther)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                    return;
                }

                Optional<Party> partyPromote = GroupManager.get(otherOtherPromote.get());
                if (partyPromote.isEmpty()) {
                    ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInOther());
                    return;
                }

                GroupManager.promoteParty(sender, otherPromote.get(), otherOtherPromote.get());
                break;
            case "demote":
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                Optional<CosmicSender> otherDemote = ModuleUtils.getOrGetUserByName(strings[1]);

                if (otherDemote.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (strings.length == 2) {
                    GroupManager.demoteParty(sender, sender, otherDemote.get());
                    return;
                }

                Optional<CosmicSender> otherOtherDemote = ModuleUtils.getOrGetUserByName(strings[2]);

                if (otherOtherDemote.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (! ModuleUtils.hasPermission(sender, this.useOther)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                    return;
                }
                
                Optional<Party> partyDemote = GroupManager.get(otherOtherDemote.get());
                if (partyDemote.isEmpty()) {
                    ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInOther());
                    return;
                }

                GroupManager.demoteParty(sender, otherDemote.get(), otherOtherDemote.get());
                break;
            case "leave":
                if (strings.length < 2) {
                    GroupManager.leaveParty(sender, sender);
                    return;
                }

                Optional<CosmicSender> otherLeave = ModuleUtils.getOrGetUserByName(strings[1]);

                if (otherLeave.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (! ModuleUtils.hasPermission(sender, this.useOther)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                    return;
                }

                GroupManager.leaveParty(sender, otherLeave.get());
                break;
            case "chat":
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                String message = ModuleUtils.argsToStringMinus(strings, 0);

                GroupManager.chatParty(sender, sender, message);
                break;
            case "chat-as":
                if (strings.length < 3) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                if (! ModuleUtils.hasPermission(sender, useOther)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                    return;
                }

                Optional<CosmicSender> otherChatAs = ModuleUtils.getOrGetUserByName(strings[1]);

                if (otherChatAs.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                String messageChatAs = ModuleUtils.argsToStringMinus(strings, 0, 1);

                GroupManager.chatParty(sender, otherChatAs.get(), messageChatAs);
                break;
            case "create-as":
                if (strings.length < 3) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                if (strings.length > 3) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_MANY.get());
                    return;
                }

                if (! ModuleUtils.hasPermission(sender, useOther)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                    return;
                }

                Optional<CosmicSender> otherCreateAs = ModuleUtils.getOrGetUserByName(strings[1]);

                if (otherCreateAs.isEmpty()) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                GroupManager.createParty(sender, otherCreateAs.get());
                break;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender sender, String[] strings) {
        Optional<Party> optional = GroupManager.get(sender);
        
        if (strings.length <= 1) {
            return new ConcurrentSkipListSet<>(List.of(
                    "create",
                    "create-as",
                    "list",
                    "invite",
                    "accept",
                    "deny",
                    "disband",
                    "promote",
                    "demote",
                    "leave",
                    "chat",
                    "chat-as"
            ));
        }
        if (strings.length == 2) {
            if (strings[0].equalsIgnoreCase("create-as")) {
                if (ModuleUtils.hasPermission(sender, this.useOther)) {
                    return ModuleUtils.getOnlinePlayerNames();
                }
            }
            if (strings[0].equalsIgnoreCase("list") || strings[0].equalsIgnoreCase("disband")) {
                if (ModuleUtils.hasPermission(sender, this.useOther)) {
                    return ModuleUtils.getOnlinePlayerNames();
                }
            }
            if (strings[0].equalsIgnoreCase("promote")) {
                if (optional.isEmpty()) return ModuleUtils.getOnlinePlayerNames();
                Party party = optional.get();
                if (! party.userHasFlag(sender, GroupFlag.PROMOTE)) return new ConcurrentSkipListSet<>();
                ConcurrentSkipListSet<CosmicSender> users = party.getAllUsers();
                party.getGroupRoleMap().rolesAbove(party.getRole(sender)).forEach(a -> {
                    users.removeAll(party.getGroupRoleMap().getUsersOf(a));
                });
                users.removeAll(party.getGroupRoleMap().getUsersOf(party.getRole(sender)));
                ConcurrentSkipListSet<String> names = new ConcurrentSkipListSet<>();
                users.forEach(a -> {
                    names.add(a.getCurrentName());
                });
                return names;
            }
            if (strings[0].equalsIgnoreCase("demote")) {
                if (optional.isEmpty()) return ModuleUtils.getOnlinePlayerNames();
                Party party = optional.get();
                if (! party.userHasFlag(sender, GroupFlag.PROMOTE)) return new ConcurrentSkipListSet<>();
                ConcurrentSkipListSet<CosmicSender> users = party.getAllUsers();
                party.getGroupRoleMap().rolesAbove(party.getRole(sender)).forEach(a -> {
                    users.removeAll(party.getGroupRoleMap().getUsersOf(a));
                });
                users.removeAll(party.getGroupRoleMap().getUsersOf(party.getRole(sender)));
                ConcurrentSkipListSet<String> names = new ConcurrentSkipListSet<>();
                users.forEach(a -> {
                    names.add(a.getCurrentName());
                });
                return names;
            }

            if (strings[0].equalsIgnoreCase("accept") || strings[0].equalsIgnoreCase("deny")
                    || strings[0].equalsIgnoreCase("invite")) {
                return ModuleUtils.getOnlinePlayerNames();
            }
        }

        if (strings.length == 3) {
            if (strings[0].equalsIgnoreCase("chat-as")) {
                if (ModuleUtils.hasPermission(sender, this.useOther)) {
                    return ModuleUtils.getOnlinePlayerNames();
                }
            }
        }

        return new ConcurrentSkipListSet<>();
    }
}
