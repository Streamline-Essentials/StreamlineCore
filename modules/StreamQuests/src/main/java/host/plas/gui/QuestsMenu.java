package host.plas.gui;

import host.plas.adapters.placeholderapi.PlaceholderAdapter;
import host.plas.data.Quest;
import host.plas.data.QuestManager;
import host.plas.data.players.QuestPlayer;
import host.plas.data.require.Requirement;
import host.plas.data.require.RequirementType;
import host.plas.data.reward.Reward;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicItem;
import singularity.gui.GuiIcon;
import singularity.gui.GuiLayout;
import singularity.gui.PaginatedGui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Every configured quest, paged, with the viewer's progress on each requirement, its rewards, and
 * whether (and when) they completed it.
 */
public class QuestsMenu extends PaginatedGui {
    private static final int ROWS = 6;
    private static final int SUMMARY_SLOT = 4;

    private final QuestPlayer progress;

    public QuestsMenu(QuestPlayer progress) {
        super("&6&lQuests", ROWS);
        this.progress = progress;
    }

    @Override
    protected void drawFrame(CosmicPlayer viewer) {
        border(CosmicItem.of("minecraft:gray_stained_glass_pane", " "));

        int total = QuestManager.getQuests().size();
        int completed = 0;
        for (Quest quest : QuestManager.getQuests()) if (progress.hasCompletedQuest(quest)) completed++;

        setItem(SUMMARY_SLOT, CosmicItem.of("minecraft:nether_star", "&e&lYour Quests",
                "&7Completed&8: &f" + completed + "&8/&f" + total,
                "&7Points&8: &f" + number(progress.getPoints())));
        setIcon(GuiLayout.bottomCenter(ROWS), GuiIcon.close(CosmicItem.of("minecraft:barrier", "&cClose")));
    }

    @Override
    protected List<GuiIcon> buildEntries(CosmicPlayer viewer) {
        List<GuiIcon> entries = new ArrayList<>();
        for (Quest quest : QuestManager.getQuests()) entries.add(GuiIcon.of(icon(quest)));
        return entries;
    }

    private CosmicItem icon(Quest quest) {
        boolean done = progress.hasCompletedQuest(quest);

        List<String> lore = new ArrayList<>();
        if (! quest.getRequirements().isEmpty()) {
            lore.add("&7Requirements&8:");
            for (Requirement requirement : quest.getRequirements()) lore.add(requirementLine(requirement, done));
        }

        List<String> rewards = new ArrayList<>();
        for (Reward reward : quest.getRewards()) {
            reward.getPoints().ifPresent(points -> rewards.add("&8- &f" + number(points) + " &7points"));
        }
        if (! rewards.isEmpty()) {
            lore.add("");
            lore.add("&7Rewards&8:");
            lore.addAll(rewards);
        }

        lore.add("");
        if (done) {
            Date when = progress.getCompletedQuests().get(quest.getIdentifier());
            lore.add("&a&lCOMPLETED" + (when == null ? "" : " &7on &f" + new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(when)));
        } else {
            lore.add("&e&lIN PROGRESS");
        }

        CosmicItem item = CosmicItem.of(done ? "minecraft:enchanted_book" : "minecraft:book", "&f" + quest.getPrettyName())
                .setLore(lore);
        item.setGlowing(done);
        return item;
    }

    private String requirementLine(Requirement requirement, boolean questDone) {
        RequirementType type = requirement.getType();
        String label = describe(requirement);

        if (type == RequirementType.ITEMS_IN_INVENTORY) {
            return mark(questDone) + " &7" + label;
        }
        if (type == RequirementType.QUEST_COMPLETED) {
            boolean met = questDone || progress.hasCompletedQuest(requirement.getValue());
            return mark(met) + " &7" + label;
        }

        double current;
        if (type == RequirementType.PLACEHOLDER_VALUE) {
            current = placeholderValue(requirement).orElse(0d);
        } else {
            current = progress.getValue(type, requirement.getValue()).orElse(0d);
        }
        double goal = requirement.getAmount();
        boolean met = questDone || current >= goal;
        double shown = met ? Math.max(current, goal) : current;
        return mark(met) + " &7" + label + "&8: &f" + number(shown) + "&8/&f" + number(goal);
    }

    private static String describe(Requirement requirement) {
        String value = requirement.getValue() == null ? "" : requirement.getValue();
        switch (requirement.getType()) {
            case QUEST_COMPLETED:
                return "Complete " + QuestManager.getQuest(value).map(Quest::getPrettyName).orElse(value);
            case ITEMS_IN_INVENTORY:
                return "Bring the required items";
            case PLACEHOLDER_VALUE:
                return "Reach " + value;
            case DEATHS:
                return "Die";
            default:
                String type = title(requirement.getType().name());
                return value.isEmpty() ? type : type + " &8(&f" + title(value) + "&8)";
        }
    }

    /** The requirement's placeholder read as a number, or empty when it does not resolve to one. */
    private Optional<Double> placeholderValue(Requirement requirement) {
        try {
            return Optional.of(Double.parseDouble(PlaceholderAdapter.replace(progress.getIdentifier(), requirement.getValue()).trim()));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static String mark(boolean met) {
        return met ? "&a✔" : "&c✘";
    }

    /** {@code BREAK_BLOCK} or {@code minecraft:oak_log} as {@code Break Block} / {@code Oak Log}. */
    private static String title(String raw) {
        String text = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
        StringBuilder out = new StringBuilder();
        for (String word : text.toLowerCase(Locale.ROOT).split("[_\\s]+")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    private static String number(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) return String.format(Locale.ENGLISH, "%,d", (long) value);
        return String.format(Locale.ENGLISH, "%,.2f", value);
    }
}
