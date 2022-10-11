package net.streamline.platform.commands;

import lombok.Getter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.interfaces.IProperCommand;
import net.streamline.api.utils.MessageUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.Messenger;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class ProperCommand extends Command implements TabExecutor, IProperCommand {
    @Getter
    private final StreamlineCommand parent;

    public ProperCommand(StreamlineCommand parent) {
        super(parent.getBase(), parent.getPermission(), parent.getAliases());
        this.parent = parent;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        parent.run(UserManager.getInstance().getOrGetUser(sender), args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args == null) return new ArrayList<>();
        if (args.length <= 0) return new ArrayList<>();

        ConcurrentSkipListSet<String> r = parent.doTabComplete(UserManager.getInstance().getOrGetUser(sender), args);

        return r == null ? new ArrayList<>() : MessageUtils.getCompletion(r, args[args.length - 1]);
    }

    public void register() {
        Streamline.getInstance().getProxy().getPluginManager().registerCommand(Streamline.getInstance(), this);
    }

    public void unregister() {
        Streamline.getInstance().getProxy().getPluginManager().unregisterCommand(this);
    }
}
