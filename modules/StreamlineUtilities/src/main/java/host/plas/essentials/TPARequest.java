package host.plas.essentials;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.teleportation.TPTicket;
import singularity.messages.proxied.ProxiedMessage;
import singularity.modules.ModuleUtils;
import singularity.scheduler.ModuleDelayedRunnable;
import org.jetbrains.annotations.NotNull;
import host.plas.StreamlineUtilities;
import host.plas.events.TPATimeoutEvent;

import java.util.Date;

@Getter @Setter
public class TPARequest implements Comparable<TPARequest> {
    public static class TimeoutTimer extends ModuleDelayedRunnable {
        @Getter
        private final TPARequest request;

        public TimeoutTimer(TPARequest request) {
            super(StreamlineUtilities.getInstance(), StreamlineUtilities.getConfigs().getTPATimeout());
            this.request = request;
        }

        @Override
        public void runDelayed() {
            TPATimeoutEvent event = new TPATimeoutEvent(request).fire();
            if (event.isCancelled()) {
                request.setTimeoutTimer(new TimeoutTimer(request));
                return;
            }

            request.timeout();
        }
    }

    public enum TransportType {
        SENDER_TO_RECEIVER,
        RECEIVER_TO_SENDER,
        ;
    }
    public enum ResolveType {
        LAST_LOCATION,
        CURRENT_LOCATION,
        ;
    }

    @Getter
    private CosmicPlayer sender;
    @Getter
    private CosmicPlayer receiver;
    @Getter
    private final Date timeSent;
    @Getter
    private final TransportType transportType;
    @Getter
    private final ResolveType resolveType;
    @Getter
    private final String server;
    @Getter @Setter
    private TimeoutTimer timeoutTimer;

    public TPARequest(CosmicPlayer sender, CosmicPlayer receiver, TransportType transportType, ResolveType resolveType, String server) {
        this.sender = sender;
        this.receiver = receiver;
        this.timeSent = new Date();
        this.transportType = transportType;
        this.resolveType = resolveType;
        this.server = server;
        this.timeoutTimer = new TimeoutTimer(this);
    }

    public TPARequest(CosmicPlayer sender, CosmicPlayer receiver, TransportType transportType, ResolveType resolveType) {
        this(sender, receiver, transportType, resolveType, receiver.getServerName());
    }

    public TPARequest(CosmicPlayer sender, CosmicPlayer receiver, TransportType transportType) {
        this(sender, receiver, transportType, ResolveType.CURRENT_LOCATION);
    }

    public boolean isSender(String uuid) {
        return sender.getUuid().equals(uuid);
    }

    public boolean isReceiver(String uuid) {
        return receiver.getUuid().equals(uuid);
    }

    public void perform() {
        CosmicPlayer from;
        CosmicPlayer to;

        if (transportType == TransportType.SENDER_TO_RECEIVER) {
            from = getSender();
            to = getReceiver();
        } else {
            from = getReceiver();
            to = getSender();
        }

        if (from == null || to == null) {
            return;
        }

        CosmicLocation location = to.getLocation();
        StreamlineUtilities.getInstance().logDebug("Performing TPA from " + from.getCurrentName() + " to " + to.getCurrentName() + " at " + location.asString());

        from.teleport(to);

        ModuleUtils.sendMessage(to, ModuleUtils.replaceAllPlayerBungee(to,
                StreamlineUtilities.getMessages().tpaPerformTo()
                        .replace("%this_from%", from.getCurrentName())
                        .replace("%this_to%", to.getCurrentName())
        ));
        ModuleUtils.sendMessage(from, ModuleUtils.replaceAllPlayerBungee(from,
                StreamlineUtilities.getMessages().tpaPerformFrom()
                        .replace("%this_from%", from.getCurrentName())
                        .replace("%this_to%", to.getCurrentName())
        ));

        EssentialsManager.removeTPARequest(this);

        this.timeoutTimer.cancel();
    }

    public void deny() {
        EssentialsManager.removeTPARequest(this);

        CosmicPlayer from;
        CosmicPlayer to;

        if (transportType == TransportType.SENDER_TO_RECEIVER) {
            from = getSender();
            to = getReceiver();
        } else {
            from = getReceiver();
            to = getSender();
        }

        if (from == null || to == null) {
            return;
        }

        // Maybe make this send a deny message?
//        ModuleUtils.sendMessage(to, ModuleUtils.replaceAllPlayerBungee(to,
//                StreamlineUtilities.getMessages().tpaPerformTo()
//                        .replace("%this_from%", from.getCurrentName())
//                        .replace("%this_to%", to.getCurrentName())
//        ));
//        ModuleUtils.sendMessage(from, ModuleUtils.replaceAllPlayerBungee(from,
//                StreamlineUtilities.getMessages().tpaPerformFrom()
//                        .replace("%this_from%", from.getCurrentName())
//                        .replace("%this_to%", to.getCurrentName())
//        ));
    }

    public void timeout() {
        EssentialsManager.removeTPARequest(this);

        CosmicPlayer from;
        CosmicPlayer to;

        if (transportType == TransportType.SENDER_TO_RECEIVER) {
            from = getSender();
            to = getReceiver();
        } else {
            from = getReceiver();
            to = getSender();
        }

        if (from == null || to == null) {
            return;
        }

        ModuleUtils.sendMessage(getReceiver(), ModuleUtils.replaceAllPlayerBungee(to,
                StreamlineUtilities.getMessages().tpaTimeoutTo()
                        .replace("%this_from%", from.getCurrentName())
                        .replace("%this_sender%", getSender().getCurrentName())
                        .replace("%this_to%", to.getCurrentName())
                        .replace("%this_receiver%", getReceiver().getCurrentName())
        ));
        ModuleUtils.sendMessage(getSender(), ModuleUtils.replaceAllPlayerBungee(from,
                StreamlineUtilities.getMessages().tpaTimeoutFrom()
                        .replace("%this_from%", from.getCurrentName())
                        .replace("%this_sender%", getSender().getCurrentName())
                        .replace("%this_to%", to.getCurrentName())
                        .replace("%this_receiver%", getReceiver().getCurrentName())
        ));
    }

    @Override
    public int compareTo(@NotNull TPARequest o) {
        return timeSent.compareTo(o.timeSent);
    }
}
