package host.plas.collections.data;

import lombok.Getter;
import singularity.Singularity;
import singularity.configs.ModularizedConfig;
import singularity.data.players.CosmicPlayer;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

    /**
     * Runs the reward's commands as this server's console. {@code %player%} and {@code %uuid%} are
     * the claiming player's; {@code replacements} holds further placeholder and value pairs.
     */
    public void run(CosmicPlayer player, String... replacements) {
        for (String command : commands) {
            String resolved = command
                    .replace("%player%", player.getCurrentName())
                    .replace("%uuid%", player.getUuid());
            for (int i = 0; i + 1 < replacements.length; i += 2) resolved = resolved.replace(replacements[i], replacements[i + 1]);
            if (resolved.startsWith("/")) resolved = resolved.substring(1);
            Singularity.getInstance().getUserManager().runAs(UserUtils.getConsole(), true, resolved);
        }
    }

    /**
     * Reads the rewards configured under {@code path}, each a numbered level holding a
     * {@code description} and a list of {@code commands}.
     */
    public static TreeMap<Integer, LevelReward> parse(ModularizedConfig config, String path) {
        config.reloadResource();
        TreeMap<Integer, LevelReward> rewards = new TreeMap<>();
        for (String key : config.singleLayerKeySet(path)) {
            int level;
            try {
                level = Integer.parseInt(key.trim());
            } catch (NumberFormatException e) {
                continue;
            }
            String levelPath = path + "." + key;
            String description = config.getResource().getString(levelPath + ".description");
            List<String> commands = config.getResource().getStringList(levelPath + ".commands");
            rewards.put(level, new LevelReward(description == null ? "" : description,
                    commands == null ? new ArrayList<>() : commands));
        }
        return rewards;
    }

    /** The reward for {@code level}: its own entry, or the highest configured one below it. */
    public static LevelReward pick(TreeMap<Integer, LevelReward> rewards, int level) {
        Map.Entry<Integer, LevelReward> entry = rewards.floorEntry(level);
        if (entry == null) entry = rewards.firstEntry();
        return entry == null ? NONE : entry.getValue();
    }
}
