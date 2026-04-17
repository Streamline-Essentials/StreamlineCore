package singularity.interfaces.audiences.messaging;

/**
 * Represents an audience member that can receive text messages.
 *
 * <p>Two message channels are provided: a formatted channel ({@link #sendMessage}) that
 * applies colour codes and platform-specific text processing, and a raw channel
 * ({@link #sendMessageRaw}) that transmits the string without any additional processing.</p>
 */
public interface IMessagable {

    /**
     * Sends a formatted message to this entity.
     *
     * <p>Implementations should apply colour-code translation and any other platform-specific
     * text transformations (e.g., MiniMessage parsing on Velocity) before delivering the
     * message.</p>
     *
     * @param message the message text, potentially containing colour or formatting codes
     */
    void sendMessage(String message);

    /**
     * Sends a message to this entity exactly as provided, without any colour-code translation
     * or additional processing.
     *
     * <p>Useful when the caller has already formatted the text, or when raw JSON/legacy
     * format strings must be delivered verbatim.</p>
     *
     * @param message the raw message text to deliver unchanged
     */
    void sendMessageRaw(String message);
}
