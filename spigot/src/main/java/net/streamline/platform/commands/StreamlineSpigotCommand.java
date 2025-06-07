package net.streamline.platform.commands;

import host.plas.bou.commands.CommandContext;
import host.plas.bou.commands.SimplifiedCommand;
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
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class StreamlineSpigotCommand extends SimplifiedCommand {
    public StreamlineSpigotCommand() {
        super("streamlinespigot", Streamline.getInstance());
    }

    @Override
    public boolean command(CommandContext ctx) {
        if (ctx.getArgCount() < 1) {
            ctx.sendMessage("&cCommand not found.");
            return false;
        }

        String alias = ctx.getStringArg(0);

        CosmicCommand streamlineCommand = CommandHandler.getCommandByAlias(alias);
        if (streamlineCommand == null) {
            ctx.sendMessage("&cCommand not found.");
            return false;
        }

        String[] newArgs = StringUtils.argsMinus(ctx.getArgsAsStringArray(), 0);

        CosmicSender s = UserManager.getInstance().getOrCreateSender(ctx.getCommandSender());
        if (s == null) {
            ctx.sendMessage("&cCould not find your user...");
            return true;
        }

        streamlineCommand.baseRun(s, newArgs);
        return true;
    }

    @Nullable
    @Override
    public ConcurrentSkipListSet<String> tabComplete(CommandContext ctx) {
        if (ctx.getArgCount() <= 1) {
            return CommandHandler.getAllAliases();
        }

        String alias = ctx.getStringArg(0);

        CosmicCommand streamlineCommand = CommandHandler.getCommandByAlias(alias);
        if (streamlineCommand == null) return null;

        String[] newArgs = StringUtils.argsMinus(ctx.getArgsAsStringArray(), 0);

        CosmicSender s = UserManager.getInstance().getOrCreateSender(ctx.getCommandSender());
        if (s == null) return null;

        return streamlineCommand.baseTabComplete(s, newArgs);
    }
}
