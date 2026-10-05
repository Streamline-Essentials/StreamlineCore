package gg.drak.tacoessentials.alias;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.commands.Perms;
import singularity.command.CommandHandler;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.command.result.CommandResult;
import singularity.utils.MessageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * The live command behind one alias. It looks its alias up by name on every use, so editing
 * an alias changes what the command does without re-registering it.
 *
 * <p>Alias commands stay out of the module's command list: that list is built once by
 * {@code registerCommands()} and reused on the next enable, while aliases are loaded from
 * the database on every enable and come and go while the module runs.</p>
 */
public class AliasCommand extends ModuleCommand {

    public AliasCommand(String name) {
        super(TacoEssentials.getInstance(), name, Perms.ALIAS,
                new File(TacoEssentials.getInstance().getDataFolder(), "alias-commands"));
    }

    @Override
    public void register() {
        if (! isEnabled()) return;
        CommandHandler.registerModuleCommand(this);
    }

    /** Unregisters by identifier, so only while the label is still this command's and not another one's that took it over. */
    @Override
    public void unregister() {
        if (CommandHandler.getModuleCommand(getIdentifier()) == this) CommandHandler.unregisterModuleCommand(this);
    }

    @Override
    public CommandResult<?> resultedRun(CommandContext<CosmicCommand> context) {
        AliasEngine.run(getBase(), context.getSender(), args(context));
        return success();
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        ConcurrentSkipListSet<String> out = new ConcurrentSkipListSet<>();
        String[] raw = context.getArgsArray();
        int index = Math.max(0, raw.length - 1);
        List<String> candidates = AliasTabs.complete(AliasManager.get(getBase()), index);
        String typed = raw.length == 0 ? "" : raw[raw.length - 1];
        out.addAll(MessageUtils.getCompletion(new ArrayList<>(candidates), typed));
        return out;
    }

    /** The typed arguments; an invocation without any arrives as one empty argument. */
    private static String[] args(CommandContext<CosmicCommand> context) {
        List<String> args = Arrays.stream(context.getArgsArray()).filter(a -> ! a.isEmpty()).collect(Collectors.toList());
        return args.toArray(new String[0]);
    }
}
