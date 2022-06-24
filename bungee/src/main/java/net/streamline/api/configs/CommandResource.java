package net.streamline.api.configs;

import de.leonhard.storage.Yaml;
import net.streamline.api.command.Command;
import net.streamline.base.Streamline;

import java.io.File;
import java.util.List;

public abstract class CommandResource extends FlatFileResource<Yaml> {
    public String identifier;
    public Command command;

    public CommandResource(Command command, File parentDirectory) {
        super(Yaml.class, command.getName() + ".yml", parentDirectory, false);
        this.identifier = command.getName();
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

    public void defaults() {
        write("basic.enabled", command.isRegistered());
        write("basic.label", command.getLabel());
        write("basic.permissions.default", command.getPermission());
        write("basic.aliases", command.getAliases());
    }

    public void syncCommand() {
        boolean enabled = resource.getBoolean("basic.enabled");
        String label = resource.getString("basic.label");
        String defaultPermission = resource.getString("basic.permissions.default");
        List<String> aliases = resource.getStringList("basic.aliases");

        if (this.command.isRegistered()) if (! enabled) this.command.unregister(Streamline.getInstance().getModuleManager().getCommandMap());
        if (! this.command.isRegistered()) if (enabled) this.command.register(Streamline.getInstance().getModuleManager().getCommandMap());
        this.command.setLabel(label);
        this.command.setPermission(defaultPermission);
        this.command.setAliases(aliases);
    }
}
