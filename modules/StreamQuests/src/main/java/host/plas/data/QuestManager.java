package host.plas.data;

import host.plas.StreamQuests;
import host.plas.adapters.placeholderapi.PlaceholderAdapter;
import host.plas.configs.QuestConfig;
import host.plas.data.players.QuestPlayer;
import host.plas.data.require.Requirement;
import host.plas.data.require.RequirementType;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class QuestManager {
    public static void fireQuestEvent(String playerUuid, RequirementType type, String value, double amount) {
        CompletableFuture.runAsync(() -> {
            try {
                QuestPlayer player = StreamQuests.getLoader().getOrCreateAsync(playerUuid).join();

                player.addValue(type, value, amount);

                checkQuestRequirements(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void checkQuestRequirements(QuestPlayer player) {
        getQuests().forEach(quest -> {
            quest.checkPlayer(player);
        });
    }

    public static boolean checkQuestRequirements(QuestPlayer player, Quest quest) {
        if (hasCompletedQuest(player, quest)) return true;

        AtomicBoolean atomicBoolean = new AtomicBoolean(true);
        quest.getRequirements().forEach(requirement -> {
            if (requirement.getType() == RequirementType.PLACEHOLDER_VALUE) {
                Optional<Double> optional = parsePlaceholder(player, requirement);
                if (optional.isEmpty()) {
                    atomicBoolean.set(false);
                } else {
                    double value = optional.get();
                    if (value < requirement.getAmount()) {
                        atomicBoolean.set(false);
                    }
                }
            } else if (requirement.getType() == RequirementType.QUEST_COMPLETED) {
                Optional<Quest> optional = getQuest(requirement.getValue());
                if (optional.isEmpty()) {
                    atomicBoolean.set(false);
                } else {
                    Quest reqQuest = optional.get();
                    if (! hasCompletedQuest(player, reqQuest)) {
                        atomicBoolean.set(false);
                    }
                }
            } else if (requirement.getType() == RequirementType.ITEMS_IN_INVENTORY) {
                Player bukkitPlayer = Bukkit.getPlayer(UUID.fromString(player.getIdentifier()));
                if (bukkitPlayer == null) {
                    atomicBoolean.set(false);
                } else {
                    boolean hasItems = requirement.getItems().values().stream().allMatch(item -> {
                        if (requirement.getAmount() < 0) {
                            return bukkitPlayer.getInventory().contains(item);
                        } else {
                            return bukkitPlayer.getInventory().containsAtLeast(item, (int) requirement.getAmount());
                        }
                    });
                    if (! hasItems) {
                        atomicBoolean.set(false);
                    }
                }
            } else {
                double currentAmount = getCurrentQuestValue(player, requirement.getType(), requirement.getValue()).orElse(0.0);
                if (currentAmount < requirement.getAmount()) {
                    atomicBoolean.set(false);
                }
            }
        });

        return atomicBoolean.get();
    }

    public static Optional<Double> parsePlaceholder(QuestPlayer player, Requirement requirement) {
        if (requirement.getType() != RequirementType.PLACEHOLDER_VALUE) return Optional.empty();

        String placeholder = requirement.getValue();
        String value = PlaceholderAdapter.replace(player.getIdentifier(), placeholder);

        double valueAmount = -1d;
        try {
            valueAmount = Double.parseDouble(value);

            return Optional.of(valueAmount);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static void addCompletedQuest(QuestPlayer player, Quest quest) {
        player.completeQuest(quest);
    }

    public static boolean hasCompletedQuest(QuestPlayer player, Quest quest) {
        return player.hasCompletedQuest(quest);
    }

    public static Optional<Double> getCurrentQuestValue(QuestPlayer player, RequirementType type, String value) {
        return player.getValue(type, value);
    }

    public static ConcurrentSkipListSet<Quest> getQuests() {
        return QuestConfig.getQuests();
    }

    public static Optional<Quest> getQuest(String identifier) {
        return getQuests().stream().filter(quest -> quest.getIdentifier().equals(identifier)).findFirst();
    }

    public static ConcurrentSkipListSet<Quest> getPlayerCompletedQuests(QuestPlayer player) {
        ConcurrentSkipListSet<Quest> quests = new ConcurrentSkipListSet<>();

        player.getCompletedQuests().forEach((string, date) -> {
            getQuest(string).ifPresent(quests::add);
        });

        return quests;
    }

    public static void runCommands(QuestPlayer player, List<String> commands) {
        CosmicSender sender = player.asSender().orElse(null);
        if (sender == null) return;

        for (String command : commands) {
            runCommand(sender, command);
        }
    }

    public static void runCommand(CosmicSender mainSender, String command) {
        if (command.startsWith("@c")) {
            ModuleUtils.getConsole().runCommand(padCommand(mainSender, command));
        }
        if (command.startsWith("@s")) {
            mainSender.runCommand(padCommand(mainSender, command));
        }
        if (command.startsWith("@o")) {
            mainSender.runCommand(padCommand(mainSender, command));
        }
        if (command.startsWith("@u")) {
            String part = command.split(" ", 2)[0];
            part = part.substring("@u:\"".length());
            part = part.substring(0, part.length() - "\"".length());
            String uuid = part;
            CosmicSender sender = UserUtils.getOrCreateSender(uuid).orElse(null);
            if (sender == null) return;

            if (sender.isConsole()) {
                ModuleUtils.getConsole().runCommand(padCommand(mainSender, command));
                return;
            }
            sender.runCommand(padCommand(mainSender, command));
        }
        if (command.startsWith("@n")) {
            String part = command.split(" ", 2)[0];
            part = part.substring("@n:\"".length());
            part = part.substring(0, part.length() - "\"".length());
            String name = part;
            UserUtils.getUUIDFromName(name).ifPresent(uuid -> {
                CosmicSender sender = UserUtils.getOrCreateSender(uuid).orElse(null);
                if (sender == null) return;

                if (sender.isConsole()) {
                    ModuleUtils.getConsole().runCommand(padCommand(mainSender, command));
                    return;
                }
                sender.runCommand(padCommand(mainSender, command));
            });
        }
    }

    public static String padCommand(CosmicSender mainSender, String command) {
        String cmd = command;
        if (cmd.startsWith("@c") || cmd.startsWith("@s") || cmd.startsWith("@o") || cmd.startsWith("@u") || cmd.startsWith("@n")) {
            String[] split = cmd.split(" ", 2);
            cmd = split[split.length - 1];
        }

        while (cmd.startsWith(" ")) {
            cmd = cmd.substring(1);
        }

        cmd = ModuleUtils.replacePlaceholders(mainSender, cmd);

        return cmd;
    }
}
