package host.plas.configs.bits;

import gg.drak.thebase.objects.Identifiable;
import host.plas.StreamlineRedirect;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class ConfiguredReasonCheck implements Identifiable {
    private String identifier;
    private CheckType checkType;
    private boolean enabled;
    private boolean action; // true = allow, false = deny
    private boolean sayKickMessage;
    private boolean caseInsensitive;
    private List<String> list;

    public ConfiguredReasonCheck(String identifier, CheckType checkType, boolean enabled, boolean action, boolean sayKickMessage, boolean caseInsensitive, List<String> list) {
        this.identifier = identifier;
        this.checkType = checkType;
        this.enabled = enabled;
        this.action = action;
        this.sayKickMessage = sayKickMessage;
        this.caseInsensitive = caseInsensitive;
        this.list = list;
    }

    public ConfiguredReasonCheck(String identifier, CheckType checkType, boolean enabled, boolean action, boolean sayKickMessage, boolean caseInsensitive, String... list) {
        this(identifier, checkType, enabled, action, sayKickMessage, caseInsensitive, new ArrayList<>(List.of(list)));
    }

    public static boolean getActionFromString(String action) {
        switch (action.toLowerCase()) {
            case "allow":
            case "true":
            case "yes":
                return true;
            case "deny":
            case "false":
            case "no":
                return false;
            default:
                StreamlineRedirect.getInstance().logInfo("Invalid action: " + action + ". Defaulting to false.");
                return false;
        }
    }

    public static CheckType getCheckTypeFromString(String checkType) {
        switch (checkType.toLowerCase()) {
            case "equals":
                return CheckType.EQUALS;
            case "contains":
                return CheckType.CONTAINS;
            case "starts-with":
            case "starts_with":
                return CheckType.STARTS_WITH;
            case "ends-with":
            case "ends_with":
                return CheckType.ENDS_WITH;
            case "regex":
                return CheckType.REGEX;
            case "not-equals":
            case "not_equals":
                return CheckType.NOT_EQUALS;
            case "not-contains":
            case "not_contains":
                return CheckType.NOT_CONTAINS;
            case "not-starts-with":
            case "not_starts_with":
                return CheckType.NOT_STARTS_WITH;
            case "not-ends-with":
            case "not_ends_with":
                return CheckType.NOT_ENDS_WITH;
            default:
                return CheckType.valueOf(checkType.toUpperCase());
        }
    }

    public static ConfiguredReasonCheck of(String identifier, String checkType, boolean enabled, String action, boolean sayKickMessage, boolean caseInsensitive, List<String> list) {
        return new ConfiguredReasonCheck(identifier, getCheckTypeFromString(checkType), enabled, getActionFromString(action), sayKickMessage, caseInsensitive, list);
    }
}
