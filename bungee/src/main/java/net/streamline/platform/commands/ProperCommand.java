package net.streamline.platform.commands;

import lombok.Getter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.streamline.base.StreamlineBungee;
import singularity.command.CosmicCommand;
import singularity.data.console.CosmicSender;
import singularity.interfaces.IProperCommand;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.NotNull;
import singularity.utils.MessageUtils;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class ProperCommand extends Command implements TabExecutor, IProperCommand {
    private final CosmicCommand parent;

    public ProperCommand(CosmicCommand parent) {
        super(parent.getBase(), parent.getPermission(), parent.getAliases());
        this.parent = parent;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        CosmicSender s = UserManager.getInstance().getOrCreateSender(sender).orElse(null);
        if (s == null) {
            MessageUtils.logWarning("Command execution failed: Sender is not a CosmicSender.");
            return;
        }

        parent.baseRun(s, args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args == null) args = new String[] { "" };
        if (args.length < 1) args = new String[] { "" };

        CosmicSender s = UserManager.getInstance().getOrCreateSender(sender).orElse(null);
        if (s == null) {
            MessageUtils.logWarning("Tab completion failed: Sender is not a CosmicSender.");
            return new ArrayList<>();
        }

        ConcurrentSkipListSet<String> r = parent.baseTabComplete(s, args);

        return r == null ? new ArrayList<>() : MessageUtils.getCompletion(r, args[args.length - 1]);
    }

    public void register() {
        StreamlineBungee.getInstance().getProxy().getPluginManager().registerCommand(StreamlineBungee.getInstance(), this);
    }

    public void unregister() {
        StreamlineBungee.getInstance().getProxy().getPluginManager().unregisterCommand(this);
    }
}
