package singularity.holders.builtin;

/**
 * Contract for integrating with the Geyser cross-play bridge, which allows
 * Bedrock Edition clients to connect to Java Edition servers.
 *
 * <p>Implementations wrap the Geyser API and provide utility methods for
 * identifying Bedrock players by their UUID or username and for translating
 * between the two identifier forms.</p>
 */
public interface CosmicGeyserHolder {

    /**
     * Returns {@code true} if the given UUID string belongs to a Bedrock
     * Edition player as identified by the Geyser bridge.
     *
     * @param uuid the UUID string to check; must not be {@code null}
     * @return {@code true} if the UUID is a Bedrock player UUID,
     *         {@code false} otherwise
     */
    public boolean isBedrockUUID(String uuid);

    /**
     * Returns {@code true} if the given player name belongs to a Bedrock
     * Edition player (typically identified by the Bedrock prefix returned by
     * {@link #getBedrockPrefix()}).
     *
     * @param name the player name to check; must not be {@code null}
     * @return {@code true} if the name is associated with a Bedrock player,
     *         {@code false} otherwise
     */
    public boolean isBedrockName(String name);

    /**
     * Returns the name prefix that Geyser prepends to Bedrock player usernames
     * to distinguish them from Java Edition players (e.g. {@code "."}).
     *
     * @return the Bedrock player name prefix configured in Geyser; never
     *         {@code null}
     */
    public String getBedrockPrefix();

    /**
     * Resolves the Bedrock Edition username associated with the given Bedrock
     * UUID string.
     *
     * @param uuid the Bedrock player UUID string; must not be {@code null}
     * @return the Bedrock username for the given UUID, or {@code null} if no
     *         matching Bedrock session is found
     */
    public String getUsernameFromBedrockUUID(String uuid);

    /**
     * Resolves the Bedrock Edition UUID associated with the given username.
     *
     * @param name the Bedrock player username (with or without the Geyser
     *             prefix); must not be {@code null}
     * @return the Bedrock UUID string for the given username, or {@code null}
     *         if no matching Bedrock session is found
     */
    public String getBedrockUUIDFromUsername(String name);
}
