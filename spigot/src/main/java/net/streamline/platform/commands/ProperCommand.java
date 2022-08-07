package net.streamline.platform.commands;

import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.base.Streamline;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProperCommand implements CommandExecutor, TabExecutor {
    @Getter
    private final StreamlineCommand parent;

    public ProperCommand(StreamlineCommand parent) {
        this.parent = parent;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        parent.run(SLAPI.getInstance().getUserManager().getOrGetUserByName(sender.getName()), args);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args == null) return new ArrayList<>();
        if (args.length <= 0) return new ArrayList<>();

        List<String> r = parent.doTabComplete(SLAPI.getInstance().getUserManager().getOrGetUserByName(sender.getName()), args);

        return r == null ? new ArrayList<>() : SLAPI.getInstance().getMessenger().getCompletion(r, args[args.length - 1]).stream().toList();
    }

    public void register() {
        Streamline.getInstance().getConfig().set("commands." + getParent().getBase() + ".permission", getParent().getPermission());
        PluginCommand command = Streamline.getInstance().getCommand(getParent().getBase());
        if (command == null) {
            SLAPI.getInstance().getMessenger().logWarning("Unable to register command with parent base '" + getParent().getBase() + "' because the command getter returned null!");
            return;
        }
        command.setExecutor(this);
        command.setAliases(Arrays.stream(getParent().getAliases()).toList());
        command.setPermission(getParent().getPermission());
    }

    public void unregister() {
        Streamline.getInstance().getConfig().set("commands." + getParent().getBase(), null);
    }
}
