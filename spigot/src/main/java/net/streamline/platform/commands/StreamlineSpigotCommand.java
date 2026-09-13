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

/**
 * Root Streamline command registered on the Spigot server under the
 * {@code streamlinespigot} label.
 *
 * <p>Acts as a catch-all dispatcher: the first argument is matched against
 * all registered {@link singularity.command.CosmicCommand} aliases, and
 * the remaining arguments are forwarded to that command's execution or
 * tab-completion logic.
 */
public class StreamlineSpigotCommand extends SimplifiedCommand {
    /**
     * Constructs and registers the {@code streamlinespigot} command on the
     * Spigot server.
     */
    public StreamlineSpigotCommand() {
        super("streamlinespigot", StreamlineSpigot.getInstance());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the first argument as an alias, resolves the matching
     * {@link singularity.command.CosmicCommand}, strips that first argument from
     * the array, and runs the command for the resolved
     * {@link singularity.data.console.CosmicSender}.
     *
     * @param ctx the command invocation context
     * @return {@code true} if the command was dispatched; {@code false} if no
     *         matching command or sender was found
     */
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

        CosmicSender s = UserManager.getInstance().getOrCreateSender(ctx.getCommandSender()).orElse(null);
        if (s == null) {
            ctx.sendMessage("&cCould not find your user...");
            return true;
        }

        streamlineCommand.baseRun(s, newArgs);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>When only one argument has been typed, returns all registered command
     * aliases. Otherwise, resolves the first argument to a
     * {@link singularity.command.CosmicCommand} and delegates tab-completion for
     * the remaining arguments.
     *
     * @param ctx the tab-completion context
     * @return the set of applicable completions, or {@code null} to fall back to
     *         the default Bukkit player-name completions
     */
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

        CosmicSender s = UserManager.getInstance().getOrCreateSender(ctx.getCommandSender()).orElse(null);
        if (s == null) return null;

        return streamlineCommand.baseTabComplete(s, newArgs);
    }
}
