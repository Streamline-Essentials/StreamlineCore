package host.plas.collections.data;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * What claiming a level gives: console commands to run and a line describing them in menus.
 */
@Getter
public class LevelReward {
    public static final LevelReward NONE = new LevelReward("", new ArrayList<>());

    private final String description;
    private final List<String> commands;

    public LevelReward(String description, List<String> commands) {
        this.description = description;
        this.commands = commands;
    }

    public boolean isEmpty() {
        return commands.isEmpty() && description.isEmpty();
    }
}
