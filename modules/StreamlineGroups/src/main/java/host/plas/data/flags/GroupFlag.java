package host.plas.data.flags;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public enum GroupFlag {
    LEADER("Signifies that you are a leader in the group."),
    MODERATOR("Signifies that you are a moderator in the group."),

    INVITE("Signifies that you have invite permissions in the group."),
    DISBAND("Signifies that you have disband permissions in the group."),
    WARP("Signifies that you have warp permissions in the group."),
    PROMOTE("Signifies that you have promote permissions in the group.", "Only works if your role is above the victim."),
    DEMOTE("Signifies that you have demote permissions in the group.", "Only works if your role is above the victim."),
    CHAT("Signifies that you have chat permissions in the group."),
    MUTE("Signifies that you have mute permissions in the group."),
    MUTE_BYPASS("Signifies that you can bypass muted chat in the group."),
    ;

    @Getter
    private final String description;
    @Getter
    private final List<String> notes;

    GroupFlag(String description, String... notes) {
        this.description = description;
        this.notes = Arrays.stream(notes).collect(Collectors.toList());
    }

    public static GroupFlag get(String s) {
        return GroupFlag.valueOf(s.toUpperCase(Locale.ROOT));
    }
}
