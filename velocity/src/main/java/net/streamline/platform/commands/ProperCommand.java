package net.streamline.platform.commands;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import lombok.Getter;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.interfaces.IProperCommand;
import net.streamline.api.utils.MessageUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class ProperCommand implements SimpleCommand, IProperCommand {
    private final StreamlineCommand parent;
    private final String base;
    private final String permission;
    private final String[] aliases;

    public ProperCommand(StreamlineCommand parent) {
        this.parent = parent;
        this.base = parent.getBase();
        this.permission = parent.getPermission();
        this.aliases = parent.getAliases();
    }

    @Override
    public void execute(Invocation invocation) {
        parent.baseRun(UserManager.getInstance().getOrGetUser(invocation.source()), invocation.arguments());
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length < 1) args = new String[] { "" };
        ConcurrentSkipListSet<String> r = parent.baseTabComplete(UserManager.getInstance().getOrGetUser(invocation.source()), invocation.arguments());

        return CompletableFuture.completedFuture(r == null ? new ArrayList<>() : new ArrayList<>(MessageUtils.getCompletion(r, args[args.length - 1])));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(permission);
    }

    public void register() {
        Streamline.getInstance().getProxy().getCommandManager().register(getMeta(), this);
    }

    public void unregister() {
        Streamline.getInstance().getProxy().getCommandManager().unregister(getMeta());
    }

    public CommandMeta getMeta() {
        return Streamline.getInstance().getProxy().getCommandManager().metaBuilder(this.getParent().getBase())
                .plugin(Streamline.getInstance())
                .aliases(this.getParent().getAliases())
                .build();
    }
}
