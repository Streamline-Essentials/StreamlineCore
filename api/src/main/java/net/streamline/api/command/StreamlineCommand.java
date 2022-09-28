package net.streamline.api.command;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.CommandResource;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UserUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

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

    abstract public void run(StreamlineUser sender, String[] args);

    abstract public List<String> doTabComplete(StreamlineUser sender, String[] args);

    public String getWithOther(StreamlineUser sender, String base, StreamlineUser other) {
        base = base.replace("%this_other%", other.getName());
        base = base.replace("%this_other_uuid%", other.getUuid());
        return SLAPI.getInstance().getMessenger().replaceAllPlayerBungee(other, getWithOther(sender, base, other.getLatestName()));
    }

    public String getWithOther(StreamlineUser sender, String base, String other) {
        base = base.replace("%this_sender%", sender.getName());
        base = base.replace("%this_sender_uuid%", sender.getName());
        StreamlineUser user = UserUtils.getOrGetUser(sender.getUuid());
        return SLAPI.getInstance().getMessenger().replaceAllPlayerBungee(user, getWithOther(base, other));
    }

    public String getWithOther(String base, String other) {
        return base.replace("%this_other%", other);
    }

    public boolean isLoaded() {
        return CommandHandler.isStreamlineCommandRegistered(getIdentifier());
    }

    public boolean isEnabled() {
        return getCommandResource().resource.getBoolean("basic.enabled");
    }

    public void disable() {
        CommandHandler.unregisterStreamlineCommand(this);
    }

    @Override
    public int compareTo(@NotNull StreamlineCommand o) {
        return CharSequence.compare(getIdentifier(), o.getIdentifier());
    }
}
