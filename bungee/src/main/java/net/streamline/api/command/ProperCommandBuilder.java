package net.streamline.api.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.streamline.api.entities.ConsoleCommandSender;
import net.streamline.api.entities.Player;
import net.streamline.base.Streamline;

public class ProperCommandBuilder extends Command implements TabExecutor {
    StreamlineCommand command;

    public ProperCommandBuilder(StreamlineCommand from) {
        super(from.getName(), from.getPermission(), from.getAliases().toArray(new String[0]));
        this.command = from;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ICommandSender s;
        if (sender instanceof ProxiedPlayer player) {
            s = new Player(Streamline.getInstance(), player.getName(), player.getUniqueId().toString());
        } else {
            s = new ConsoleCommandSender();
        }

        this.command.execute(s, this.command.getLabel(), args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        ICommandSender s;
        if (sender instanceof ProxiedPlayer player) {
            s = new Player(Streamline.getInstance(), player.getName(), player.getUniqueId().toString());
        } else {
            s = new ConsoleCommandSender();
        }

        return this.command.onTabComplete(s, this.command, args[args.length - 1], args);
    }
}
