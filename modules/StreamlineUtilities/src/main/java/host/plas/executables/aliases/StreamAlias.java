package host.plas.executables.aliases;

import lombok.Getter;
import lombok.Setter;
import singularity.command.ModuleCommand;
import host.plas.StreamlineUtilities;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;

import java.io.File;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Setter
@Getter
public class StreamAlias extends ModuleCommand {
    private AliasExecution execution;


    public StreamAlias(String base, File folder) {
        super(StreamlineUtilities.getInstance(),
                base,
                "streamline.utils.alias.<change-this>",
                folder,
                "change-this-too");
        AliasExecution.Type aeType = AliasExecution.Type.valueOf(getCommandResource().getOrSetDefault("execution.type", AliasExecution.Type.FUNCTION.toString()));
        String aeExecutes = getCommandResource().getOrSetDefault("execution.executes", "change-this");
        ConcurrentSkipListMap<String, String> assignables = grabAssignables();

        this.execution = new AliasExecution(aeType, aeExecutes, assignables);
    }

    public ConcurrentSkipListMap<String, String> grabAssignables() {
        ConcurrentSkipListMap<String, String> assignables = new ConcurrentSkipListMap<>();

        getCommandResource().singleLayerKeySet("assignables").forEach(key -> {
            String value = getCommandResource().getOrSetDefault("assignables." + key, "change-this");
            assignables.put(key, value);
        });

        return assignables;
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        execution.execute(context);
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender streamSender, String[] strings) {
        if (AliasCompletions.getCompletions(this).containsKey(strings.length)) {
            return AliasCompletions.getCompletions(this).get(strings.length);
        }

        return new ConcurrentSkipListSet<>();
    }
}
