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

@Getter
public class ProperCommand implements SimpleCommand, IProperCommand {
    private final CosmicCommand parent;
    private final String base;
    private final String permission;
    private final String[] aliases;

    public ProperCommand(CosmicCommand parent) {
        this.parent = parent;
        this.base = parent.getBase();
        this.permission = parent.getPermission();
        this.aliases = parent.getAliases();
    }

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

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(permission);
    }

    @Override
    public void registerThis() {
        StreamlineVelocity.getInstance().getProxy().getCommandManager().register(getMeta(), this);
    }

    @Override
    public void unregisterThis() {
        StreamlineVelocity.getInstance().getProxy().getCommandManager().unregister(getMeta());
    }

    public CommandMeta getMeta() {
        return StreamlineVelocity.getInstance().getProxy().getCommandManager().metaBuilder(this.getParent().getBase())
                .plugin(StreamlineVelocity.getInstance())
                .aliases(this.getParent().getAliases())
                .build();
    }
}
