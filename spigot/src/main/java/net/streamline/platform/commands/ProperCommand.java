package net.streamline.platform.commands;

import host.plas.bou.commands.BuildableCommand;
import host.plas.bou.commands.CommandBuilder;
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

/**
 * Spigot/Bukkit adapter that bridges a cross-platform {@link CosmicCommand}
 * to the Bukkit command system.
 *
 * <p>Implements both {@link org.bukkit.command.TabExecutor} and
 * {@link IProperCommand} so that Streamline's command handler can dispatch
 * execution and tab-completion through the standard Bukkit command pipeline.
 */
@Getter
public class ProperCommand extends BuildableCommand implements TabExecutor, IProperCommand {
    /**
     * The underlying cross-platform command that this adapter wraps.
     * Lombok generates a {@code getParent()} accessor via the class-level {@code @Getter}.
     */
    private final CosmicCommand parent;

    /**
     * Constructs a new {@code ProperCommand} wrapping the given
     * {@link CosmicCommand}, initialising the Bukkit command metadata
     * (description, usage, aliases) from the parent.
     *
     * @param parent the cross-platform command to wrap
     */
    public ProperCommand(CosmicCommand parent) {
        super(builder(parent));
        this.parent = parent;
    }

    /**
     * Creates a {@link CommandBuilder} pre-configured with the base name,
     * description, usage text, and aliases taken from the given
     * {@link CosmicCommand}.
     *
     * @param parent the cross-platform command providing metadata
     * @return a configured {@link CommandBuilder} ready to build this command
     */
    public static CommandBuilder builder(CosmicCommand parent) {
        return new CommandBuilder(parent.getBase(), StreamlineSpigot.getInstance())
                .setDescription("Not defined.")
                .setUsage("Not defined.")
                .setAliases(parent.getAliases());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link #execute(CommandSender, String, String[])} to run
     * the underlying {@link CosmicCommand}.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return execute(sender, label, args);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the sender to a {@link singularity.data.console.CosmicSender},
     * delegates to {@link CosmicCommand#baseTabComplete}, and filters the result
     * against the current argument text.
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link #onTabComplete} and sanitises the result by
     * removing {@code null} and blank entries.
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Registration on Spigot is handled externally via
     * {@link net.streamline.platform.BasePlugin#registerCommands(ProperCommand...)};
     * this method is intentionally a no-op.
     */
    @Override
    public void registerThis() {
//        try {
//            StreamlineSpigot.registerCommands(this);
//        } catch(Exception e) {
//            e.printStackTrace();
//        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Unregistration on Spigot is handled externally via
     * {@link net.streamline.platform.BasePlugin#unregisterCommands(String...)};
     * this method is intentionally a no-op.
     */
    @Override
    public void unregisterThis() {
//        try {
//            StreamlineSpigot.unregisterCommands(getParent().getBase());
//        } catch(Exception e) {
//            e.printStackTrace();
//        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the sender to a {@link singularity.data.console.CosmicSender},
     * invokes {@link CosmicCommand#baseRun}, and maps the resulting
     * {@link singularity.command.result.CommandResult} to a boolean return value
     * ({@code true} for success, {@code false} for error/failure/null).
     */
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
