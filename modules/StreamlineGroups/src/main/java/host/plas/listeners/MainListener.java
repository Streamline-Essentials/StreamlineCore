package host.plas.listeners;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.data.GroupManager;
import host.plas.data.Guild;
import host.plas.data.Party;
import host.plas.data.chats.ChatType;
import host.plas.data.player.GroupedPlayer;
import host.plas.database.PlayerLoader;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.events.server.CosmicChatEvent;
import singularity.events.server.LoginCompletedEvent;
import singularity.events.server.LogoutEvent;

import java.util.Optional;

public class MainListener implements BaseEventListener {
    /**
     * Brings the player's stored chat routing into memory so their first message after
     * joining already goes to the right place.
     */
    @BaseProcessor
    public void updateLogin(LoginCompletedEvent event) {
        CosmicPlayer sender = event.getPlayer();
        if (sender == null) return;

        PlayerLoader.getInstance().getOrCreate(sender.getUuid());
    }

    /**
     * Persists the player's routing and disbands their party once nobody in it is left
     * online.
     */
    @BaseProcessor
    public void updateLogout(LogoutEvent event) {
        CosmicPlayer sender = event.getPlayer();
        if (sender == null) return;

        PlayerLoader.getInstance().get(sender.getUuid()).ifPresent(GroupedPlayer::saveAndUnload);

        Optional<Party> optional = GroupManager.getParty(sender);
        if (optional.isPresent()) {
            Party party = optional.get();

            if (! areAnyOnline(party.getAllUsers().toArray(new CosmicSender[0]))) {
                party.disband();
            }
        }
    }

    /**
     * Diverts chat into the player's selected group.
     *
     * <p>The event is cancelled when the message is delivered to a group, so it is not also
     * broadcast to the whole server. A player whose selected group no longer exists falls
     * back to normal chat rather than losing the message.</p>
     */
    @BaseProcessor
    public void onChat(CosmicChatEvent event) {
        if (event.isCanceled()) return;

        CosmicPlayer sender = event.getPlayer();
        if (sender == null) return;

        Optional<GroupedPlayer> optional = PlayerLoader.getInstance().get(sender.getUuid());
        if (optional.isEmpty()) return;

        ChatType chatType = optional.get().getChatType();
        if (chatType == null || chatType == ChatType.NOT_SET || chatType == ChatType.ERROR) return;

        if (chatType == ChatType.PARTY) {
            if (GroupManager.getParty(sender).isEmpty()) return;

            GroupManager.chatParty(sender, sender, event.getMessage());
            event.setCanceled(true);
            return;
        }

        if (chatType == ChatType.GUILD) {
            Optional<Guild> guild = GroupManager.getGuild(sender);
            if (guild.isEmpty()) return;

            GroupManager.chatGuild(sender, sender, event.getMessage());
            event.setCanceled(true);
        }
    }

    public boolean areAnyOnline(CosmicSender... users) {
        for (CosmicSender user : users) {
            if (user != null && user.isOnline()) return true;
        }

        return false;
    }
}
