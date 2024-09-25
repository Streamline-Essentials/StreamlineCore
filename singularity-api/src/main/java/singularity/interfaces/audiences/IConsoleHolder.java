package singularity.interfaces.audiences;

import singularity.Singularity;
import singularity.interfaces.audiences.real.RealSender;

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
                getRealConsole().sendLogMessage(Singularity.getInstance().getMessenger().stripColor(message));
            }
        } else {
            getRealConsole().sendLogMessage(Singularity.getInstance().getMessenger().stripColor(message));
        }
    }
}
