package host.plas.events;

import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;

@Getter @Setter
public class NicknameUpdateEvent extends UtilityEvent {
    private CosmicSender user;
    private String changeTo;
    private String changeFrom;

    public NicknameUpdateEvent(CosmicSender user, String changeTo, String changeFrom) {
        super();

        this.user = user;
        this.changeTo = changeTo;
        this.changeFrom = changeFrom;
    }
}
