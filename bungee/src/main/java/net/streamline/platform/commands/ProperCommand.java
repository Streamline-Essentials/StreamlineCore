package net.streamline.platform.commands;

import lombok.Getter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.streamline.base.StreamlineBungee;
import net.streamline.platform.Messenger;
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
        try {
            CosmicSender s = UserManager.getInstance().getOrCreateSender(sender).orElse(null);
            if (s == null) {
                MessageUtils.logWarning("Command execution failed: Sender is not a CosmicSender.");
                return;
            }

            parent.baseRun(s, args);
        } catch (Throwable e) {
            Messenger.getInstance().sendMessage(sender, "&cAn error occurred while executing the command &7'&e" + getName() + "&7'&8. &cPlease tell an admin to check the console.");

            MessageUtils.logWarning("An error occurred while executing command '" + parent.getBase() + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        try {
            if (args == null) args = new String[]{""};
            if (args.length < 1) args = new String[]{""};

            CosmicSender s = UserManager.getInstance().getOrCreateSender(sender).orElse(null);
            if (s == null) {
                MessageUtils.logWarning("Tab completion failed: Sender is not a CosmicSender.");
                return new ArrayList<>();
            }

            ConcurrentSkipListSet<String> r = parent.baseTabComplete(s, args);

            return r == null ? new ArrayList<>() : MessageUtils.getCompletion(r, args[args.length - 1]);
        } catch (Throwable e) {
            MessageUtils.logWarning("An error occurred while tab completing command '" + parent.getBase() + "': " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public void registerThis() {
        StreamlineBungee.getInstance().getProxy().getPluginManager().registerCommand(StreamlineBungee.getInstance(), this);
    }

    @Override
    public void unregisterThis() {
        StreamlineBungee.getInstance().getProxy().getPluginManager().unregisterCommand(this);
    }
}
