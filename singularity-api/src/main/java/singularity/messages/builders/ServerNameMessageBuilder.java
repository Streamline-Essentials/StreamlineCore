package singularity.messages.builders;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.players.CosmicPlayer;
import singularity.messages.proxied.ProxiedMessage;
import singularity.utils.MessageUtils;

import java.util.Date;

/**
 * Builds and handles {@link ProxiedMessage} payloads that propagate the proxy's
 * configured server name to backend servers.
 *
 * <p>Updates are rate-limited: a backend will only apply a new name if at least
 * {@link #getUpdateMillis()} milliseconds have elapsed since the last accepted update
 * (checked via {@link #isNeedUpdate()}).</p>
 *
 * <p>The sub-channel identifier defaults to {@code "server-name-setter"}.</p>
 */
public class ServerNameMessageBuilder {

    /**
     * The plugin-messaging sub-channel name used to route server-name update messages.
     */
    @Getter @Setter
    private static String subChannel = "server-name-setter";

    /**
     * Timestamp of the most recently applied server-name update, or {@code null} if
     * no update has been applied yet.
     */
    @Getter @Setter
    private static Date lastUpdate = null;

    /**
     * Constructs a {@link ProxiedMessage} that instructs the recipient to update
     * its local server name to {@code serverName}.
     *
     * <p>The message is always marked as proxy-originated when called from a proxy
     * environment.</p>
     *
     * @param carrier    the online {@link CosmicPlayer} used to deliver the plugin message
     * @param serverName the new server name to propagate
     * @return a fully populated {@link ProxiedMessage} ready to be sent
     */
    public static ProxiedMessage build(CosmicPlayer carrier, String serverName) {
        ProxiedMessage r = new ProxiedMessage(carrier, Singularity.isProxy()); // only run from proxy...

        r.setSubChannel(getSubChannel());
        r.write("serverName", serverName);

        return r;
    }

    /**
     * Processes an incoming server-name update message and, if an update is due,
     * persists the new name via {@link GivenConfigs#writeServerName(String)}.
     *
     * <p>Returns silently if the sub-channel does not match or the rate-limit window
     * has not yet elapsed. Logs a warning if the encoded server name is {@code null}.</p>
     *
     * @param in the incoming {@link ProxiedMessage} to process
     */
    public static void handle(ProxiedMessage in) {
        if (! in.getSubChannel().equals(getSubChannel())) return;
        if (! isNeedUpdate()) return;

        String serverName = in.getString("serverName");

        if (serverName == null) {
            MessageUtils.logWarning("Received an invalid server name update call: " + in.getLiteralAsString());
            return;
        }

        GivenConfigs.writeServerName(serverName);
        update();
    }

    /**
     * Returns {@code true} if no update has ever been applied or if the rate-limit
     * window ({@link #getUpdateMillis()} ms) has fully elapsed since the last one.
     *
     * @return {@code true} when a new server-name update should be accepted
     */
    public static boolean isNeedUpdate() {
        if (getLastUpdate() == null) return true;
        return (new Date().getTime() - getLastUpdate().getTime()) > getUpdateMillis();
    }

    /**
     * Records the current time as the most recent server-name update timestamp,
     * resetting the rate-limit window.
     */
    public static void update() {
        setLastUpdate(new Date());
    }

    /**
     * Returns the minimum number of milliseconds that must elapse between accepted
     * server-name updates. Currently 30 minutes.
     *
     * @return the update interval in milliseconds
     */
    public static long getUpdateMillis() {
        return 30000 * 60; // 30 minutes
    }
}
