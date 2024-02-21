package net.streamline.api.interfaces.audiences.real;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.interfaces.audiences.getters.SenderGetter;
import net.streamline.api.interfaces.audiences.messaging.ICommandable;
import net.streamline.api.interfaces.audiences.messaging.IConsolable;
import net.streamline.api.interfaces.audiences.messaging.IMessagable;
import net.streamline.api.interfaces.audiences.permissions.IPermissionHolder;

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
