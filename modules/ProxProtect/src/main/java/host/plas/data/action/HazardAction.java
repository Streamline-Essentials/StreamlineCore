package host.plas.data.action;

import gg.drak.thebase.lib.re2j.Matcher;
import gg.drak.thebase.objects.Identifiable;
import gg.drak.thebase.utils.MatcherUtils;
import host.plas.data.cause.HazardCause;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import singularity.data.console.CosmicSender;
import singularity.data.uuid.UuidManager;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public class HazardAction implements Identifiable {
    private String identifier;

    private HazardActionType type;
    private String value;

    public HazardAction(String identifier, HazardActionType type, String value) {
        this.identifier = identifier;
        this.type = type;
        this.value = value;
    }

    public void execute(HazardCause cause) {
        String finalValue = ModuleUtils.replacePlaceholders(cause.getPlayer(), value);

        switch (type) {
            case EXECUTE_CONSOLE_COMMAND:
                ModuleUtils.getConsole().runCommand(finalValue);
                break;
            case EXECUTE_PLAYER_COMMAND:
                cause.getPlayer().runCommand(finalValue);
                break;
            case KICK:
                SLAPI.getInstance().getUserManager().kick(cause.getPlayer(), finalValue);
                break;
            case MESSAGE:
                Matcher matcher = MatcherUtils.matcherBuilder("([(](.+)[)])[ ](.+)", finalValue);
                List<String[]> groups = MatcherUtils.getGroups(matcher, 3);
                for (String[] group : groups) {
                    String toM = group[1];
                    String message = group[2];

                    message = ModuleUtils.replacePlaceholders(cause.getPlayer(), message);

                    ConcurrentSkipListSet<CosmicSender> toMessage = new ConcurrentSkipListSet<>();

                    if (toM.startsWith("p:")) {
                        UserUtils.getLoadedSenders().forEach((s, sender) -> {
                            if (sender == null) return;
                            if (sender.hasPermission(toM.substring(2))) {
                                toMessage.add(sender);
                            }
                        });
                    } else {
                        UuidManager.getFromName(toM).ifPresent(uuid -> {
                            UserUtils.getLoadedSenders().forEach((s, sender) -> {
                                if (sender == null) return;
                                if (sender.getUuid().equals(uuid.toString())) {
                                    toMessage.add(sender);
                                }
                            });
                        });
                    }

                    final String messageFinal = message;

                    toMessage.forEach(sender -> sender.sendMessage(messageFinal));
                }

                break;
        }
    }
}
