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

/**
 * BungeeCord-specific adapter that bridges a cross-platform {@link CosmicCommand}
 * to the BungeeCord {@link Command} and {@link TabExecutor} API.
 *
 * <p>Resolves the executing {@link singularity.data.console.CosmicSender} from
 * the BungeeCord {@link CommandSender} and delegates execution and tab-completion
 * to the underlying {@link CosmicCommand}.
 */
@Getter
public class ProperCommand extends Command implements TabExecutor, IProperCommand {

    /** The cross-platform command definition this adapter wraps. */
    private final CosmicCommand parent;

    /**
     * Constructs a new {@code ProperCommand} wrapping the given {@link CosmicCommand}.
     * The BungeeCord command is registered with the base name, permission, and
     * aliases taken from {@code parent}.
     *
     * @param parent the cross-platform command definition to wrap
     */
    public ProperCommand(CosmicCommand parent) {
        super(parent.getBase(), parent.getPermission(), parent.getAliases());
        this.parent = parent;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the {@link singularity.data.console.CosmicSender} from the
     * BungeeCord sender and delegates to {@link CosmicCommand#baseRun}. Sends
     * an error message to the sender and logs to the console if an unexpected
     * exception is thrown.
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the sender, delegates to {@link CosmicCommand#baseTabComplete},
     * and filters the results to match the last argument token.
     * Returns an empty list on any error.
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Registers this command with the BungeeCord proxy plugin manager.
     */
    @Override
    public void registerThis() {
        StreamlineBungee.getInstance().getProxy().getPluginManager().registerCommand(StreamlineBungee.getInstance(), this);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Unregisters this command from the BungeeCord proxy plugin manager.
     */
    @Override
    public void unregisterThis() {
        StreamlineBungee.getInstance().getProxy().getPluginManager().unregisterCommand(this);
    }
}
