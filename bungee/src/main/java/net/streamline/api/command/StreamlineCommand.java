package net.streamline.api.command;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.streamline.api.configs.CommandResource;
import net.streamline.api.modules.BundledModule;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.base.Streamline;
import net.streamline.utils.MessagingUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class StreamlineCommand extends Command implements TabExecutor {
    @Getter @Setter
    private String base;
    @Getter @Setter
    private String permission;
    @Getter @Setter
    private String[] aliases;
    @Getter
    private final CommandResource commandResource;

    public StreamlineCommand(String base, String permission, File parentDirectory, String... aliases) {
        super(base, permission, aliases);
        this.base = base;
        this.permission = permission;
        this.aliases = aliases;
        this.commandResource = new CommandResource(this, parentDirectory);
        Streamline.registerStreamlineCommand(this);
    }

    public StreamlineCommand(BundledModule module, String base, String permission, String... aliases) {
        this(base, permission, new File(module.getDataFolder(), Streamline.getCommandsFolderChild()), aliases);
    }

    public StreamlineCommand(String base, String permission, String... aliases) {
        this(base, permission, Streamline.getMainCommandsFolder(), aliases);
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        run(UserManager.getOrGetUser(sender), args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args == null) return new ArrayList<>();
        if (args.length <= 0) return new ArrayList<>();

        List<String> r = doTabComplete(UserManager.getOrGetUser(sender), args);

        return r == null ? new ArrayList<>() : MessagingUtils.getCompletion(r, args[args.length - 1]).stream().toList();
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
        for (Map.Entry<String, Command> entry : Streamline.getInstance().getProxy().getPluginManager().getCommands()) {
            if (entry.getKey().equals(this.base)) return true;
        }

        return false;
    }
}
