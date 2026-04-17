package singularity.messages.builders;

import lombok.Getter;
import singularity.Singularity;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.messages.answered.ReturnableMessage;
import singularity.messages.proxied.ProxiedMessage;
import singularity.modules.ModuleUtils;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

/**
 * Builds and handles {@link singularity.messages.proxied.ProxiedMessage} payloads that
 * request placeholder parsing from the proxy side.
 *
 * <p>A backend server sends a {@link ReturnableMessage} via {@link #build}, embedding
 * the raw string to parse and the sender's UUID. The proxy intercepts the message,
 * resolves all placeholders via {@code ModuleUtils.replacePlaceholders}, and echoes
 * the parsed result back. The caller retrieves the result through {@link #parse}.</p>
 *
 * <p>The sub-channel identifier is {@value #subChannel}. Only proxy-side environments
 * should call {@link #handle}.</p>
 */
public class ProxyParseMessageBuilder {

    /**
     * The plugin-messaging sub-channel name used to route parse-request messages.
     */
    @Getter
    private static final String subChannel = "api-parse";

    /**
     * Constructs a {@link ReturnableMessage} that requests placeholder parsing of
     * {@code toParse} in the context of {@code user}, using {@code carrier} as the
     * transport vehicle.
     *
     * <p>The returned message is <em>not</em> sent automatically; call
     * {@link ReturnableMessage#send()} when ready.</p>
     *
     * @param carrier  the online {@link CosmicPlayer} used to deliver the plugin message
     * @param toParse  the raw string containing placeholders to be resolved by the proxy
     * @param user     the {@link CosmicSender} whose context is used for placeholder resolution
     * @return an unsent {@link ReturnableMessage} wrapping the parse request
     */
    public static ReturnableMessage build(CosmicPlayer carrier, String toParse, CosmicSender user) {
        ProxiedMessage r = new ProxiedMessage(carrier, Singularity.isProxy());

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", user.getUuid());
        r.write("parse", toParse);

        return new ReturnableMessage(r, false);
    }

    /**
     * Extracts the parsed string from a reply message returned by the proxy.
     *
     * @param answeredMessage the reply {@link singularity.messages.proxied.ProxiedMessage}
     *                        received in the {@link ReturnableMessage} answer callback
     * @return the placeholder-resolved string written under the {@code "parsed"} key
     */
    public static String parse(ProxiedMessage answeredMessage) {
        return answeredMessage.getString("parsed");
    }

    /**
     * Handles an incoming parse request on the proxy side by resolving all
     * placeholders in the embedded string and sending the result back to the
     * originating backend.
     *
     * <p>This method returns early with a warning if the current environment is not
     * a proxy, if the sub-channel does not match, or if the message is not
     * returnable-like (i.e. missing the correlation key).</p>
     *
     * @param in the incoming {@link singularity.messages.proxied.ProxiedMessage} to handle
     */
    public static void handle(ProxiedMessage in) {
        if (! Singularity.isProxy()) {
            MessageUtils.logDebug("Tried to handle a ProxiedMessage with sub-channel '" + in.getSubChannel() + "', but this is not a proxy.");
            return;
        }

        if (! in.getSubChannel().equals(getSubChannel())) {
            MessageUtils.logWarning("Data mis-match on ProxyMessageIn for '" + ServerConnectMessageBuilder.class.getSimpleName() + "'.");
            return;
        }
        if (! in.isReturnableLike()) {
            MessageUtils.logWarning("Tried to reply to a ProxiedMessage with sub-channel '" + in.getSubChannel() + "', but it was not ReturnableLike.");
            return;
        }

        String uuid = in.getString("user_uuid");
        String parse = in.getString("parse");
        String key = in.getString(ReturnableMessage.getKey());

//        MessageUtils.logInfo("ProxiedMessage in > uuid = '" + uuid + "', parse = '" + parse + "', key = '" + key + "'.");
        CosmicSender sender = UserUtils.getOrCreateSender(uuid).orElse(null);
        if (sender == null) {
            MessageUtils.logWarning("Could not find CosmicSender for UUID '" + uuid + "'.");
            return;
        }

        String parsed = ModuleUtils.replacePlaceholders(sender, parse);

        ProxiedMessage r = new ProxiedMessage(in.getCarrier(), Singularity.isProxy());

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", uuid);
        r.write("parse", parse);
        r.write("parsed", parsed);
        r.write(ReturnableMessage.getKey(), key);

        r.send();
    }
}
