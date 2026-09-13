package host.plas.events;

import lombok.Getter;
import host.plas.essentials.TPARequest;

@Getter
public class TPATimeoutEvent extends UtilityEvent {
    final TPARequest request;

    public TPATimeoutEvent(TPARequest tpaRequest) {
        super();
        this.request = tpaRequest;
    }
}
