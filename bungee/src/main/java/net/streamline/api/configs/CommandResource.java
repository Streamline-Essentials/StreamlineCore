package net.streamline.api.configs;

import de.leonhard.storage.Config;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.modules.BundledModule;
import net.streamline.base.Streamline;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CommandResource extends FlatFileResource<Config> {
    public String identifier;
    public StreamlineCommand command;

    public CommandResource(StreamlineCommand command, File parentDirectory) {
        super(Config.class, command.getIdentifier() + ".yml", parentDirectory, false);
        this.identifier = command.getIdentifier();
        this.command = command;

        if (this.exists()) {
            if (this.empty()) {
                defaults();
            }
        } else {
            defaults();
        }

        syncCommand();
    }

    public CommandResource(BundledModule module, StreamlineCommand command, File parentDirectory) {
        super(module, Config.class, command.getIdentifier() + ".yml", parentDirectory, false);
    }

    public void defaults() {
        write("basic.enabled", command.isEnabled());
        write("basic.label", command.getBase());
        write("basic.permissions.default", command.getPermission());
        write("basic.aliases", Arrays.stream(command.getAliases()).toList());
    }

    public void syncCommand() {
        boolean enabled = resource.getBoolean("basic.enabled");
        String label = resource.getString("basic.label");
        String defaultPermission = resource.getString("basic.permissions.default");
        List<String> aliases = resource.getStringList("basic.aliases");

        if (this.command.isEnabled()) if (! enabled) Streamline.registerStreamlineCommand(this.command);
        if (! this.command.isEnabled()) if (enabled) Streamline.unregisterStreamlineCommand(this.command);
        this.command.setBase(label);
        this.command.setPermission(defaultPermission);
        this.command.setAliases(aliases.toArray(new String[0]));
    }
}
