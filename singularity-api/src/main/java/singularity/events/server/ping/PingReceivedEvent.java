package singularity.events.server.ping;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;
import singularity.objects.PingedResponse;

/**
 * Fired when the server receives a status ping from a client or external
 * service.
 *
 * <p>Listeners may modify the {@link PingedResponse} to customise the
 * server-list information (MOTD, player count, etc.) that is returned to the
 * pinging client.</p>
 */
@Setter
@Getter
public class PingReceivedEvent extends CosmicEvent {

    /**
     * The response object that will be sent back to the pinging client.
     * Modifications made to this object before the event finishes processing
     * are reflected in the actual ping reply.
     */
    private PingedResponse response;

    /**
     * The hostname used by the client when it sent the ping request. This may
     * differ from the server's canonical hostname when virtual hosting or
     * SRV records are in use.
     */
    private String hostname;

    /**
     * Constructs a new {@code PingReceivedEvent}.
     *
     * @param response the mutable response that will be returned to the
     *                 pinging client; must not be {@code null}
     * @param hostname the hostname string from the client's ping packet
     */
    public PingReceivedEvent(PingedResponse response, String hostname) {
        this.response = response;
        this.hostname = hostname;
    }
}
