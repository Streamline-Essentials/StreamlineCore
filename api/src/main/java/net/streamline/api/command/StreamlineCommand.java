package net.streamline.api.command;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.CommandResource;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class StreamlineCommand implements Comparable<StreamlineCommand> {
    @Getter @Setter
    private String identifier;
    @Getter @Setter
    private String base;
    @Getter @Setter
    private String permission;
    @Getter @Setter
    private String[] aliases;
    @Getter
    private final CommandResource commandResource;

    public StreamlineCommand(String base, String permission, File parentDirectory, String... aliases) {
        this.identifier = base;
        this.base = base;
        this.permission = permission;
        this.aliases = aliases;
        this.commandResource = new CommandResource(this, parentDirectory);
    }

    public StreamlineCommand(String base, String permission, String... aliases) {
        this(base, permission, SLAPI.getMainCommandsFolder(), aliases);
    }

    public void register() {
        if (! isEnabled()) return;

        CommandHandler.registerStreamlineCommand(this);
    }

    public void unregister() {
        if (! isEnabled()) if (! CommandHandler.isStreamlineCommandRegistered(getIdentifier())) return;

        CommandHandler.unregisterStreamlineCommand(this);
    }

    public void baseRun(StreamlineUser sender, @Nullable String[] args) {
        if (args == null) args = new String[] { "" };
        if (args.length < 1) args = new String[] { "" };
        run(sender, args);
    }

    abstract public void run(StreamlineUser sender, String[] args);

    public ConcurrentSkipListSet<String> baseTabComplete(StreamlineUser sender, @Nullable String[] args) {
        if (args == null) args = new String[] { "" };
        if (args.length < 1) args = new String[] { "" };
        return doTabComplete(sender, args);
    }

    abstract public ConcurrentSkipListSet<String> doTabComplete(StreamlineUser sender, String[] args);

    public String getWithOther(StreamlineUser sender, String base, StreamlineUser other) {
        base = base.replace("%this_other%", other.getName());
        base = base.replace("%this_other_uuid%", other.getUuid());
        return MessageUtils.replaceAllPlayerBungee(other, getWithOther(sender, base, other.getLatestName()));
    }

    public String getWithOther(StreamlineUser sender, String base, String other) {
        base = base.replace("%this_sender%", sender.getName());
        base = base.replace("%this_sender_uuid%", sender.getName());
        StreamlineUser user = UserUtils.getOrGetUser(sender.getUuid());
        return MessageUtils.replaceAllPlayerBungee(user, getWithOther(base, other));
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
}
