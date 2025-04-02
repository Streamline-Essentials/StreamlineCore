package net.streamline.platform.commands;

import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import singularity.command.CommandHandler;
import singularity.command.CosmicCommand;
import singularity.data.console.CosmicSender;
import tv.quaint.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StreamlineSpigotCommand implements TabExecutor {
    public StreamlineSpigotCommand() {
        try {
            Objects.requireNonNull(Streamline.getInstance().getCommand("streamlinespigot")).setExecutor(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args == null) args = new String[] { "" };
        String alias = args[0];

        CosmicCommand streamlineCommand = CommandHandler.getCommandByAlias(alias);
        if (streamlineCommand == null) {
            sender.sendMessage(ChatColor.RED + "Command not found.");
            return true;
        }

        String[] newArgs = StringUtils.argsMinus(args, 0);

        CosmicSender s = UserManager.getInstance().getOrCreateSender(sender);
        if (s == null) {
            sender.sendMessage(ChatColor.RED + "Could not find your user...");
            return true;
        }

        streamlineCommand.baseRun(s, newArgs);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return StringUtils.getAsCompletionList("", CommandHandler.getAllAliases());
        }
        if (args.length == 1) {
            return StringUtils.getAsCompletionList(args[0], CommandHandler.getAllAliases());
        }

        String alias = args[0];

        CosmicCommand streamlineCommand = CommandHandler.getCommandByAlias(alias);
        if (streamlineCommand == null) return null;

        String[] newArgs = StringUtils.argsMinus(args, 0);

        CosmicSender s = UserManager.getInstance().getOrCreateSender(sender);
        if (s == null) return null;

        return new ArrayList<>(streamlineCommand.baseTabComplete(s, newArgs));
    }
}
