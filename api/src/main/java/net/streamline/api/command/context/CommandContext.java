package net.streamline.api.command.context;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.base.commands.ParseCommand;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.command.result.CommandResult;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public class CommandContext<C extends StreamlineCommand> {
    private StreamSender sender;
    private C command;
    private String label;
    private ConcurrentSkipListSet<CommandArgument> args;
    private CommandResult<?> result;

    public CommandContext(StreamSender sender, C command, CommandResult<?> result, String... args) {
        this.sender = sender;
        this.command = command;
        this.label = command.getLabel();
        this.result = result;
        this.args = getArgsFrom(args);
    }

    public CommandArgument getArg(int index) {
        return args.stream().filter(arg -> arg.getIndex() == index).findFirst().orElse(new CommandArgument());
    }

    public boolean isArgUsable(int index) {
        return args.stream().anyMatch(arg -> arg.getIndex() == index);
    }

    public boolean isConsole() {
        return sender.isConsole();
    }

    public boolean isPlayer() {
        return ! isConsole();
    }

    public void sendMessage(String message, boolean format) {
        sender.sendMessage(message, format);
    }

    public void sendMessage(String message) {
        sender.sendMessage(message);
    }

    public int getArgCount() {
        return args.size();
    }

    public boolean isEmpty() {
        return args.isEmpty();
    }

    public boolean hasArgs() {
        return ! isEmpty();
    }

    public boolean hasArg(int index) {
        return getArg(index).isEmpty();
    }

    public boolean isSenderArgUsable(int index) {
        return getSenderArg(index).isPresent();
    }

    public boolean isPlayerArgUsable(int index) {
        return getPlayerArg(index).isPresent();
    }

    public Optional<StreamSender> getSenderArg(int index) {
        String username = getStringArg(index);
        return UserUtils.getOrCreateSenderByName(username);
    }

    public StreamSender getSenderArgRequired(int index) {
        return getSenderArg(index).orElse(null);
    }

    public Optional<StreamPlayer> getPlayerArg(int index) {
        String username = getStringArg(index);
        return UserUtils.getOrCreatePlayerByName(username);
    }

    public StreamPlayer getPlayerArgRequired(int index) {
        return getPlayerArg(index).orElse(null);
    }

    public String getStringArg(int index) {
        return args.stream().filter(arg -> arg.getIndex() == index).findFirst().orElse(new CommandArgument()).getContent();
    }

    public Optional<Integer> getIntArg(int index) {
        try {
            return Optional.of(Integer.parseInt(getStringArg(index)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public Optional<Double> getDoubleArg(int index) {
        try {
            return Optional.of(Double.parseDouble(getStringArg(index)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public Optional<Float> getFloatArg(int index) {
        try {
            return Optional.of(Float.parseFloat(getStringArg(index)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public Optional<Long> getLongArg(int index) {
        try {
            return Optional.of(Long.parseLong(getStringArg(index)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public Optional<Short> getShortArg(int index) {
        try {
            return Optional.of(Short.parseShort(getStringArg(index)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public Optional<Byte> getByteArg(int index) {
        try {
            return Optional.of(Byte.parseByte(getStringArg(index)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public Optional<Boolean> getBooleanArg(int index) {
        try {
            return Optional.of(Boolean.parseBoolean(getStringArg(index)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static ConcurrentSkipListSet<CommandArgument> getArgsFrom(String... args) {
        ConcurrentSkipListSet<CommandArgument> arguments = new ConcurrentSkipListSet<>();
        for (int i = 0; i < args.length; i++) {
            arguments.add(new CommandArgument(i, args[i]));
        }

        return arguments;
    }

    public static ConcurrentSkipListSet<CommandArgument> getArgsFrom(String string) {
        String[] args = string.split(" ");

        return getArgsFrom(args);
    }
}
