package net.streamline.api.command;

import lombok.Getter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.streamline.api.savables.UserManager;
import net.streamline.utils.MessagingUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ProperCommand extends Command implements TabExecutor {
    @Getter
    private final StreamlineCommand parent;

    public ProperCommand(StreamlineCommand parent) {
        super(parent.getBase(), parent.getPermission(), parent.getAliases());
        this.parent = parent;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        parent.run(UserManager.getOrGetUser(sender), args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args == null) return new ArrayList<>();
        if (args.length <= 0) return new ArrayList<>();

        List<String> r = parent.doTabComplete(UserManager.getOrGetUser(sender), args);

        return r == null ? new ArrayList<>() : MessagingUtils.getCompletion(r, args[args.length - 1]).stream().toList();
    }
}
