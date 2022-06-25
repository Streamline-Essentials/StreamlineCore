package net.streamline.api.command;

import net.streamline.api.configs.CommandResource;
import net.streamline.base.Streamline;
import net.streamline.utils.MessagingUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class StreamlineCommand extends Command implements TabExecutor {
    public String base;
    public String permission;
    public String[] aliases;
    public CommandResource commandResource;

    public StreamlineCommand(String base, String description, String usageMessage, String permission, File parentDirectory, String... aliases) {
        super(base, description, usageMessage, List.of(aliases));
        this.base = base;
        this.permission = permission;
        this.aliases = aliases;
        this.commandResource = new CommandResource(this, parentDirectory);
        Streamline.registerCommand(this);
    }

    @Override
    public boolean execute(@NotNull ICommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        run(sender, args);
        return true;
    }

    @Override
    public boolean onCommand(@NotNull ICommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Command other = Streamline.getInstance().getModuleManager().getCommandMap().getCommand(command.getName());
        if (other == null) return false;
        return other.execute(sender, label, args);
    }

    @Override
    public List<String> onTabComplete(ICommandSender sender, Command command, String alias, String[] args) {
        if (args == null) return new ArrayList<>();
        if (args.length <= 0) return new ArrayList<>();

        List<String> r = doTabComplete(sender, args);

        return r == null ? new ArrayList<>() : MessagingUtils.getCompletion(r, alias).stream().toList();
    }

    abstract public void run(ICommandSender sender, String[] args);

    abstract public List<String> doTabComplete(ICommandSender sender, String[] args);
}
