package net.streamline.api.base.command;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.base.Streamline;
import net.streamline.api.utils.StringUtil;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

public abstract class Command {
    private final String name;
    private String nextLabel;
    private String label;
    private List<String> aliases;
    private List<String> activeAliases;
    private CommandMap commandMap = null;
    protected String description = "";
    protected String usageMessage;
    private String permission;
    private String permissionMessage;

    protected Command(String name) {
        this(name, "", "/" + name, new ArrayList<String>());
    }

    protected Command(String name, String description, String usageMessage, List<String> aliases) {
        this.name = name;
        this.nextLabel = name;
        this.label = name;
        this.description = description;
        this.usageMessage = usageMessage;
        this.aliases = aliases;
        this.activeAliases = new ArrayList<String>(aliases);
    }

    public abstract  boolean execute(CommandExecutor executor, String commandLabel, String[] args);
    public List<String> tabComplete(CommandExecutor executor, String alias, String[] args) throws IllegalArgumentException {
        Validate.notNull(executor, "Executor cannot be null");
        Validate.notNull(args, "Arguments cannot be null");
        Validate.notNull(alias, "Alias cannot be null");

        if(args.length==0) {return ImmutableList.of();}

        String lastWord = args[args.length-1];
        ProxiedPlayer executorPlayer = executor instanceof CommandExecutor ? (CommandExecutor) executor : null;

        ArrayList<String> matchedPlayers = new ArrayList<String>();
        for(ProxiedPlayer player : executorPlayer.getServer().getInfo().getPlayers()) {
            String name = player.getName();
            if( && StringUtil.startsWithIgnoreCase(name, lastWord)) {
                matchedPlayers.add(name);
            }
        }
    }
}
