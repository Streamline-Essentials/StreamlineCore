package net.streamline.platform.commands;

import gg.drak.thebase.utils.StringUtils;
import host.plas.bou.commands.CommandContext;
import host.plas.bou.commands.SimplifiedCommand;
import net.streamline.base.StreamlineSpigot;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.Nullable;
import singularity.command.CommandHandler;
import singularity.command.CosmicCommand;
import singularity.data.console.CosmicSender;

import java.util.concurrent.ConcurrentSkipListSet;

public class StreamlineSpigotCommand extends SimplifiedCommand {
    public StreamlineSpigotCommand() {
        super("streamlinespigot", StreamlineSpigot.getInstance());
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
