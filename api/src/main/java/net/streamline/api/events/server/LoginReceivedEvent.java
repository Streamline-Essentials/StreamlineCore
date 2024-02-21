package net.streamline.api.events.server;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.utils.MessageUtils;

@Getter
public class LoginReceivedEvent extends LoginEvent {
    @Setter
    private ConnectionResult result;

    public LoginReceivedEvent(StreamPlayer player) {
        super(player);
        this.result = new ConnectionResult();
    }

    @Getter @Setter
    public static class ConnectionResult {
        private boolean cancelled;
        @NonNull
        private String disconnectMessage;

        public ConnectionResult() {
            cancelled = false;
            disconnectMessage = "";
        }

        public boolean validate() {
            if (isCancelled() && disconnectMessage.isEmpty()) {
                MessageUtils.logWarning("LoginReceivedEvent has an invalid ConnectionResult! This is due to being set to cancelled while the disconnectMessage is empty!");
                return false;
            }
            return true;
        }
    }
}
