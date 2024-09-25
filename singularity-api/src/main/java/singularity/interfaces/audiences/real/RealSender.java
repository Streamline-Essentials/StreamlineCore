package singularity.interfaces.audiences.real;

import lombok.Getter;
import lombok.Setter;
import singularity.interfaces.audiences.getters.SenderGetter;
import singularity.interfaces.audiences.messaging.ICommandable;
import singularity.interfaces.audiences.messaging.IConsolable;
import singularity.interfaces.audiences.messaging.IMessagable;
import singularity.interfaces.audiences.permissions.IPermissionHolder;

@Getter @Setter
public abstract class RealSender<C> implements IMessagable, ICommandable, IConsolable, IPermissionHolder {
    private final SenderGetter<C> senderGetter;

    public RealSender(SenderGetter<C> senderGetter) {
        this.senderGetter = senderGetter;
    }

    public C getConsole() {
        return senderGetter.get();
    }
}
