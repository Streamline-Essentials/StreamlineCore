package net.streamline.platform.commands;

import net.streamline.api.command.CommandHandler;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.users.StreamPlayer;
import net.streamline.base.Streamline;
import net.streamline.platform.Messenger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tv.quaint.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StreamlineSpigotCommand implements TabExecutor {
    public StreamlineSpigotCommand() {
        try {
            Objects.requireNonNull(Streamline.getInstance().getCommand("streamlinespigot")).setExecutor(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args == null) args = new String[] { "" };
        String commandName = args[0];

        StreamlineCommand streamlineCommand = CommandHandler.getStreamlineCommand(commandName);
        if (streamlineCommand == null) {
            sender.sendMessage(Messenger.getInstance().codedText("&cCommand not found!"));
            return true;
        }

        String[] newArgs = StringUtils.argsMinus(args, 0);

        StreamPlayer senderUser = ModuleUtils.getOrGetUserByName(sender.getName());
        if (senderUser == null) {
            sender.sendMessage(Messenger.getInstance().codedText("&cCould not find your user..."));
            return true;
        }

        streamlineCommand.baseRun(senderUser, newArgs);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return StringUtils.getAsCompletionList("",
                    CommandHandler.getLoadedStreamlineCommands().values().stream().map(StreamlineCommand::getBase).collect(Collectors.toList()));
        }
        if (args.length == 1) {
            return StringUtils.getAsCompletionList(args[0],
                    CommandHandler.getLoadedStreamlineCommands().values().stream().map(StreamlineCommand::getBase).collect(Collectors.toList()));
        }

        String commandName = args[0];

        StreamlineCommand streamlineCommand = CommandHandler.getStreamlineCommand(commandName);
        if (streamlineCommand == null) return null;

        String[] newArgs = StringUtils.argsMinus(args, 0);

        StreamPlayer senderUser = ModuleUtils.getOrGetUserByName(sender.getName());
        if (senderUser == null) return null;

        return new ArrayList<>(streamlineCommand.baseTabComplete(senderUser, newArgs));
    }
}
