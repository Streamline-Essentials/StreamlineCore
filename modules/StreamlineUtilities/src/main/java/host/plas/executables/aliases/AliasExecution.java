package host.plas.executables.aliases;

import host.plas.StreamlineUtilities;
import host.plas.executables.ExecutableHandler;
import lombok.Getter;
import lombok.Setter;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.modules.ModuleUtils;

import java.util.concurrent.ConcurrentSkipListMap;

@Setter
@Getter
public class AliasExecution {
    private Type type;
    private String execution;
    private ConcurrentSkipListMap<String, String> assignables = new ConcurrentSkipListMap<>();

    public AliasExecution(Type type, String execution, ConcurrentSkipListMap<String, String> assignables) {
        this.type = type;
        this.execution = execution;
        this.assignables = assignables;
    }

    public enum Type {
        COMMAND,
        FUNCTION,
        SCRIPT,
        ;
    }

    public boolean execute(CommandContext<CosmicCommand> context) {
        switch (getType()) {
            case COMMAND:
                ModuleUtils.runAs(context.getSender(), execution);
                break;
            case SCRIPT:
                return false;
            case FUNCTION:
                if (! ExecutableHandler.isFunctionLoadedByName(execution)) {
                    ModuleUtils.sendMessage(context.getSender(), StreamlineUtilities.getMessages().errorsFunctionsNotLoaded());
                    return false;
                }
                if (! ExecutableHandler.isFunctionEnabledByName(execution)) {
                    ModuleUtils.sendMessage(context.getSender(), StreamlineUtilities.getMessages().errorsFunctionsNotEnabled());
                    return false;
                }
                ConcurrentSkipListMap<String, String> localAssignables = new ConcurrentSkipListMap<>(assignables);
                context.getArgs().forEach((arg) -> {
                    int index = arg.getIndex();
                    String content = arg.getContent();

                    String argIdentifier = "arg" + index;
                    localAssignables.put(argIdentifier, content);
                });

                ExecutableHandler.getFunction(execution).runAs(context.getSender(), localAssignables);
                break;
        }
        return true;
    }
}
