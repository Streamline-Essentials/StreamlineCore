package host.plas.timers;

import lombok.Getter;
import lombok.Setter;
import singularity.scheduler.ModuleRunnable;
import host.plas.StreamlineMessaging;
import host.plas.savables.SavableChatter;

@Getter
public class FriendInviteExpiry extends ModuleRunnable {
    private final SavableChatter sender;
    private final SavableChatter invited;
    @Setter
    private long ticksLeft;

    public FriendInviteExpiry(SavableChatter sender, SavableChatter invited, long ticksLeft) {
        super(StreamlineMessaging.getInstance(), 0L, 1L);
        this.sender = sender;
        this.invited = invited;
        this.ticksLeft = ticksLeft;
    }

    @Override
    public void run() {
        if (ticksLeft <= 0) {
            sender.handleInviteExpiryEnd(this);
            this.cancel();
        }

        ticksLeft --;
    }
}
