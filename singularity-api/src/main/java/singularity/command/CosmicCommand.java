package singularity.command;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.command.context.CommandArgument;
import singularity.command.context.CommandContext;
import singularity.command.result.CommandResult;
import singularity.configs.CommandResource;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.events.command.CommandResultedEvent;
import singularity.modules.ModuleUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Abstract base class for all cross-platform commands in Streamline. Provides the
 * registration lifecycle, permission checking, tab-completion scaffolding, and
 * placeholder-replacement helpers that concrete commands inherit.
 *
 * <p>Subclasses should override {@link #resultedRun} (preferred) or the deprecated
 * {@link #run(CommandContext)} to implement command logic.</p>
 */
@Getter
public abstract class CosmicCommand implements Comparable<CosmicCommand> {

    /**
     * The unique identifier used as the primary key in the command registry.
     * Defaults to the base name on construction.
     */
    @Setter
    private String identifier;

    /**
     * The human-readable label of this command, typically matching the owning
     * module's identifier.
     */
    @Setter
    private String label;

    /**
     * The primary command name (without a leading slash) that players use to invoke
     * this command.
     */
    @Setter
    private String base;

    /**
     * The permission node required to execute this command. Senders without this
     * permission receive the configured "invalid permissions" message.
     */
    @Setter
    private String permission;

    /**
     * Alternative names that also trigger this command. Each alias is registered
     * alongside the base name.
     */
    @Setter
    private String[] aliases;

    /**
     * The YAML-backed resource file that stores per-command configuration such as
     * the {@code basic.enabled} flag.
     */
    private final CommandResource commandResource;

    /**
     * Creates a {@code CosmicCommand} with a custom parent directory for its
     * {@link CommandResource} configuration file.
     *
     * @param label           the human-readable label (typically the owning module's identifier)
     * @param base            the primary command name
     * @param permission      the permission node required to execute the command
     * @param parentDirectory the directory in which the command's config file is stored
     * @param aliases         alternative names for this command
     */
    public CosmicCommand(String label, String base, String permission, File parentDirectory, String... aliases) {
        this.label = label;
        this.identifier = base;
        this.base = base;
        this.permission = permission;
        this.aliases = aliases;
        this.commandResource = new CommandResource(this, parentDirectory);
    }

    /**
     * Creates a {@code CosmicCommand} using the default global commands folder as the
     * parent directory for the command's configuration file.
     *
     * @param label      the human-readable label
     * @param base       the primary command name
     * @param permission the permission node required to execute the command
     * @param aliases    alternative names for this command
     */
    public CosmicCommand(String label, String base, String permission, String... aliases) {
        this(label, base, permission, Singularity.getMainCommandsFolder(), aliases);
    }

    /**
     * Registers this command with the platform via {@link CommandHandler} if the command
     * is currently enabled in its configuration file.
     */
    public void register() {
        if (! isEnabled()) return;

        CommandHandler.registerStreamlineCommand(this);
    }

    /**
     * Unregisters this command from the platform via {@link CommandHandler}. Does nothing
     * if the command is disabled and not currently registered.
     */
    public void unregister() {
        if (! isEnabled()) if (! CommandHandler.isStreamlineCommandRegistered(getIdentifier())) return;

        CommandHandler.unregisterStreamlineCommand(this);
    }

    /**
     * Entry point called by the platform adapter when a sender executes this command.
     * Performs permission checking, normalises the argument array, delegates to
     * {@link #resultedRun}, fires a {@link singularity.events.command.CommandResultedEvent},
     * and returns the final {@link CommandResult}.
     *
     * @param sender the entity that invoked the command
     * @param args   the raw argument strings; {@code null} is treated as an empty array
     * @return the {@link CommandResult} produced by the execution
     */
    public CommandResult<?> baseRun(CosmicSender sender, @Nullable String[] args) {
        if (! sender.hasPermission(getPermission())) {
            sender.sendMessage(MainMessagesHandler.MESSAGES.INVALID.PERMISSIONS.get());
            return CommandResult.Failure.get();
        }

        if (args == null) args = new String[] { "" };
        if (args.length < 1) args = new String[] { "" };

        CommandContext<CosmicCommand> context = new CommandContext<>(sender, this, notSet(), args);

        CommandResult<?> result = resultedRun(context);
        if (result != null) {
            context.setResult(result);
        }

        CommandResultedEvent<CosmicCommand> event = new CommandResultedEvent<>(context);
        event.fire();

        return context.getResult();
    }

    /**
     * Legacy execution hook accepting a sender and a raw argument array.
     *
     * @param sender the entity invoking the command
     * @param args   the raw argument strings
     * @deprecated Implement {@link #resultedRun} instead.
     */
    @Deprecated
    public void run(CosmicSender sender, @Nullable String[] args) {
        // Nothing.
    }

    /**
     * Legacy execution hook accepting a {@link CommandContext}. Delegates to
     * {@link #run(CosmicSender, String[])}.
     *
     * @param context the command context for this invocation
     * @deprecated Implement {@link #resultedRun} instead.
     */
    @Deprecated
    public void run(CommandContext<CosmicCommand> context) {
        run(context.getSender(), context.getArgs().stream().map(CommandArgument::getContent).toArray(String[]::new));
    }

    /**
     * The primary execution hook for subclasses. Override this method to implement
     * command logic and return a meaningful {@link CommandResult}. The default
     * implementation delegates to the deprecated {@link #run(CommandContext)} and
     * returns {@code null} (which leaves the result unchanged).
     *
     * @param context the command context for this invocation
     * @return the result of the command execution, or {@code null} to leave the context
     *         result unchanged
     */
    public CommandResult<?> resultedRun(CommandContext<CosmicCommand> context) {
        run(context);
        return null;
    }

    /**
     * Entry point called by the platform adapter to gather tab-completion suggestions.
     * Performs permission checking before delegating to {@link #doTabComplete(CommandContext)}.
     *
     * @param sender the entity requesting tab completions
     * @param args   the current argument array; {@code null} is treated as an empty array
     * @return a sorted set of completion suggestions, or an empty set if the sender lacks
     *         permission
     */
    public ConcurrentSkipListSet<String> baseTabComplete(CosmicSender sender, @Nullable String[] args) {
        if (! sender.hasPermission(getPermission())) return new ConcurrentSkipListSet<>();

        if (args == null) args = new String[] { "" };
        if (args.length < 1) args = new String[] { "" };
        return doTabComplete(new CommandContext<>(sender, this, notSet(), args));
    }

    /**
     * Legacy tab-completion hook accepting a sender and a raw argument array. Returns
     * an empty set by default.
     *
     * @param sender the entity requesting tab completions
     * @param args   the current argument array
     * @return a sorted set of completion suggestions
     * @deprecated Override {@link #doTabComplete(CommandContext)} instead.
     */
    @Deprecated
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender sender, @Nullable String[] args) {
        return new ConcurrentSkipListSet<>();
    }

    /**
     * The preferred tab-completion hook. Subclasses should override this to return
     * context-aware suggestions. The default implementation delegates to the deprecated
     * {@link #doTabComplete(CosmicSender, String[])}.
     *
     * @param context the command context for this tab-completion request
     * @return a sorted set of completion suggestions
     */
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        return doTabComplete(context.getSender(), context.getArgs().stream().map(CommandArgument::getContent).toArray(String[]::new));
    }

    /**
     * Replaces {@code %this_other%} and {@code %this_other_uuid%} in {@code base} with
     * {@code other}'s current name and UUID, then replaces {@code %this_sender%} and
     * {@code %this_sender_uuid%} via {@link #getWithOther(CosmicSender, String, String)},
     * and finally applies all of {@code other}'s placeholders.
     *
     * @param sender the command sender (provides sender-side placeholders)
     * @param base   the message template to process
     * @param other  the secondary sender whose name and UUID are substituted
     * @return the fully processed message string
     */
    public String getWithOther(CosmicSender sender, String base, CosmicSender other) {
        base = base.replace("%this_other%", other.getCurrentName());
        base = base.replace("%this_other_uuid%", other.getUuid());
        return ModuleUtils.replacePlaceholders(other, getWithOther(sender, base, other.getCurrentName()));
    }

    /**
     * Replaces {@code %this_sender%} and {@code %this_sender_uuid%} in {@code base} with
     * {@code sender}'s current name and UUID, then replaces {@code %this_other%} with
     * the plain {@code other} string via {@link #getWithOther(String, String)}, and
     * finally applies {@code sender}'s placeholders.
     *
     * @param sender the command sender whose name and UUID are substituted
     * @param base   the message template to process
     * @param other  the other party's name to substitute
     * @return the processed message string
     */
    public String getWithOther(CosmicSender sender, String base, String other) {
        base = base.replace("%this_sender%", sender.getCurrentName());
        base = base.replace("%this_sender_uuid%", sender.getUuid());
        return ModuleUtils.replacePlaceholders(sender, getWithOther(base, other));
    }

    /**
     * Replaces the {@code %this_other%} token in {@code base} with the given {@code other}
     * string. No placeholder API processing is applied.
     *
     * @param base  the message template containing {@code %this_other%}
     * @param other the string to substitute for {@code %this_other%}
     * @return the resulting message string
     */
    public String getWithOther(String base, String other) {
        return base.replace("%this_other%", other);
    }

    /**
     * Returns {@code true} if this command is currently registered with the platform
     * command registry.
     *
     * @return {@code true} if registered
     */
    public boolean isLoaded() {
        return CommandHandler.isStreamlineCommandRegistered(getIdentifier());
    }

    /**
     * Returns {@code true} if this command is enabled according to its YAML
     * configuration file ({@code basic.enabled}).
     *
     * @return {@code true} if enabled
     */
    public boolean isEnabled() {
        return getCommandResource().getResource().getBoolean("basic.enabled");
    }

    /**
     * Unregisters this command from the platform immediately, bypassing the enabled
     * check used by {@link #unregister()}.
     */
    public void disable() {
        CommandHandler.unregisterStreamlineCommand(this);
    }

    /**
     * {@inheritDoc}
     *
     * Compares commands lexicographically by their {@link #identifier} so they sort
     * naturally in skip-list collections.
     */
    @Override
    public int compareTo(@NotNull CosmicCommand o) {
        return CharSequence.compare(getIdentifier(), o.getIdentifier());
    }

    /**
     * Returns a sorted set of integer strings ranging from {@code -5} to {@code 5}
     * (inclusive), suitable for use as tab-completion suggestions.
     *
     * @return a sorted set of integer strings in the default range
     */
    public ConcurrentSkipListSet<String> getIntegerArgument() {
        return getIntegerArgument(-5, 5);
    }

    /**
     * Returns a sorted set of integer strings from {@code min} to {@code max} inclusive.
     * If {@code min} is greater than {@code max} they are swapped before iteration.
     *
     * @param min the lower bound of the range (inclusive)
     * @param max the upper bound of the range (inclusive)
     * @return a sorted set of integer strings covering the range
     */
    public ConcurrentSkipListSet<String> getIntegerArgument(int min, int max) {
        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        for (int i = min; i <= max; i ++) {
            r.add(String.valueOf(i));
        }

        return r;
    }

    /**
     * Returns a sorted set of double strings stepping from {@code -3.0} to {@code 3.0}
     * in increments of {@code 0.25}, suitable for tab-completion suggestions.
     *
     * @return a sorted set of double strings in the default range and step
     */
    public ConcurrentSkipListSet<String> getDoubleArgument() {
        return getDoubleArgument(-3.0, 3.0, 0.25);
    }

    /**
     * Returns a sorted set of double strings stepping through the given range with the
     * specified increment. If {@code min} is greater than {@code max} they are swapped.
     * The {@code max} value is always included as the last entry.
     *
     * @param min  the lower bound of the range (inclusive)
     * @param max  the upper bound of the range (inclusive)
     * @param step the increment between successive values
     * @return a sorted set of double strings covering the range
     */
    public ConcurrentSkipListSet<String> getDoubleArgument(double min, double max, double step) {
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        for (double i = min; i <= max; i += step) {
            r.add(String.valueOf(i));
        }
        r.add(String.valueOf(max));

        return r;
    }

    /**
     * Creates a generic {@link CommandResult} with the given key and value.
     *
     * @param <T>    the type of the result variable
     * @param key    the result key string
     * @param result the result value
     * @return a new {@link CommandResult} wrapping {@code key} and {@code result}
     */
    public static <T> CommandResult<T> result(String key, T result) {
        return new CommandResult<>(key, result);
    }

    /**
     * Returns the singleton {@link CommandResult.Success} instance.
     *
     * @return the success result
     */
    public static CommandResult.Success success() {
        return CommandResult.Success.get();
    }

    /**
     * Returns the singleton {@link CommandResult.Failure} instance.
     *
     * @return the failure result
     */
    public static CommandResult.Failure failure() {
        return CommandResult.Failure.get();
    }

    /**
     * Returns the singleton {@link CommandResult.Error} instance.
     *
     * @return the error result
     */
    public static CommandResult.Error error() {
        return CommandResult.Error.get();
    }

    /**
     * Returns the singleton {@link CommandResult.NotSet} instance, used as the initial
     * result before {@link #resultedRun} has completed.
     *
     * @return the not-set result
     */
    public static CommandResult.NotSet notSet() {
        return CommandResult.NotSet.get();
    }

    /**
     * Get a message with another sender's placeholders replaced.
     * @param type The type of sender relation. If FROM_IS_SENDER, 'from' is the sender and 'to' is the other. If TO_IS_SENDER, 'to' is the sender and 'from' is the other.
     * @param from The person the action is from. (Not the sender unless type is FROM_IS_SENDER.)
     * @param to The person the action is to. (Not the sender unless type is TO_IS_SENDER.)
     * @param message The message to replace.
     * @return The message with placeholders replaced.
     */
    public String getWithOther(SenderWithOther type, CosmicSender from, CosmicSender to, String message) {
        message = message
                .replace("%this_from%", from.getCurrentName())
                .replace("%this_to%", to.getCurrentName());
        CosmicSender sendTo = (type == SenderWithOther.FROM_IS_SENDER) ? from : to;
        CosmicSender other = (type == SenderWithOther.FROM_IS_SENDER) ? to : from;

        return getWithOther(sendTo, message, other);
    }
}
