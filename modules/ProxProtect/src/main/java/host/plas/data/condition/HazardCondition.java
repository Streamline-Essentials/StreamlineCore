package host.plas.data.condition;

import gg.drak.thebase.objects.Identifiable;
import host.plas.data.cause.HazardCause;
import host.plas.data.cause.HazardCauseType;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter @Setter
public class HazardCondition implements Identifiable {
    private String identifier;

    private HazardConditionType type;
    private String value;

    private Optional<ConditionPage> pageOptional;

    public HazardCondition(String identifier, HazardConditionType type, String value) {
        this.identifier = identifier;
        this.type = type;
        this.value = value;

        this.pageOptional = Optional.empty();

        loadPage();
    }

    public void loadPage() {
        if (value.startsWith("http")) {
            if (pageOptional.isEmpty()) {
                pageOptional = Optional.of(new ConditionPage(value));
            }
        }
    }

    public boolean checkHazard(HazardCause cause) {
        return checkHazard(cause, value, false);
    }

    public boolean checkHazard(HazardCause cause, String value, boolean fromPage) {
        if (! fromPage && pageOptional.isPresent()) {
            return pageOptional.get().check(this, cause);
        }

        boolean isCommand = false;
        if (cause.getType() == HazardCauseType.CHAT) {
            isCommand = cause.getValue().startsWith("/");
        }

        String[] words = cause.getValue().split(" ");

        switch (type) {
            case MESSAGE_EQUALS:
                if (isCommand) return false;
                return cause.getValue().equals(value);
            case MESSAGE_CONTAINS:
                if (isCommand) return false;
                return cause.getValue().contains(value);
            case MESSAGE_STARTS_WITH:
                if (isCommand) return false;
                return cause.getValue().startsWith(value);
            case MESSAGE_ENDS_WITH:
                if (isCommand) return false;
                return cause.getValue().endsWith(value);
            case MESSAGE_REGEX:
                if (isCommand) return false;
                return cause.getValue().matches(value);
            case MESSAGE_EQUALS_CASE_INSENSITIVE:
                if (isCommand) return false;
                return cause.getValue().equalsIgnoreCase(value);
            case MESSAGE_CONTAINS_CASE_INSENSITIVE:
                if (isCommand) return false;
                return cause.getValue().toLowerCase().contains(value.toLowerCase());
            case MESSAGE_STARTS_WITH_CASE_INSENSITIVE:
                if (isCommand) return false;
                return cause.getValue().toLowerCase().startsWith(value.toLowerCase());
            case MESSAGE_ENDS_WITH_CASE_INSENSITIVE:
                if (isCommand) return false;
                return cause.getValue().toLowerCase().endsWith(value.toLowerCase());
            case MESSAGE_REGEX_CASE_INSENSITIVE:
                if (isCommand) return false;
                return cause.getValue().toLowerCase().matches(value.toLowerCase());
            case WORD_EQUALS:
                if (isCommand) return false;

                for (String word : words) {
                    if (word.equals(value)) return true;
                }

                return false;
            case WORD_CONTAINS:
                if (isCommand) return false;

                for (String word : words) {
                    if (word.contains(value)) return true;
                }

                return false;
            case WORD_STARTS_WITH:
                if (isCommand) return false;

                for (String word : words) {
                    if (word.startsWith(value)) return true;
                }

                return false;
            case WORD_ENDS_WITH:
                if (isCommand) return false;

                for (String word : words) {
                    if (word.endsWith(value)) return true;
                }

                return false;
            case WORD_REGEX:
                if (isCommand) return false;

                for (String word : words) {
                    if (word.matches(value)) return true;
                }

                return false;
            case WORD_EQUALS_CASE_INSENSITIVE:
                if (isCommand) return false;

                for (String word : words) {
                    if (word.toLowerCase().equalsIgnoreCase(value)) return true;
                }

                return false;
            case WORD_CONTAINS_CASE_INSENSITIVE:
                if (isCommand) return false;

                for (String word : words) {
                    if (word.toLowerCase().contains(value.toLowerCase())) return true;
                }

                return false;
            case WORD_STARTS_WITH_CASE_INSENSITIVE:
                if (isCommand) return false;

                for (String word : words) {
                    if (word.toLowerCase().startsWith(value.toLowerCase())) return true;
                }

                return false;
            case WORD_ENDS_WITH_CASE_INSENSITIVE:
                if (isCommand) return false;

                for (String word : words) {
                    if (word.toLowerCase().endsWith(value.toLowerCase())) return true;
                }

                return false;
            case WORD_REGEX_CASE_INSENSITIVE:
                if (isCommand) return false;

                for (String word : words) {
                    if (word.toLowerCase().matches(value.toLowerCase())) return true;
                }

                return false;
            case COMMAND_EQUALS:
                if (! isCommand) return false;
                return cause.getValue().equals(value);
            case COMMAND_CONTAINS:
                if (! isCommand) return false;
                return cause.getValue().contains(value);
            case COMMAND_STARTS_WITH:
                if (! isCommand) return false;
                return cause.getValue().startsWith(value);
            case COMMAND_ENDS_WITH:
                if (! isCommand) return false;
                return cause.getValue().endsWith(value);
            case COMMAND_REGEX:
                if (! isCommand) return false;
                return cause.getValue().matches(value);
            case COMMAND_EQUALS_CASE_INSENSITIVE:
                if (! isCommand) return false;
                return cause.getValue().equalsIgnoreCase(value);
            case COMMAND_CONTAINS_CASE_INSENSITIVE:
                if (! isCommand) return false;
                return cause.getValue().toLowerCase().contains(value.toLowerCase());
            case COMMAND_STARTS_WITH_CASE_INSENSITIVE:
                if (! isCommand) return false;
                return cause.getValue().toLowerCase().startsWith(value.toLowerCase());
            case COMMAND_ENDS_WITH_CASE_INSENSITIVE:
                if (! isCommand) return false;
                return cause.getValue().toLowerCase().endsWith(value.toLowerCase());
            case COMMAND_REGEX_CASE_INSENSITIVE:
                if (! isCommand) return false;
                return cause.getValue().toLowerCase().matches(value.toLowerCase());
            case SERVER_NAME_EQUALS:
                return cause.getPlayer().getServerName().equals(value);
            case SERVER_NAME_CONTAINS:
                return cause.getPlayer().getServerName().contains(value);
            case SERVER_NAME_STARTS_WITH:
                return cause.getPlayer().getServerName().startsWith(value);
            case SERVER_NAME_ENDS_WITH:
                return cause.getPlayer().getServerName().endsWith(value);
            case SERVER_NAME_REGEX:
                return cause.getPlayer().getServerName().matches(value);
            case SERVER_NAME_EQUALS_CASE_INSENSITIVE:
                return cause.getPlayer().getServerName().equalsIgnoreCase(value);
            case SERVER_NAME_CONTAINS_CASE_INSENSITIVE:
                return cause.getPlayer().getServerName().toLowerCase().contains(value.toLowerCase());
            case SERVER_NAME_STARTS_WITH_CASE_INSENSITIVE:
                return cause.getPlayer().getServerName().toLowerCase().startsWith(value.toLowerCase());
            case SERVER_NAME_ENDS_WITH_CASE_INSENSITIVE:
                return cause.getPlayer().getServerName().toLowerCase().endsWith(value.toLowerCase());
            case SERVER_NAME_REGEX_CASE_INSENSITIVE:
                return cause.getPlayer().getServerName().toLowerCase().matches(value.toLowerCase());
            case PLAYER_NAME_EQUALS:
                return cause.getPlayer().getCurrentName().equals(value);
            case PLAYER_NAME_CONTAINS:
                return cause.getPlayer().getCurrentName().contains(value);
            case PLAYER_NAME_STARTS_WITH:
                return cause.getPlayer().getCurrentName().startsWith(value);
            case PLAYER_NAME_ENDS_WITH:
                return cause.getPlayer().getCurrentName().endsWith(value);
            case PLAYER_NAME_REGEX:
                return cause.getPlayer().getCurrentName().matches(value);
            case PLAYER_NAME_EQUALS_CASE_INSENSITIVE:
                return cause.getPlayer().getCurrentName().equalsIgnoreCase(value);
            case PLAYER_NAME_CONTAINS_CASE_INSENSITIVE:
                return cause.getPlayer().getCurrentName().toLowerCase().contains(value.toLowerCase());
            case PLAYER_NAME_STARTS_WITH_CASE_INSENSITIVE:
                return cause.getPlayer().getCurrentName().toLowerCase().startsWith(value.toLowerCase());
            case PLAYER_NAME_ENDS_WITH_CASE_INSENSITIVE:
                return cause.getPlayer().getCurrentName().toLowerCase().endsWith(value.toLowerCase());
            case PLAYER_NAME_REGEX_CASE_INSENSITIVE:
                return cause.getPlayer().getCurrentName().toLowerCase().matches(value.toLowerCase());
            case PLAYER_UUID_EQUALS:
                return cause.getPlayer().getUuid().equals(value);
            case PLAYER_UUID_CONTAINS:
                return cause.getPlayer().getUuid().contains(value);
            case PLAYER_UUID_STARTS_WITH:
                return cause.getPlayer().getUuid().startsWith(value);
            case PLAYER_UUID_ENDS_WITH:
                return cause.getPlayer().getUuid().endsWith(value);
            case PLAYER_UUID_REGEX:
                return cause.getPlayer().getUuid().matches(value);
            case PLAYER_UUID_EQUALS_CASE_INSENSITIVE:
                return cause.getPlayer().getUuid().equalsIgnoreCase(value);
            case PLAYER_UUID_CONTAINS_CASE_INSENSITIVE:
                return cause.getPlayer().getUuid().toLowerCase().contains(value.toLowerCase());
            case PLAYER_UUID_STARTS_WITH_CASE_INSENSITIVE:
                return cause.getPlayer().getUuid().toLowerCase().startsWith(value.toLowerCase());
            case PLAYER_UUID_ENDS_WITH_CASE_INSENSITIVE:
                return cause.getPlayer().getUuid().toLowerCase().endsWith(value.toLowerCase());
            case PLAYER_UUID_REGEX_CASE_INSENSITIVE:
                return cause.getPlayer().getUuid().toLowerCase().matches(value.toLowerCase());
            case PLAYER_IP_EQUALS:
                return cause.getPlayer().getCurrentIp().equals(value);
            case PLAYER_IP_CONTAINS:
                return cause.getPlayer().getCurrentIp().contains(value);
            case PLAYER_IP_STARTS_WITH:
                return cause.getPlayer().getCurrentIp().startsWith(value);
            case PLAYER_IP_ENDS_WITH:
                return cause.getPlayer().getCurrentIp().endsWith(value);
            case PLAYER_IP_REGEX:
                return cause.getPlayer().getCurrentIp().matches(value);
            case PLAYER_IP_EQUALS_CASE_INSENSITIVE:
                return cause.getPlayer().getCurrentIp().equalsIgnoreCase(value);
            case PLAYER_IP_CONTAINS_CASE_INSENSITIVE:
                return cause.getPlayer().getCurrentIp().toLowerCase().contains(value.toLowerCase());
            case PLAYER_IP_STARTS_WITH_CASE_INSENSITIVE:
                return cause.getPlayer().getCurrentIp().toLowerCase().startsWith(value.toLowerCase());
            case PLAYER_IP_ENDS_WITH_CASE_INSENSITIVE:
                return cause.getPlayer().getCurrentIp().toLowerCase().endsWith(value.toLowerCase());
            case PLAYER_IP_REGEX_CASE_INSENSITIVE:
                return cause.getPlayer().getCurrentIp().toLowerCase().matches(value.toLowerCase());
            default:
                return false;
        }
    }
}
