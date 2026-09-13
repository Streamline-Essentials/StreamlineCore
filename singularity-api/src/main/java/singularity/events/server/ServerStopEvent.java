package singularity.events.server;

/**
 * Fired when the server begins its shutdown sequence.
 *
 * <p>This event extends {@link ServerLifecycleEvent} and is dispatched early
 * in the shutdown phase, giving listeners an opportunity to perform cleanup,
 * flush pending data, or notify connected systems before the process
 * terminates.</p>
 */
public class ServerStopEvent extends ServerLifecycleEvent {
}
