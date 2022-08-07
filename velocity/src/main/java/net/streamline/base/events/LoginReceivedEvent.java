package net.streamline.base.events;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.events.server.LoginEvent;
import net.streamline.api.savables.users.StreamlinePlayer;

public class LoginReceivedEvent extends LoginEvent {
    @Getter @Setter
    private ConnectionResult result;

    public LoginReceivedEvent(StreamlinePlayer resource) {
        super(resource);
        this.result = new ConnectionResult();
    }

    public static class ConnectionResult {
        @Getter @Setter
        private boolean cancelled;
        @Getter @Setter @NonNull
        private String disconnectMessage;

        public ConnectionResult() {
            cancelled = false;
            disconnectMessage = "";
        }

        public boolean validate() {
            if (isCancelled() && disconnectMessage.equals("")) {
                SLAPI.getInstance().getMessenger().logWarning("LoginReceivedEvent has an invalid ConnectionResult! This is due to being set to cancelled while the disconnectMessage is empty!");
                return false;
            }
            return true;
        }
    }
}
