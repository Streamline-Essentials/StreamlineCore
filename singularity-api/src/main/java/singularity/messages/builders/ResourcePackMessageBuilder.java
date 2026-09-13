package singularity.messages.builders;

import lombok.Getter;
import singularity.data.players.CosmicPlayer;
import singularity.messages.proxied.ProxiedMessage;
import singularity.objects.SingleSet;
import singularity.objects.CosmicResourcePack;
import singularity.utils.MessageUtils;
import org.apache.commons.codec.binary.Hex;

/**
 * Builds and decodes {@link ProxiedMessage} payloads that carry resource-pack
 * application instructions between the proxy and backend servers.
 *
 * <p>Use {@link #build} to create a message encoding a {@link CosmicResourcePack} for a
 * target player, and {@link #unbuild} to decode such a message on the receiving end.</p>
 *
 * <p>The sub-channel identifier is {@value #subChannel}.</p>
 */
public class ResourcePackMessageBuilder {

    /**
     * The plugin-messaging sub-channel name used to route resource-pack messages.
     */
    @Getter
    private static final String subChannel = "resource-pack";

    /**
     * Encodes a {@link CosmicResourcePack} into a {@link ProxiedMessage} targeted
     * at the given user.
     *
     * <p>The resource-pack hash is hex-encoded for transport.</p>
     *
     * @param carrier          the online {@link CosmicPlayer} used to deliver the plugin message
     * @param isProxyOriginated {@code true} if this message originates from the proxy side
     * @param user             the {@link CosmicPlayer} who should receive the resource pack
     * @param resourcePack     the {@link CosmicResourcePack} to encode
     * @return a fully populated {@link ProxiedMessage} ready to be sent
     */
    public static ProxiedMessage build(CosmicPlayer carrier, boolean isProxyOriginated, CosmicPlayer user, CosmicResourcePack resourcePack) {
        ProxiedMessage r = new ProxiedMessage(carrier, isProxyOriginated);

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", user.getUuid());
        r.write("url", resourcePack.getUrl());
        r.write("prompt", resourcePack.getPrompt());
        r.write("hash", Hex.encodeHexString(resourcePack.getHash()));
        r.write("force", String.valueOf(resourcePack.isForce()));

        return r;
    }

    /**
     * Decodes a {@link ProxiedMessage} and reconstructs the target player UUID
     * and the associated {@link CosmicResourcePack}.
     *
     * <p>A warning is logged if the sub-channel does not match, but decoding
     * continues anyway. If the hex hash cannot be decoded an empty byte array
     * is used in its place.</p>
     *
     * @param messageIn the incoming {@link ProxiedMessage} to decode
     * @return a {@link SingleSet} whose first element is the target player UUID and
     *         whose second element is the decoded {@link CosmicResourcePack}
     */
    public static SingleSet<String, CosmicResourcePack> unbuild(ProxiedMessage messageIn) {
        if (! messageIn.getSubChannel().equals(getSubChannel())) {
            MessageUtils.logWarning("Data mis-match on ProxyMessageIn for '" + ResourcePackMessageBuilder.class.getSimpleName() + "'. Continuing anyway...");
        }

        String uuid = messageIn.getString("user_uuid");
        String url = messageIn.getString("url");
        String prompt = messageIn.getString("url");
        String unparsed = messageIn.getString("hash");
        byte[] hash;
        try {
            if (unparsed.equals("")) {
                hash = new byte[0];
            } else {
                hash = Hex.decodeHex(unparsed.toCharArray());
            }
        } catch (Exception e) {
            e.printStackTrace();
            hash = new byte[0];
        }
        boolean force = messageIn.getBoolean("force");

        return new SingleSet<>(uuid, new CosmicResourcePack(url, hash, prompt, force));
    }
}
