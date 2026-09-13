package singularity.events.server;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.utils.MessageUtils;

/**
 * Fired when the server first receives a player's login request, before the
 * login sequence is completed.
 *
 * <p>Listeners may inspect or modify the embedded {@link ConnectionResult} to
 * cancel the connection with a custom disconnect message, effectively blocking
 * the player from joining.</p>
 */
@Setter
@Getter
public class LoginReceivedEvent extends LoginEvent {

    /**
     * The mutable result object that controls whether this login attempt is
     * accepted or cancelled.
     */
    private ConnectionResult result;

    /**
     * Constructs a new {@code LoginReceivedEvent} for the given player with a
     * default {@link ConnectionResult} that permits the connection.
     *
     * @param player the player whose login request was received; must not be
     *               {@code null}
     */
    public LoginReceivedEvent(CosmicPlayer player) {
        super(player);
        this.result = new ConnectionResult();
    }

    /**
     * Holds the outcome of a login attempt, determining whether the player is
     * allowed to connect or should be disconnected with a message.
     */
    @Getter @Setter
    public static class ConnectionResult {

        /**
         * Whether the login has been cancelled. When {@code true}, the player
         * will be disconnected using {@link #disconnectMessage}.
         */
        private boolean cancelled;

        /**
         * The message sent to the player if the login is cancelled. Must not
         * be {@code null}; use an empty string when no specific message is set.
         */
        @NonNull
        private String disconnectMessage;

        /**
         * Constructs a default {@code ConnectionResult} that permits the
         * connection (not cancelled, empty disconnect message).
         */
        public ConnectionResult() {
            cancelled = false;
            disconnectMessage = "";
        }

        /**
         * Validates that this result is internally consistent.
         *
         * <p>A result is invalid when the login is cancelled but no disconnect
         * message has been provided. In that case a warning is logged and
         * {@code false} is returned.</p>
         *
         * @return {@code true} if the result is valid; {@code false} if the
         *         login is cancelled yet the disconnect message is empty
         */
        public boolean validate() {
            if (isCancelled() && disconnectMessage.isEmpty()) {
                MessageUtils.logWarning("LoginReceivedEvent has an invalid ConnectionResult! This is due to being set to cancelled while the disconnectMessage is empty!");
                return false;
            }
            return true;
        }
    }
}
