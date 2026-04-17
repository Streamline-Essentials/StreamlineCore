package singularity.command.context;

import lombok.Getter;
import lombok.Setter;
import singularity.command.CosmicCommand;
import singularity.command.result.CommandResult;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Carries all information about a single command invocation: the sender, the
 * command being executed, the parsed arguments, and the result produced by the
 * execution. The type parameter {@code C} allows subclasses of {@link CosmicCommand}
 * to be carried without unchecked casts.
 *
 * @param <C> the specific {@link CosmicCommand} type associated with this context
 */
@Getter @Setter
public class CommandContext<C extends CosmicCommand> {

    /** The entity that invoked the command. */
    private CosmicSender sender;

    /** The command that was invoked. */
    private C command;

    /** The label used to invoke the command (may differ from the base name). */
    private String label;

    /** The ordered set of parsed arguments provided to the command. */
    private ConcurrentSkipListSet<CommandArgument> args;

    /** The result of the command execution, updated after {@code resultedRun} completes. */
    private CommandResult<?> result;

    /**
     * Creates a new {@code CommandContext}.
     *
     * @param sender  the entity that invoked the command
     * @param command the command being invoked
     * @param result  the initial (usually {@code notSet}) result
     * @param args    the raw string arguments provided by the sender
     */
    public CommandContext(CosmicSender sender, C command, CommandResult<?> result, String... args) {
        this.sender = sender;
        this.command = command;
        this.label = command.getLabel();
        this.result = result;
        this.args = getArgsFrom(args);
    }

    /**
     * Returns the {@link CommandArgument} at the given zero-based index, or a broken
     * placeholder argument if no argument exists at that position.
     *
     * @param index the zero-based argument index
     * @return the argument at {@code index}, or a broken placeholder
     */
    public CommandArgument getArg(int index) {
        return args.stream().filter(arg -> arg.getIndex() == index).findFirst().orElse(new CommandArgument());
    }

    /**
     * Returns {@code true} if an argument exists at {@code index} and its content is
     * {@link CommandArgument#isUsable() usable}.
     *
     * @param index the zero-based argument index
     * @return {@code true} if the argument is present and usable
     */
    public boolean isArgUsable(int index) {
        return args.stream().anyMatch(arg -> arg.getIndex() == index) && getArg(index).isUsable();
    }

    /**
     * Returns {@code true} if the sender is the server console rather than a player.
     *
     * @return {@code true} for a console sender
     */
    public boolean isConsole() {
        return sender.isConsole();
    }

    /**
     * Returns {@code true} if the sender is a player rather than the console.
     *
     * @return {@code true} for a player sender
     */
    public boolean isPlayer() {
        return ! isConsole();
    }

    /**
     * Sends a message to the command sender, optionally applying colour/placeholder
     * formatting.
     *
     * @param message the message text to send
     * @param format  {@code true} to apply formatting; {@code false} to send raw
     */
    public void sendMessage(String message, boolean format) {
        sender.sendMessage(message, format);
    }

    /**
     * Sends a formatted message to the command sender using the default formatting
     * behaviour.
     *
     * @param message the message text to send
     */
    public void sendMessage(String message) {
        sender.sendMessage(message);
    }

    /**
     * Returns the total number of arguments provided to this command invocation.
     *
     * @return the argument count
     */
    public int getArgCount() {
        return args.size();
    }

    /**
     * Returns all argument content strings as a plain array in index order.
     *
     * @return an array of argument content strings
     */
    public String[] getArgsArray() {
        return args.stream().map(CommandArgument::getContent).toArray(String[]::new);
    }

    /**
     * Returns {@code true} if the argument set is empty or if the first argument is
     * usable. Note: this method's name is somewhat counter-intuitive — prefer
     * {@link #hasArgs()} where clarity matters.
     *
     * @return {@code true} when there are no args or the first arg is usable
     */
    public boolean isEmpty() {
        return args.isEmpty() || isArgUsable(0);
    }

    /**
     * Returns {@code true} when {@link #isEmpty()} returns {@code false}.
     *
     * @return {@code true} if this context has arguments
     */
    public boolean hasArgs() {
        return ! isEmpty();
    }

    /**
     * Returns {@code true} if the argument at {@code index} is considered empty
     * (delegates to {@link CommandArgument#isEmpty()}).
     *
     * @param index the zero-based argument index to check
     * @return {@code true} if the argument at {@code index} is empty
     */
    public boolean hasArg(int index) {
        return getArg(index).isEmpty();
    }

    /**
     * Returns {@code true} if the string at {@code index} resolves to a known
     * {@link CosmicSender}.
     *
     * @param index the zero-based argument index containing the sender name
     * @return {@code true} if a sender was resolved from the argument at {@code index}
     */
    public boolean isSenderArgUsable(int index) {
        return getSenderArg(index).isPresent();
    }

    /**
     * Returns {@code true} if the string at {@code index} resolves to a known
     * {@link CosmicPlayer}.
     *
     * @param index the zero-based argument index containing the player name
     * @return {@code true} if a player was resolved from the argument at {@code index}
     */
    public boolean isPlayerArgUsable(int index) {
        return getPlayerArg(index).isPresent();
    }

