package host.plas.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.StreamlineMessaging;

public class Messages extends SimpleConfiguration {
    public Messages() {
        super("messages.yml", StreamlineMessaging.getInstance().getDataFolder(), false);
    }

    @Override
    public void init() {
        errorsMessagingSelf();
        errorsChannelIsNull();
        errorsChannelNoAccess();
        errorsFriendSelfInvite();

        friendsAddMessage();
        friendsAddSend();
        bestFriendsAddMessage();
        bestFriendsAddSend();

        friendsAlreadyFriends();
        friendsAlreadyBestFriends();
        friendsAlreadyInvited();
        friendsAlreadyNotFriends();
        friendsAlreadyNotBestFriends();
    }

    public String errorsMessagingSelf() {
        reloadResource();

        return getResource().getOrSetDefault("errors.messaging.self", "&cYou cannot message yourself!");
    }

    public String errorsChannelIsNull() {
        reloadResource();

        return getResource().getOrSetDefault("errors.channel.is-null", "&cThat channel does not exist or is not loaded!");
    }

    public String errorsChannelNoAccess() {
        reloadResource();

        return getResource().getOrSetDefault("errors.channel.no-access", "&cYou do not have permissions to access that channel!");
    }
    public String errorsFriendSelfInvite() {
        reloadResource();

        return getResource().getOrSetDefault("errors.friends.self.invite", "&cYou cannot friend yourself!");
    }

    public String friendsAlreadyFriends() {
        reloadResource();

        return getResource().getOrSetDefault("errors.friends.already.friends", "&cYou are already friends with this user!");
    }

    public String friendsAlreadyNotFriends() {
        reloadResource();

        return getResource().getOrSetDefault("errors.friends.already.not-friends", "&cYou are not this user's friend!");
    }

    public String friendsAlreadyBestFriends() {
        reloadResource();

        return getResource().getOrSetDefault("errors.friends.already.best-friends", "&cYou are already best friends with this user!");
    }

    public String friendsAlreadyNotBestFriends() {
        reloadResource();

        return getResource().getOrSetDefault("errors.friends.already.not-best-friends", "&cYou are already not best friends with this user!");
    }

    public String friendsAlreadyInvited() {
        reloadResource();

        return getResource().getOrSetDefault("errors.friends.already.invited", "&cYou already invited this user to be your friend!");
    }

    public String friendsToggledOffInvites() {
        reloadResource();

        return getResource().getOrSetDefault("errors.friends.toggled.off.invites", "&cThat player has friend invites toggled off!");
    }

    public String friendsAddMessage() {
        reloadResource();

        return getResource().getOrSetDefault("friends.regular.add.message", "%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &ewas &aadded &eto &dyour &cfriend&7'&cs list&8!");
    }

    public boolean friendsAddSend() {
        reloadResource();

        return getResource().getOrSetDefault("friends.regular.add.send", false);
    }

    public String friendsRemoveMessage() {
        reloadResource();

        return getResource().getOrSetDefault("friends.regular.remove.message", "%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &ewas &cremoved &efrom &dyour &cfriend&7'&cs list&8!");
    }

    public boolean friendsRemoveSend() {
        reloadResource();

        return getResource().getOrSetDefault("friends.regular.remove.send", false);
    }

    public int friendsListMaxPerPage() {
        reloadResource();

        return getResource().getOrSetDefault("friends.regular.list.max-per-page", 5);
    }

    public String friendsListEntryNotLast() {
        reloadResource();

        return getResource().getOrSetDefault("friends.regular.list.entry.not-last", "&d%streamline_user_formatted% &7(&e%streamline_user_online%&7)%newline%");
    }

    public String friendsListEntryLast() {
        reloadResource();

        return getResource().getOrSetDefault("friends.regular.list.entry.last", "&d%streamline_user_formatted% &7(&e%streamline_user_online%&7)");
    }

    public String bestFriendsAddMessage() {
        reloadResource();

        return getResource().getOrSetDefault("friends.best.add.message", "%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &ewas &aadded &eto &dyour &cbest friend&7'&cs list&8!");
    }

    public boolean bestFriendsAddSend() {
        reloadResource();

        return getResource().getOrSetDefault("friends.best.add.send", true);
    }

    public String bestFriendsRemoveMessage() {
        reloadResource();

        return getResource().getOrSetDefault("friends.best.remove.message", "%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &ewas &cremoved &efrom &dyour &cbest friend&7'&cs list&8!");
    }

    public boolean bestFriendsRemoveSend() {
        reloadResource();

        return getResource().getOrSetDefault("friends.best.remove.send", true);
    }

    public int bestFriendsListMaxPerPage() {
        reloadResource();

        return getResource().getOrSetDefault("friends.best.list.max-per-page", 5);
    }

    public String bestFriendsListEntryNotLast() {
        reloadResource();

        return getResource().getOrSetDefault("friends.best.list.entry.not-last", "&d%streamline_user_formatted% &7(&e%streamline_user_online%&7)%newline%");
    }

    public String bestFriendsListEntryLast() {
        reloadResource();

        return getResource().getOrSetDefault("friends.best.list.entry.last", "&d%streamline_user_formatted% &7(&e%streamline_user_online%&7)");
    }

    public String friendInviteTimeoutSender() {
        reloadResource();

        return getResource().getOrSetDefault("friends.invite.timeout.sender", "&dYour &efriend invite to &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &ehas &c&lexpired&8!");
    }

    public String friendInviteTimeoutInvited() {
        reloadResource();

        return getResource().getOrSetDefault("friends.invite.timeout.invited", "&eThe friend invite to &dyou &efrom &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &ehas &c&lexpired&8!");
    }
}
