package net.streamline.platform.commands;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProperCommand implements SimpleCommand {
    @Getter
    private final StreamlineCommand parent;
    @Getter
    private String base;
    @Getter
    private String permission;
    @Getter
    private String[] aliases;

    public ProperCommand(StreamlineCommand parent) {
        this.parent = parent;
        this.base = parent.getBase();
        this.permission = parent.getPermission();
        this.aliases = parent.getAliases();
    }

    @Override
    public void execute(Invocation invocation) {
        parent.run(UserManager.getInstance().getOrGetUser(invocation.source()), invocation.arguments());
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(new ArrayList<>(parent.doTabComplete(UserManager.getInstance().getOrGetUser(invocation.source()), invocation.arguments())));
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
