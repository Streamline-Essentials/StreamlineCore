package singularity.interfaces.audiences.real;

import lombok.Getter;
import lombok.Setter;
import singularity.interfaces.audiences.getters.PlayerGetter;
import singularity.interfaces.audiences.messaging.IChatter;

@Getter @Setter
public abstract class RealPlayer<P> extends RealSender<P> implements IChatter {
    private final PlayerGetter<P> playerGetter;

    public RealPlayer(PlayerGetter<P> playerGetter) {
        super(playerGetter);
        this.playerGetter = playerGetter;
    }

    public P getPlayer() {
        return playerGetter.get();
    }

    public void sendConsoleMessageNonNull(String message) {
        // do nothing
    }

    public void sendLogMessage(String message) {
        // do nothing
    }
}
