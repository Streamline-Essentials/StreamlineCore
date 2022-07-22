package net.streamline.api.command;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.configs.CommandResource;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.base.Streamline;
import net.streamline.utils.MessagingUtils;

import java.io.File;
import java.util.List;

public abstract class StreamlineCommand {
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
        this(base, permission, Streamline.getMainCommandsFolder(), aliases);
    }

    public void register() {
        Streamline.registerStreamlineCommand(this);
    }

    public void unregister() {
        Streamline.unregisterStreamlineCommand(this);
    }

    abstract public void run(SavableUser sender, String[] args);

    abstract public List<String> doTabComplete(SavableUser sender, String[] args);

    public String getWithOther(SavableUser sender, String base, SavableUser other) {
        return MessagingUtils.replaceAllPlayerBungee(other, getWithOther(sender, base, other.latestName));
    }

    public String getWithOther(SavableUser sender, String base, String other) {
        SavableUser user = UserManager.getOrGetUser(sender.uuid);
        return MessagingUtils.replaceAllPlayerBungee(user, getWithOther(base, other));
    }

    public String getWithOther(String base, String other) {
        return base.replace("%this_other%", other);
    }

    public boolean isEnabled() {
        for (String identifier : Streamline.getProperlyRegisteredCommands().keySet()) {
            if (identifier.equals(this.getIdentifier())) return true;
        }

        return false;
    }

    public void disable() {
        Streamline.unregisterStreamlineCommand(this);
    }
}