    /**
     * Resolves the string argument at {@code index} to a {@link CosmicSender}, creating
     * the user entry if it does not yet exist.
     *
     * @param index the zero-based argument index containing the sender's name
     * @return an {@link Optional} containing the resolved sender, or empty if not found
     */
    public Optional<CosmicSender> getSenderArg(int index) {
        String username = getStringArg(index);
        return UserUtils.getOrCreateSenderByName(username);
    }

    /**
     * Resolves the string argument at {@code index} to a {@link CosmicSender}, returning
     * {@code null} if the sender cannot be resolved.
     *
     * @param index the zero-based argument index containing the sender's name
     * @return the resolved sender, or {@code null}
     */
    public CosmicSender getSenderArgRequired(int index) {
        return getSenderArg(index).orElse(null);
    }

    /**
     * Resolves the string argument at {@code index} to a {@link CosmicPlayer}, creating
     * the player entry if it does not yet exist.
     *
     * @param index the zero-based argument index containing the player's name
     * @return an {@link Optional} containing the resolved player, or empty if not found
     */
    public Optional<CosmicPlayer> getPlayerArg(int index) {
        String username = getStringArg(index);
        return UserUtils.getOrCreatePlayerByName(username);
    }

    /**
     * Resolves the string argument at {@code index} to a {@link CosmicPlayer}, returning
     * {@code null} if the player cannot be resolved.
     *
     * @param index the zero-based argument index containing the player's name
     * @return the resolved player, or {@code null}
     */
    public CosmicPlayer getPlayerArgRequired(int index) {
        return getPlayerArg(index).orElse(null);
    }

    /**
     * Returns the raw content string of the argument at {@code index}, or an empty
     * string if no argument exists at that position.
     *
     * @param index the zero-based argument index
     * @return the raw string content of the argument, or {@code ""}
     */
    public String getStringArg(int index) {
        return args.stream().filter(arg -> arg.getIndex() == index).findFirst().orElse(new CommandArgument()).getContent();
    }

    /**
     * Parses the argument at {@code index} as an {@link Integer}.
     *
     * @param index the zero-based argument index
     * @return an {@link Optional} containing the parsed integer, or empty if parsing fails
     */
    public Optional<Integer> getIntArg(int index) {
        try {
            return Optional.of(Integer.parseInt(getStringArg(index)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Parses the argument at {@code index} as a {@link Double}.
     *
     * @param index the zero-based argument index
     * @return an {@link Optional} containing the parsed double, or empty if parsing fails
     */
    public Optional<Double> getDoubleArg(int index) {
        try {
            return Optional.of(Double.parseDouble(getStringArg(index)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Parses the argument at {@code index} as a {@link Float}.
     *
     * @param index the zero-based argument index
     * @return an {@link Optional} containing the parsed float, or empty if parsing fails
     */
    public Optional<Float> getFloatArg(int index) {
        try {
            return Optional.of(Float.parseFloat(getStringArg(index)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Parses the argument at {@code index} as a {@link Long}.
     *
     * @param index the zero-based argument index
     * @return an {@link Optional} containing the parsed long, or empty if parsing fails
     */
    public Optional<Long> getLongArg(int index) {
        try {
            return Optional.of(Long.parseLong(getStringArg(index)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Parses the argument at {@code index} as a {@link Short}.
     *
     * @param index the zero-based argument index
     * @return an {@link Optional} containing the parsed short, or empty if parsing fails
     */
    public Optional<Short> getShortArg(int index) {
        try {
            return Optional.of(Short.parseShort(getStringArg(index)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Parses the argument at {@code index} as a {@link Byte}.
     *
     * @param index the zero-based argument index
     * @return an {@link Optional} containing the parsed byte, or empty if parsing fails
     */
    public Optional<Byte> getByteArg(int index) {
        try {
            return Optional.of(Byte.parseByte(getStringArg(index)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Parses the argument at {@code index} as a {@link Boolean} using
     * {@link Boolean#parseBoolean}.
     *
     * @param index the zero-based argument index
     * @return an {@link Optional} containing the parsed boolean, or empty if parsing fails
     */
    public Optional<Boolean> getBooleanArg(int index) {
        try {
            return Optional.of(Boolean.parseBoolean(getStringArg(index)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Converts a varargs array of raw strings into an ordered set of
     * {@link CommandArgument} instances, assigning sequential zero-based indices.
     *
     * @param args the raw argument strings
     * @return a sorted set of {@link CommandArgument} objects
     */
    public static ConcurrentSkipListSet<CommandArgument> getArgsFrom(String... args) {
        ConcurrentSkipListSet<CommandArgument> arguments = new ConcurrentSkipListSet<>();
        for (int i = 0; i < args.length; i++) {
            arguments.add(new CommandArgument(i, args[i]));
        }

        return arguments;
    }

    /**
     * Splits a single space-delimited string and delegates to
     * {@link #getArgsFrom(String...)} to produce an ordered argument set.
     *
     * @param string the space-delimited command argument string
     * @return a sorted set of {@link CommandArgument} objects
     */
    public static ConcurrentSkipListSet<CommandArgument> getArgsFrom(String string) {
        String[] args = string.split(" ");

        return getArgsFrom(args);
    }
}
