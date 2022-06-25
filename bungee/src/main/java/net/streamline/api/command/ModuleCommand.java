package net.streamline.api.command;

import com.google.common.base.Preconditions;
import net.streamline.api.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a {@link Command} belonging to a module
 */
public final class ModuleCommand extends Command implements ModuleIdentifiableCommand {
    private final Module owningModule;
    private CommandExecutor executor;
    private TabCompleter completer;

    protected ModuleCommand(@NotNull String name, @NotNull Module owner) {
        super(name);
        this.executor = owner;
        this.owningModule = owner;
        this.usageMessage = "";
    }

    /**
     * Executes the command, returning its success
     *
     * @param sender Source object which is executing this command
     * @param commandLabel The alias of the command used
     * @param args All arguments passed to the command, split via ' '
     * @return true if the command was successful, otherwise false
     */
    @Override
    public boolean execute(@NotNull ICommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        boolean success = false;

        if (!owningModule.isEnabled()) {
            throw new CommandException("Cannot execute command '" + commandLabel + "' in module " + owningModule.getDescription().getFullName() + " - module is disabled.");
        }

        if (!testPermission(sender)) {
            return true;
        }

        try {
            success = executor.onCommand(sender, this, commandLabel, args);
        } catch (Throwable ex) {
            throw new CommandException("Unhandled exception executing command '" + commandLabel + "' in module " + owningModule.getDescription().getFullName(), ex);
        }

        if (!success && usageMessage.length() > 0) {
            for (String line : usageMessage.replace("<command>", commandLabel).split("\n")) {
                sender.sendMessage(line);
            }
        }

        return success;
    }

    /**
     * Sets the {@link CommandExecutor} to run when parsing this command
     *
     * @param executor New executor to run
     */
    public void setExecutor(@Nullable CommandExecutor executor) {
        this.executor = executor == null ? owningModule : executor;
    }

    /**
     * Gets the {@link CommandExecutor} associated with this command
     *
     * @return CommandExecutor object linked to this command
     */
    @NotNull
    public CommandExecutor getExecutor() {
        return executor;
    }

    /**
     * Sets the {@link TabCompleter} to run when tab-completing this command.
     * <p>
     * If no TabCompleter is specified, and the command's executor implements
     * TabCompleter, then the executor will be used for tab completion.
     *
     * @param completer New tab completer
     */
    public void setTabCompleter(@Nullable TabCompleter completer) {
        this.completer = completer;
    }

    /**
     * Gets the {@link TabCompleter} associated with this command.
     *
     * @return TabCompleter object linked to this command
     */
    @Nullable
    public TabCompleter getTabCompleter() {
        return completer;
    }

    /**
     * Gets the owner of this ModuleCommand
     *
     * @return Module that owns this command
     */
    @Override
    @NotNull
    public Module getModule() {
        return owningModule;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates to the tab completer if present.
     * <p>
     * If it is not present or returns null, will delegate to the current
     * command executor if it implements {@link TabCompleter}. If a non-null
     * list has not been found.
     * <p>
     * This method does not consider permissions.
     *
     * @throws CommandException if the completer or executor throw an
     *     exception during the process of tab-completing.
     * @throws IllegalArgumentException if sender, alias, or args is null
     */
    @NotNull
    @Override
    public List<String> tabComplete(@NotNull ICommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        Preconditions.checkArgument(sender != null, "Sender cannot be null");
        Preconditions.checkArgument(args != null, "Arguments cannot be null");
        Preconditions.checkArgument(alias != null, "Alias cannot be null");

        List<String> completions = null;
        try {
            if (completer != null) {
                completions = completer.onTabComplete(sender, this, alias, args);
            }
            if (completions == null && executor instanceof TabCompleter) {
                completions = ((TabCompleter) executor).onTabComplete(sender, this, alias, args);
            }
        } catch (Throwable ex) {
            StringBuilder message = new StringBuilder();
            message.append("Unhandled exception during tab completion for command '/").append(alias).append(' ');
            for (String arg : args) {
                message.append(arg).append(' ');
            }
            message.deleteCharAt(message.length() - 1).append("' in module ").append(owningModule.getDescription().getFullName());
            throw new CommandException(message.toString(), ex);
        }

        if (completions == null) {
            return super.tabComplete(sender, alias, args);
        }
        return completions;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(super.toString());
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        stringBuilder.append(", ").append(owningModule.getDescription().getFullName()).append(')');
        return stringBuilder.toString();
    }
}