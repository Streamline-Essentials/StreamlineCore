package net.streamline.platform.commands;

import host.plas.bou.utils.SenderUtils;
import lombok.Getter;
import net.streamline.base.StreamlineSpigot;
import net.streamline.platform.savables.UserManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import singularity.command.CosmicCommand;
import singularity.command.result.CommandResult;
import singularity.data.console.CosmicSender;
import singularity.interfaces.IProperCommand;
import singularity.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Getter
public class ProperCommand extends BukkitCommand implements TabExecutor, IProperCommand {
    private final CosmicCommand parent;

    public ProperCommand(CosmicCommand parent) {
        super(parent.getBase(), "Not defined.", "Not defined.", List.of(parent.getAliases()));
        this.parent = parent;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return execute(sender, label, args);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        try {
            if (args == null) args = new String[]{""};
            if (args.length < 1) args = new String[]{""};

            CosmicSender s = UserManager.getInstance().getOrCreateSender(sender).orElse(null);
            if (s == null) {
                MessageUtils.logWarning("Cannot tab complete for command '" + label + "' as the sender is not a CosmicSender.");
                return new ArrayList<>();
            }

            ConcurrentSkipListSet<String> r = parent.baseTabComplete(s, args);

            return r == null ? new ArrayList<>() : new ArrayList<>(MessageUtils.getCompletion(r, args[args.length - 1]));
        } catch (Throwable e) {
            MessageUtils.logWarning("An error occurred while tab completing command '" + label + "': " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String @NotNull [] args) throws IllegalArgumentException {
        try {
            List<String> completions = onTabComplete(sender, this, alias, args);
            if (completions == null) {
                return new ArrayList<>();
            }

            return completions.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Throwable e) {
            MessageUtils.logWarning("An error occurred while tab completing command '" + alias + "': " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void register() {
        try {
            StreamlineSpigot.registerCommands(this);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void unregister() {
        try {
            StreamlineSpigot.unregisterCommands(getParent().getBase());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        try {
            CosmicSender s = UserManager.getInstance().getOrCreateSender(sender).orElse(null);
            if (s == null) {
                MessageUtils.logWarning("Cannot execute command '" + commandLabel + "' as the sender is not a CosmicSender.");
                return false;
            }

            CommandResult<?> result = parent.baseRun(s, args);

            if (result == null) return false;
            if (result == CosmicCommand.notSet()) return true;
            if (result == CosmicCommand.error()) return false;
            if (result == CosmicCommand.failure()) return false;
            return result == CosmicCommand.success();
        } catch (Throwable e) {
            SenderUtils.getSender(sender).sendMessage("&cAn error occurred while executing the command &7'&e" + commandLabel + "&7'&8. &cPlease tell an admin to check the console.");

            MessageUtils.logWarning("An error occurred while executing command '" + commandLabel + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
