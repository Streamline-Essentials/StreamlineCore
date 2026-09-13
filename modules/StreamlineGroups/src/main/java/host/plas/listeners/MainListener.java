package host.plas.listeners;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.data.Party;
import singularity.data.players.CosmicPlayer;
import singularity.events.server.LoginCompletedEvent;
import singularity.events.server.LogoutEvent;
import singularity.data.console.CosmicSender;
import host.plas.data.GroupManager;

import java.util.Optional;

public class MainListener implements BaseEventListener {
    @BaseProcessor
    public void updateLogin(LoginCompletedEvent event) {
        CosmicPlayer sender = event.getPlayer();

        Optional<Party> optional = GroupManager.get(sender);
        if (optional.isPresent()) {
            Party party = optional.get();
            // do something with party
        }
    }

    @BaseProcessor
    public void updateLogout(LogoutEvent event) {
        CosmicPlayer sender = event.getPlayer();

        Optional<Party> optional = GroupManager.get(sender);
        if (optional.isPresent()) {
            Party party = optional.get();

            if (! areAnyOnline(party.getAllUsers().toArray(new CosmicSender[0]))) {
                party.disband();
            }
        }
    }

    public boolean areAnyOnline(CosmicSender... users) {
        for (CosmicSender user : users) {
            if (user.isOnline()) return true;
        }

        return false;
    }
}
