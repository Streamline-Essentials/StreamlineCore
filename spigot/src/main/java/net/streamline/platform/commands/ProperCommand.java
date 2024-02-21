package net.streamline.platform.commands;

import lombok.Getter;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.command.result.CommandResult;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.interfaces.IProperCommand;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class ProperCommand extends BukkitCommand implements TabExecutor, IProperCommand {
    private final StreamlineCommand parent;

    public ProperCommand(StreamlineCommand parent) {
        super(parent.getBase(), "Not defined.", "Not defined.", List.of(parent.getAliases()));
        this.parent = parent;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return execute(sender, label, args);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args == null) args = new String[] { "" };
        if (args.length < 1) args = new String[] { "" };

        Optional<StreamPlayer> player = UserManager.getInstance().getOrGetPlayer(sender);
        if (player.isEmpty()) return new ArrayList<>();
        StreamPlayer p = player.get();

        ConcurrentSkipListSet<String> r = parent.baseTabComplete(p, args);

        return r == null ? new ArrayList<>() : new ArrayList<>(MessageUtils.getCompletion(r, args[args.length - 1]));
    }

    public void register() {
        try {
            Streamline.registerCommands(this);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void unregister() {
        try {
            Streamline.unregisterCommands(getParent().getBase());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        Optional<StreamPlayer> player = UserManager.getInstance().getOrGetPlayer(sender);
        if (player.isEmpty()) return false;
        StreamPlayer p = player.get();

        CommandResult<?> result = parent.baseRun(p, args);

        if (result == null) return false;
        if (result == StreamlineCommand.notSet()) return true;
        if (result == StreamlineCommand.error()) return false;
        if (result == StreamlineCommand.failure()) return false;
        return result == StreamlineCommand.success();
    }
}
