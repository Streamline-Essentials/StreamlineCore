package net.streamline.platform.commands;

import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.interfaces.IProperCommand;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class ProperCommand extends BukkitCommand implements TabExecutor, IProperCommand {
    @Getter
    private final StreamlineCommand parent;

    public ProperCommand(StreamlineCommand parent) {
        super(parent.getBase(), "Not defined.", "Not defined.", List.of(parent.getAliases()));
        this.parent = parent;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        parent.baseRun(UserUtils.getOrGetUserByName(sender.getName()), args);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args == null) args = new String[] { "" };
        if (args.length < 1) args = new String[] { "" };
        ConcurrentSkipListSet<String> r = parent.baseTabComplete(UserUtils.getOrGetUserByName(sender.getName()), args);

        return r == null ? new ArrayList<>() : MessageUtils.getCompletion(r, args[args.length - 1]).stream().toList();
    }

    public void register() {
        try {
            getCommandMap().register(getParent().getBase(), getParent().getLabel(), this);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private CommandMap getCommandMap() throws Exception {
        Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        field.setAccessible(true);
        return (CommandMap) field.get(Bukkit.getServer());
    }

    public void unregister() {
        try {
            unregister(getCommandMap());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        parent.baseRun(UserUtils.getOrGetUserByName(sender.getName()), args);
        return true;
    }
}
