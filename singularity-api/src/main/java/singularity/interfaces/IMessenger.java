package singularity.interfaces;

import singularity.data.console.CosmicSender;
import singularity.objects.CosmicTitle;
import org.jetbrains.annotations.Nullable;

/**
 * Platform-agnostic messaging interface that handles sending formatted and raw messages,
 * titles, and color-code conversions to {@link CosmicSender} instances.
 *
 * <p>Implementations are platform-specific and handle the translation between the
 * Streamline API's color/formatting layer and the underlying platform's text API.</p>
 */
public interface IMessenger {

    /**
     * Sends a formatted message to the given recipient.
     *
     * <p>The message is processed through the platform's color/placeholder pipeline before
     * delivery. The sender context is derived from {@code to} itself.</p>
     *
     * @param to      the recipient; if {@code null} the message is discarded
     * @param message the raw message string, which may contain color codes or placeholders
     */
    void sendMessage(@Nullable CosmicSender to, String message);

    /**
     * Sends a formatted message to the given recipient, resolving placeholders relative to
     * the sender identified by {@code otherUUID}.
     *
     * @param to        the recipient; if {@code null} the message is discarded
     * @param otherUUID the UUID of the sender whose context is used for placeholder resolution
     * @param message   the raw message string, which may contain color codes or placeholders
     */
    void sendMessage(@Nullable CosmicSender to, String otherUUID, String message);

    /**
     * Sends a formatted message to the given recipient, resolving placeholders relative to
     * the provided sender object.
     *
     * @param to      the recipient; if {@code null} the message is discarded
     * @param other   the sender whose context is used for placeholder resolution
     * @param message the raw message string, which may contain color codes or placeholders
     */
    void sendMessage(@Nullable CosmicSender to, CosmicSender other, String message);

    /**
     * Sends a raw (unprocessed) message to the given recipient, bypassing color and
     * placeholder processing.
     *
     * @param to      the recipient; if {@code null} the message is discarded
     * @param message the literal message string to deliver
     */
    void sendMessageRaw(@Nullable CosmicSender to, String message);

    /**
     * Sends a raw message to the given recipient without placeholder or color processing,
     * using the sender identified by {@code otherUUID} as context.
     *
     * @param to        the recipient; if {@code null} the message is discarded
     * @param otherUUID the UUID of the contextual sender (unused for processing but passed to the platform)
     * @param message   the literal message string to deliver
     */
    void sendMessageRaw(@Nullable CosmicSender to, String otherUUID, String message);

    /**
     * Sends a raw message to the given recipient without placeholder or color processing,
     * using the provided sender object as context.
     *
     * @param to      the recipient; if {@code null} the message is discarded
     * @param other   the contextual sender (unused for processing but passed to the platform)
     * @param message the literal message string to deliver
     */
    void sendMessageRaw(@Nullable CosmicSender to, CosmicSender other, String message);

    /**
     * Displays a title and subtitle overlay to the specified user.
     *
     * @param user  the player to show the title to
     * @param title the {@link CosmicTitle} containing title text and timing parameters
     */
    void sendTitle(CosmicSender user, CosmicTitle title);

    /**
     * Translates legacy color codes (e.g., {@code &a}, {@code &l}) and any platform-specific
     * formatting codes present in the input string into the native format used by this platform.
     *
     * @param from the raw string potentially containing color codes
     * @return the formatted string ready for display
     */
    String codedString(String from);

    /**
     * Strips all color and formatting codes from the given string, returning plain text.
     *
     * @param string the string from which color codes should be removed
     * @return the plain-text representation of the input
     */
    String stripColor(String string);
}
