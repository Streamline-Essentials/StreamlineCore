package net.streamline.platform.commands;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import lombok.Getter;
import net.streamline.base.StreamlineVelocity;
import net.streamline.platform.Messenger;
import net.streamline.platform.savables.UserManager;
import singularity.command.CosmicCommand;
import singularity.data.console.CosmicSender;
import singularity.interfaces.IProperCommand;
import singularity.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Velocity adapter that bridges a cross-platform {@link CosmicCommand} to the Velocity
 * {@link SimpleCommand} API.
 *
 * <p>Handles execution, async tab-completion, and permission checks by delegating to the
 * wrapped {@link CosmicCommand}. Command registration and unregistration are performed
 * through the Velocity {@link com.velocitypowered.api.command.CommandManager}.
 */
@Getter
public class ProperCommand implements SimpleCommand, IProperCommand {
    /**
     * The underlying cross-platform command definition.
     */
    private final CosmicCommand parent;

    /**
     * The primary command label (the first alias / base name).
     */
    private final String base;

    /**
     * The permission node required to execute this command.
     */
    private final String permission;

    /**
     * Additional aliases under which this command can be invoked.
     */
    private final String[] aliases;

    /**
     * Constructs a new {@code ProperCommand} wrapping the given {@link CosmicCommand}.
     *
     * @param parent the cross-platform command to wrap
     */
    public ProperCommand(CosmicCommand parent) {
        this.parent = parent;
        this.base = parent.getBase();
        this.permission = parent.getPermission();
        this.aliases = parent.getAliases();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the executing {@link singularity.data.console.CosmicSender} from the
     * invocation source and delegates to {@link CosmicCommand#baseRun(singularity.data.console.CosmicSender, String[])}. On any error
     * a user-visible message is sent and the stack trace is printed.
     *
     * @param invocation the command invocation context
     */
    @Override
    public void execute(Invocation invocation) {
        try {
            CosmicSender s = UserManager.getInstance().getOrCreateSender(invocation.source()).orElse(null);
            if (s == null) {
                MessageUtils.logWarning("Command execution failed: Sender is null.");
                return;
            }

            parent.baseRun(s, invocation.arguments());
        } catch (Throwable e) {
            Messenger.getInstance().sendMessage(invocation.source(), "&cAn error occurred while executing the command &7'&e" + invocation.alias() + "&7'&8. &cPlease tell an admin to check the console.");
            MessageUtils.logWarning("An error occurred while executing command '" + base + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link CosmicCommand#baseTabComplete} and filters the completions
     * against the last typed argument. Returns an empty list on any error.
     *
     * @param invocation the command invocation context
     * @return a {@link CompletableFuture} resolving to the list of tab-complete suggestions
     */
    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        try {
            String[] args = invocation.arguments();
            if (args.length < 1) args = new String[]{""};
            CosmicSender s = UserManager.getInstance().getOrCreateSender(invocation.source()).orElse(null);
            if (s == null) {
                MessageUtils.logWarning("Command suggestion failed: Sender is null.");
                return CompletableFuture.completedFuture(new ArrayList<>());
            }

            ConcurrentSkipListSet<String> r = parent.baseTabComplete(s, invocation.arguments());

            return CompletableFuture.completedFuture(r == null ? new ArrayList<>() : new ArrayList<>(MessageUtils.getCompletion(r, args[args.length - 1])));
        } catch (Throwable e) {
            MessageUtils.logWarning("An error occurred while suggesting command '" + base + "': " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether the invocation source has the {@link #permission} node.
     *
     * @param invocation the command invocation context
     * @return {@code true} if the source holds the required permission
     */
    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(permission);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Registers this command with the Velocity command manager using the meta
     * returned by {@link #getMeta()}.
     */
    @Override
    public void registerThis() {
        StreamlineVelocity.getInstance().getProxy().getCommandManager().register(getMeta(), this);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Unregisters this command from the Velocity command manager using the meta
     * returned by {@link #getMeta()}.
     */
    @Override
    public void unregisterThis() {
        StreamlineVelocity.getInstance().getProxy().getCommandManager().unregister(getMeta());
    }

    /**
     * Builds the Velocity {@link CommandMeta} for this command, including the base label,
     * aliases, and plugin owner association.
     *
     * @return the constructed {@link CommandMeta}
     */
    public CommandMeta getMeta() {
        return StreamlineVelocity.getInstance().getProxy().getCommandManager().metaBuilder(this.getParent().getBase())
                .plugin(StreamlineVelocity.getInstance())
                .aliases(this.getParent().getAliases())
                .build();
    }
}
