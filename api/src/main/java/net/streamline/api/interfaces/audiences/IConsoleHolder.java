package net.streamline.api.interfaces.audiences;

import net.streamline.api.SLAPI;
import net.streamline.api.interfaces.audiences.real.RealSender;

public interface IConsoleHolder<C> {
    RealSender<C> getRealConsole();

    default C getConsole() {
        return getRealConsole().getConsole();
    }

    default void sendConsoleMessage(String message) {
        if (getConsole() != null) {
            try {
                getRealConsole().sendConsoleMessageNonNull(message);
            } catch (Exception e) {
                getRealConsole().sendLogMessage(SLAPI.getInstance().getMessenger().stripColor(message));
            }
        } else {
            getRealConsole().sendLogMessage(SLAPI.getInstance().getMessenger().stripColor(message));
        }
    }
}
