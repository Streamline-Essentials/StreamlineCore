package host.plas.commands;

import host.plas.database.MyLoader;
import lombok.Getter;
import lombok.Setter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.ModuleUtils;
import singularity.data.console.CosmicSender;
import host.plas.StreamlineMessaging;
import host.plas.savables.SavableChatter;
import singularity.utils.UserUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public class FriendCommand extends ModuleCommand {
    private String messageResultRequestSender;
    private String messageResultRequestOther;

    private String messageResultAcceptSender;
    private String messageResultAcceptOther;
    private String messageResultDenySender;
    private String messageResultDenyOther;
    private String messageResultRemoveSender;
    private String messageResultRemoveOther;
    private String messageResultListSelf;
    private String messageResultListOther;
    private String messageResultTeleportSender;
    private String messageResultTeleportOther;
    private String messageResultBestCheck;
    private String messageResultToggle;

    private String messageErrorOfflineOther;

    private String permissionListOthers;
    private String permissionTeleport;

    public FriendCommand() {
        super(StreamlineMessaging.getInstance(),
                "friend",
                "streamline.command.friend.default",
                "f"
        );

        messageResultRequestSender = getCommandResource().getOrSetDefault("messages.result.request.sender", "&dYou &erequested %streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &eto be &dyour friend&8!");
        messageResultRequestOther = getCommandResource().getOrSetDefault("messages.result.request.other", "%streamline_user_formatted% &erequested to be &dyour friend&8! &eType &b/friend accept %streamline_user_absolute% &eto &aaccept&8!");

        messageResultAcceptSender = getCommandResource().getOrSetDefault("messages.result.accept.sender", "&dYou &aaccepted %streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&7'&es friend request&8!");
        messageResultAcceptOther = getCommandResource().getOrSetDefault("messages.result.accept.other", "%streamline_parse_%this_sender%:::*/*streamline_user_formatted*/*% &aaccepted &dyour &efriend request&8!");

        messageResultDenySender = getCommandResource().getOrSetDefault("messages.result.deny.sender", "&dYou &cdenied %streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&7'&es friend request&8!");
        messageResultDenyOther = getCommandResource().getOrSetDefault("messages.result.deny.other", "%streamline_parse_%this_sender%:::*/*streamline_user_formatted*/*% &cdenied &dyour &efriend request&8!");

        messageResultRemoveSender = getCommandResource().getOrSetDefault("messages.result.remove.sender", "&dYou &cremoved %streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &eas a friend&8!");
        messageResultRemoveOther = getCommandResource().getOrSetDefault("messages.result.remove.other", "%streamline_parse_%this_sender%:::*/*streamline_user_formatted*/*% &cremoved &dyou &eas a friend&8!");

        messageResultListSelf = getCommandResource().getOrSetDefault("messages.result.list.sender", "&dYour &cfriends &7(&epage %this_page%&7)&8:%newline%%this_friends_list%");
        messageResultListOther = getCommandResource().getOrSetDefault("messages.result.list.other", "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&7'&es &cfriends &7(&epage %this_page%&7)&8:%newline%%this_friends_list%");

        messageResultTeleportSender = getCommandResource().getOrSetDefault("messages.result.teleport.sender", "&dYou &eteleported to &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&8!");
        messageResultTeleportOther = getCommandResource().getOrSetDefault("messages.result.teleport.other", "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &eteleported to &dyou&8!");

        messageResultBestCheck = getCommandResource().getOrSetDefault("messages.result.best.check", "&eIs %streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &dyour &cbest friend&8: %messaging_friends_with_%this_other%%");

        messageResultToggle = getCommandResource().getOrSetDefault("messages.result.toggle", "&eAccepting friend invites&8: %messaging_friend_invites_enabled%");

        messageErrorOfflineOther = getCommandResource().getOrSetDefault("messages.errors.player.offline", "&cThat player is offline!");

        permissionListOthers = getCommandResource().getOrSetDefault("basic.permissions.others", "streamline.command.friend.others");
        permissionTeleport = getCommandResource().getOrSetDefault("basic.permissions.teleport.others", "streamline.command.friend.teleport.others");
    }

    @Override
    public void run(CosmicSender streamSender, String[] strings) {
        String action;
        if (strings[0].isEmpty()) {
            action = "list";
        } else {
            action = strings[0];
        }

        SavableChatter chatter = MyLoader.getInstance().getOrCreate(streamSender.getUuid());
        if (chatter == null) {
            ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.USER_SELF.get());
            return;
        }

        switch (action.toLowerCase()) {
            case "list":
                int page;
                if (strings.length < 2) {
                    page = 1;
                } else {
                    try {
                        page = Integer.parseInt(strings[1]);
                    } catch (Exception e) {
                        page = 1;
                        ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_NUMBER.get());
                    }
                }

                if (strings.length > 3) {
                    ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_MANY.get());
                    return;
                }

                if (strings.length == 3) {
                    if (! streamSender.hasPermission(permissionListOthers)) {
                        ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
                        return;
                    }

                    CosmicSender other = UserUtils.getOrCreateSenderByName(strings[2]).orElse(null);
                    if (other == null) {
                        ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                        return;
                    }

                    chatter = MyLoader.getInstance().getOrCreate(other.getUuid());

                    String p = chatter.getFriendsListPaged().get(page - 1);

                    if (p == null) {
                        p = "";
                    }

                    ModuleUtils.sendMessage(streamSender, getWithOther(streamSender, messageResultListOther, other)
                            .replace("%this_page%", String.valueOf(page))
                            .replace("%this_friends_list%", p)
                    );
                } else {
                    String p = chatter.getFriendsListPaged().get(page - 1);

                    if (p == null) {
                        p = "";
                    }

                    ModuleUtils.sendMessage(streamSender, messageResultListSelf
                            .replace("%this_page%", String.valueOf(page))
                            .replace("%this_friends_list%", p)
                    );
                }
                break;
            case "add":
            case "accept":
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                String username = strings[1];
                if (Objects.equals(username, streamSender.getCurrentName())) {
                    ModuleUtils.sendMessage(streamSender, StreamlineMessaging.getMessages().errorsFriendSelfInvite());
                    return;
                }

                CosmicSender other = UserUtils.getOrCreateSender(username).orElse(null);
                if (other == null) {
                    ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                SavableChatter otherChatter = MyLoader.getInstance().getOrCreate(other.getUuid());
                if (otherChatter == null) {
                    ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }
                if (chatter.isMyFriend(otherChatter)) {
                    ModuleUtils.sendMessage(streamSender, StreamlineMessaging.getMessages().friendsAlreadyFriends());
                    return;
                }

                if (otherChatter.isAlreadyFriendInvited(chatter)) {
                    chatter.addFriend(otherChatter);
                    otherChatter.addFriend(chatter);

                    ModuleUtils.sendMessage(streamSender, getWithOther(streamSender, messageResultAcceptSender, other));
                    ModuleUtils.sendMessage(other, getWithOther(streamSender, messageResultAcceptOther, other));
                } else {
                    if (chatter.isAlreadyFriendInvited(otherChatter)) {
                        ModuleUtils.sendMessage(streamSender, StreamlineMessaging.getMessages().friendsAlreadyInvited());
                        return;
                    }
                    if (! otherChatter.isAcceptingFriendRequests()) {
                        ModuleUtils.sendMessage(streamSender, StreamlineMessaging.getMessages().friendsToggledOffInvites());
                        return;
                    }
                    chatter.addInviteTo(otherChatter);

                    ModuleUtils.sendMessage(streamSender, getWithOther(streamSender, messageResultRequestSender, other));
                    ModuleUtils.sendMessage(other, getWithOther(streamSender, messageResultRequestOther, other));
                }
                break;
            case "remove":
            case "deny":
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                String usernameRemove = strings[1];
                if (Objects.equals(usernameRemove, streamSender.getCurrentName())) {
                    ModuleUtils.sendMessage(streamSender, StreamlineMessaging.getMessages().errorsFriendSelfInvite());
                    return;
                }

                CosmicSender otherRemove = UserUtils.getOrCreateSender(usernameRemove).orElse(null);
                if (otherRemove == null) {
                    ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                SavableChatter otherChatterRemove = MyLoader.getInstance().getOrCreate(otherRemove.getUuid());
                if (otherChatterRemove == null) {
                    ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (chatter.isAlreadyFriendInvited(otherRemove.getUuid())) {
                    if (chatter.isMyFriend(otherChatterRemove)) {
                        ModuleUtils.sendMessage(streamSender, StreamlineMessaging.getMessages().friendsAlreadyFriends());
                        return;
                    }

                    chatter.removeInviteTo(otherChatterRemove);
                    otherChatterRemove.removeInviteTo(chatter);

                    ModuleUtils.sendMessage(streamSender, getWithOther(streamSender, messageResultDenySender, otherRemove));
                    ModuleUtils.sendMessage(otherRemove, getWithOther(streamSender, messageResultDenyOther, otherRemove));
                } else {
                    if (! chatter.isMyFriend(otherChatterRemove)) {
                        ModuleUtils.sendMessage(streamSender, StreamlineMessaging.getMessages().friendsAlreadyNotFriends());
                        return;
                    }

                    chatter.removeInviteTo(otherChatterRemove);
                    otherChatterRemove.removeInviteTo(chatter);
                    chatter.removeFriend(otherChatterRemove);
                    otherChatterRemove.removeFriend(chatter);

                    ModuleUtils.sendMessage(streamSender, getWithOther(streamSender, messageResultRemoveSender, otherRemove));
                    ModuleUtils.sendMessage(otherRemove, getWithOther(streamSender, messageResultRemoveOther, otherRemove));
                }
                break;
            case "teleport":
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                String usernameTeleport = strings[1];
                CosmicSender otherTeleport = UserUtils.getOrCreateSender(usernameTeleport).orElse(null);
                if (otherTeleport == null) {
                    ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (! otherTeleport.isOnline()) {
                    ModuleUtils.sendMessage(streamSender, getWithOther(streamSender, messageErrorOfflineOther, otherTeleport));
                    return;
                }

                ModuleUtils.connect(streamSender, otherTeleport.getCurrentName());
                ModuleUtils.sendMessage(streamSender, getWithOther(streamSender, messageResultTeleportSender, otherTeleport));
                ModuleUtils.sendMessage(otherTeleport, getWithOther(streamSender, messageResultTeleportOther, otherTeleport));
                break;
            case "best":
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                String usernameBest = strings[1];
                CosmicSender otherBest = UserUtils.getOrCreateSender(usernameBest).orElse(null);
                if (otherBest == null) {
                    ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                SavableChatter otherChatterBest = MyLoader.getInstance().getOrCreate(otherBest.getUuid());
                if (otherChatterBest == null) {
                    ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }
                if (chatter.isMyFriend(otherChatterBest)) {
                    ModuleUtils.sendMessage(streamSender, StreamlineMessaging.getMessages().friendsAlreadyFriends());
                    return;
                }

                if (strings.length < 3) {
                    ModuleUtils.sendMessage(streamSender, getWithOther(streamSender, messageResultBestCheck, otherBest));
                    return;
                }

                switch (strings[2]) {
                    case "check":
                        ModuleUtils.sendMessage(streamSender, getWithOther(streamSender, messageResultBestCheck, otherBest));
                        break;
                    case "add":
                        if (chatter.isMyBestFriend(otherChatterBest)) {
                            ModuleUtils.sendMessage(streamSender, StreamlineMessaging.getMessages().friendsAlreadyBestFriends());
                            return;
                        }

                        chatter.addBestFriend(otherChatterBest);
                        break;
                    case "remove":
                        if (! chatter.isMyBestFriend(otherChatterBest)) {
                            ModuleUtils.sendMessage(streamSender, StreamlineMessaging.getMessages().friendsAlreadyNotBestFriends());
                            return;
                        }

                        chatter.removeBestFriend(otherChatterBest);
                        break;
                }

                if (otherChatterBest.isAlreadyFriendInvited(chatter)) {
                    chatter.addFriend(otherChatterBest);
                    otherChatterBest.addFriend(chatter);

                    ModuleUtils.sendMessage(streamSender, getWithOther(streamSender, messageResultAcceptSender, otherBest));
                    ModuleUtils.sendMessage(otherBest, getWithOther(streamSender, messageResultAcceptOther, otherBest));
                } else {
                    if (chatter.isAlreadyFriendInvited(otherChatterBest)) {
                        ModuleUtils.sendMessage(streamSender, StreamlineMessaging.getMessages().friendsAlreadyInvited());
                        return;
                    }
                    chatter.addInviteTo(otherChatterBest);

                    ModuleUtils.sendMessage(streamSender, getWithOther(streamSender, messageResultRequestSender, otherBest));
                    ModuleUtils.sendMessage(otherBest, getWithOther(streamSender, messageResultRequestOther, otherBest));
                }
                break;
            case "toggle":
                chatter.setAcceptingFriendRequests(! chatter.isAcceptingFriendRequests());
                ModuleUtils.sendMessage(streamSender, messageResultToggle);
                break;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        if (strings.length <= 1) {
            return new ConcurrentSkipListSet<>(List.of(
                    "list",
                    "add",
                    "remove",
                    "accept",
                    "deny",
                    "teleport",
                    "best",
                    "toggle"
            ));
        }
        if (strings.length == 2) {
            if (ModuleUtils.equalsAny(strings[0], List.of("add", "remove", "accept", "deny"))) {
                return ModuleUtils.getOnlinePlayerNames();
            }
            if (ModuleUtils.equalsAny(strings[0], List.of("list"))) {
                return new ConcurrentSkipListSet<>(List.of("1", "2", "3"));
            }
        }

        if (strings.length == 3) {
            if (ModuleUtils.equalsAny(strings[0], List.of("list"))) {
                return ModuleUtils.getOnlinePlayerNames();
            }
            if (ModuleUtils.equalsAny(strings[0], List.of("best"))) {
                return new ConcurrentSkipListSet<>(List.of("remove", "add", "toggle", "check"));
            }
        }

        return new ConcurrentSkipListSet<>();
    }
}
