package singularity.events.server;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.utils.MessageUtils;

@Setter
@Getter
public class LoginReceivedEvent extends LoginEvent {
    private ConnectionResult result;

    public LoginReceivedEvent(CosmicPlayer player) {
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
