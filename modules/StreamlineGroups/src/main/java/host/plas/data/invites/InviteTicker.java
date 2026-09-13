package host.plas.data.invites;

import host.plas.data.AbstractGroup;
import host.plas.data.events.InviteTimeoutEvent;
import lombok.Getter;
import singularity.modules.ModuleUtils;
import singularity.data.console.CosmicSender;
import singularity.scheduler.ModuleRunnable;
import org.jetbrains.annotations.NotNull;
import host.plas.StreamlineGroups;

@Getter
public class InviteTicker extends ModuleRunnable implements Comparable<InviteTicker> {
    private final AbstractGroup party;
    private final CosmicSender invited;
    private final CosmicSender inviter;

    public InviteTicker(AbstractGroup party, CosmicSender invited, CosmicSender inviter) {
        super(StreamlineGroups.getInstance(), 0, StreamlineGroups.getConfigs().inviteTimeout());
        this.party = party;
        this.invited = invited;
        this.inviter = inviter;
    }

    @Override
    public void run() {
        party.remFromInvites(this);

        ModuleUtils.sendMessage(inviter, StreamlineGroups.getMessages().partiesTimeoutInviteSender()
                .replace("%this_other%", invited.getCurrentName())
                .replace("%this_sender%", inviter.getCurrentName())
                .replace("%this_target%", invited.getCurrentName())
                .replace("%this_owner%", party.getMember(party.getUuid()).getCurrentName())
        );
        ModuleUtils.sendMessage(invited, StreamlineGroups.getMessages().partiesTimeoutInviteOther()
                .replace("%this_other%", invited.getCurrentName())
                .replace("%this_sender%", inviter.getCurrentName())
                .replace("%this_target%", invited.getCurrentName())
                .replace("%this_owner%", party.getMember(party.getUuid()).getCurrentName())
        );
        party.getAllUsers().forEach(a -> {
            if (a.equals(inviter)) return;
            ModuleUtils.sendMessage(a, StreamlineGroups.getMessages().partiesTimeoutInviteMembers()
                    .replace("%this_other%", invited.getCurrentName())
                    .replace("%this_sender%", inviter.getCurrentName())
                    .replace("%this_target%", invited.getCurrentName())
                    .replace("%this_owner%", party.getMember(party.getUuid()).getCurrentName())
            );
        });

        ModuleUtils.fireEvent(new InviteTimeoutEvent<>(party, invited, inviter));

        this.cancel();
    }

    @Override
    public int compareTo(@NotNull InviteTicker o) {
        return Integer.compare(getIndex(), o.getIndex());
    }
}
