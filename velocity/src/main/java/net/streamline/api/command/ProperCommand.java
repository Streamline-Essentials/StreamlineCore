package net.streamline.api.command;

import com.velocitypowered.api.command.SimpleCommand;
import lombok.Getter;
import net.streamline.api.savables.UserManager;

import java.util.ArrayList;
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
        parent.run(UserManager.getOrGetUser(invocation.source()), invocation.arguments());
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(new ArrayList<>(parent.doTabComplete(UserManager.getOrGetUser(invocation.source()), invocation.arguments())));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(permission);
    }
}
