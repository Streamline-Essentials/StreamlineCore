package singularity.events.server;

/**
 * Fired when the server completes its startup sequence and is ready to accept
 * connections.
 *
 * <p>This event extends {@link ServerLifecycleEvent} and is dispatched at the
 * end of the initialization phase. Listeners may use it to perform
 * post-startup logic such as scheduling tasks or loading resources that depend
 * on the server being fully online.</p>
 */
public class ServerStartEvent extends ServerLifecycleEvent {
}
