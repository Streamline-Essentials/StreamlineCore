package net.streamline.api.command;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.command.context.CommandArgument;
import net.streamline.api.command.context.CommandContext;
import net.streamline.api.command.result.CommandResult;
import net.streamline.api.configs.CommandResource;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.events.command.CommandResultedEvent;
import net.streamline.api.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public abstract class StreamlineCommand implements Comparable<StreamlineCommand> {
    @Setter
    private String identifier;
    @Setter
    private String label;
    @Setter
    private String base;
    @Setter
    private String permission;
    @Setter
    private String[] aliases;
    private final CommandResource commandResource;

    public StreamlineCommand(String label, String base, String permission, File parentDirectory, String... aliases) {
        this.label = label;
        this.identifier = base;
        this.base = base;
        this.permission = permission;
        this.aliases = aliases;
        this.commandResource = new CommandResource(this, parentDirectory);
    }

    public StreamlineCommand(String label, String base, String permission, String... aliases) {
        this(label, base, permission, SLAPI.getMainCommandsFolder(), aliases);
    }

    public void register() {
        if (! isEnabled()) return;

        CommandHandler.registerStreamlineCommand(this);
    }

    public void unregister() {
        if (! isEnabled()) if (! CommandHandler.isStreamlineCommandRegistered(getIdentifier())) return;

        CommandHandler.unregisterStreamlineCommand(this);
    }

    public CommandResult<?> baseRun(StreamSender sender, @Nullable String[] args) {
        if (args == null) args = new String[] { "" };
        if (args.length < 1) args = new String[] { "" };

        CommandContext<StreamlineCommand> context = new CommandContext<>(sender, this, notSet(), args);

        CommandResult<?> result = resultedRun(context);
        if (result != null) {
            context.setResult(result);
        }

        CommandResultedEvent<StreamlineCommand> event = new CommandResultedEvent<>(context);
        event.fire();

        return context.getResult();
    }

    @Deprecated
    public void run(StreamSender sender, @Nullable String[] args) {
        // Nothing.
    }

    @Deprecated
    public void run(CommandContext<StreamlineCommand> context) {
        run(context.getSender(), context.getArgs().stream().map(CommandArgument::getContent).toArray(String[]::new));
    }

    public CommandResult<?> resultedRun(CommandContext<StreamlineCommand> context) {
        run(context);
        return null;
    }

    public ConcurrentSkipListSet<String> baseTabComplete(StreamSender sender, @Nullable String[] args) {
        if (args == null) args = new String[] { "" };
        if (args.length < 1) args = new String[] { "" };
        return doTabComplete(new CommandContext<>(sender, this, notSet(), args));
    }

    @Deprecated
    public ConcurrentSkipListSet<String> doTabComplete(StreamSender sender, @Nullable String[] args) {
        return new ConcurrentSkipListSet<>();
    }

    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<StreamlineCommand> context) {
        return doTabComplete(context.getSender(), context.getArgs().stream().map(CommandArgument::getContent).toArray(String[]::new));
    }

    public String getWithOther(StreamSender sender, String base, StreamSender other) {
        base = base.replace("%this_other%", other.getCurrentName());
        base = base.replace("%this_other_uuid%", other.getUuid());
        return MessageUtils.replaceAllPlayerBungee(other, getWithOther(sender, base, other.getCurrentName()));
    }

    public String getWithOther(StreamSender sender, String base, String other) {
        base = base.replace("%this_sender%", sender.getCurrentName());
        base = base.replace("%this_sender_uuid%", sender.getUuid());
        return MessageUtils.replaceAllPlayerBungee(sender, getWithOther(base, other));
    }

    public String getWithOther(String base, String other) {
        return base.replace("%this_other%", other);
    }

    public boolean isLoaded() {
        return CommandHandler.isStreamlineCommandRegistered(getIdentifier());
    }

    public boolean isEnabled() {
        return getCommandResource().getResource().getBoolean("basic.enabled");
    }

    public void disable() {
        CommandHandler.unregisterStreamlineCommand(this);
    }

    @Override
    public int compareTo(@NotNull StreamlineCommand o) {
        return CharSequence.compare(getIdentifier(), o.getIdentifier());
    }

    public ConcurrentSkipListSet<String> getIntegerArgument() {
        return getIntegerArgument(-5, 5);
    }

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

    public ConcurrentSkipListSet<String> getDoubleArgument() {
        return getDoubleArgument(-3.0, 3.0, 0.25);
    }

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

    public static <T> CommandResult<T> result(String key, T result) {
        return new CommandResult<>(key, result);
    }

    public static CommandResult.Success success() {
        return CommandResult.Success.get();
    }

    public static CommandResult.Failure failure() {
        return CommandResult.Failure.get();
    }

    public static CommandResult.Error error() {
        return CommandResult.Error.get();
    }

    public static CommandResult.NotSet notSet() {
        return CommandResult.NotSet.get();
    }
}
