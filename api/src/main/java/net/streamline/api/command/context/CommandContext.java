package net.streamline.api.command.context;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.command.result.CommandResult;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public class CommandContext<C extends StreamlineCommand> {
    private StreamlineUser sender;
    private C command;
    private String label;
    private ConcurrentSkipListSet<CommandArgument> args;
    private CommandResult<?> result;

    public CommandContext(StreamlineUser sender, C command, CommandResult<?> result, String... args) {
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
