package singularity.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import lombok.Getter;
import lombok.Setter;
import singularity.command.CommandHandler;
import singularity.command.CosmicCommand;
import singularity.modules.ModuleLike;
import singularity.modules.CosmicModule;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class CommandResource extends SimpleConfiguration {
    final String identifier;
    @Setter
    CosmicCommand command;
    @Setter
    ModuleLike module;


    public CommandResource(CosmicCommand command, File parentDirectory) {
        super(command.getIdentifier() + ".yml", parentDirectory, false);
        this.identifier = command.getIdentifier();
        this.command = command;

        if (this.exists()) {
            if (this.empty()) {
                afterInit();
            }
        } else {
            afterInit();
        }

        if (! getResource().getBoolean("basic.enabled") && getResource().getOrDefault("DO-NOT-TOUCH.version", 0d) < 1d) {
            afterInit();
        }

        syncCommand();
    }

    public CommandResource(CosmicModule module, CosmicCommand command, File parentDirectory) {
        this(command, parentDirectory);
        this.module = module;
    }

    @Override
    public void init() {

    }

    public void afterInit() {
        write("DO-NOT-TOUCH.version", 1d);
        write("basic.enabled", true);
        write("basic.label", command.getBase());
        write("basic.permissions.default", command.getPermission());
        write("basic.aliases", Arrays.stream(command.getAliases()).collect(Collectors.toList()));
    }

    public void syncCommand() {
        boolean enabled = getResource().getBoolean("basic.enabled");

        if (! enabled && getResource().getOrDefault("DO-NOT-TOUCH.version", 0d) < 1d) {
            enabled = true;
            write("basic.enabled", true);
            write("DO-NOT-TOUCH.version", 1d);
        }

        String label = getResource().getString("basic.label");
        String defaultPermission = getResource().getString("basic.permissions.default");
        List<String> aliases = getResource().getStringList("basic.aliases");

        if (this.command.isLoaded()) if (! enabled) CommandHandler.registerStreamlineCommand(this.command);
        if (! this.command.isLoaded()) if (enabled) CommandHandler.unregisterStreamlineCommand(this.command);
        this.command.setBase(label);
        this.command.setPermission(defaultPermission);
        this.command.setAliases(aliases.toArray(new String[0]));
    }
}
