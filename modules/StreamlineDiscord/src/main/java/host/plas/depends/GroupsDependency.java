package host.plas.depends;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.data.GroupManager;
import host.plas.data.Party;
import host.plas.data.parties.PartyChatEvent;
import host.plas.discord.data.channeling.EndPointType;
import host.plas.discord.data.channeling.RouteLoader;
import host.plas.discord.data.channeling.RoutedUser;
import lombok.Getter;
import lombok.Setter;
import host.plas.StreamlineDiscord;
import host.plas.StreamlineGroups;
import singularity.data.console.CosmicSender;
import singularity.holders.ModuleDependencyHolder;
import singularity.modules.ModuleUtils;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

public class GroupsDependency extends ModuleDependencyHolder<StreamlineGroups> {
    @Getter @Setter
    private GroupsListener groupsListener;

    public GroupsDependency() {
        super("streamline-groups", "streamline-groups");
        if (super.isPresent()) {
            tryLoad(() -> {
                nativeLoad();
                if (getGroupsListener() == null) {
                    setGroupsListener(new GroupsListener());
                    ModuleUtils.listen(getGroupsListener(), StreamlineDiscord.getInstance());
                }
                return null;
            });
        } else {
            StreamlineDiscord.getInstance().logInfo("Did not detect a '" + getIdentifier() + "' module... Disabling support for '" + getIdentifier() + "'...");
        }
    }

    public static class GroupsListener implements BaseEventListener {
        @BaseProcessor
        public void onPartyChat(PartyChatEvent event) {
            if (! StreamlineDiscord.getConfig().allowStreamlinePartiesToDiscord()) return;

            RouteLoader.getLoadedRoutes().forEach((route) -> {
                if (route.getInput().getType().equals(EndPointType.PARTY))
                    route.bounceMessage(new RoutedUser(event.getSender()), event.getMessage());
            });
        }
    }

    public ConcurrentSkipListMap<String, CosmicSender> getPartyMembersOf(String uuid) {
        ConcurrentSkipListMap<String, CosmicSender> r = new ConcurrentSkipListMap<>();
        if (! isPresent()) return r;

        Optional<Party> optional = GroupManager.get(uuid);
        if (optional.isEmpty()) return r;
        Party party = optional.get();

        party.getAllUsers().forEach(user -> {
            r.put(user.getUuid(), user);
        });

        return r;
    }
}
