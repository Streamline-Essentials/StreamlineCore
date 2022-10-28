package net.streamline.api.configs;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.command.CommandHandler;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.StreamlineModule;
import tv.quaint.storage.resources.flat.FlatFileResource;
import tv.quaint.storage.resources.flat.simple.SimpleConfiguration;
import tv.quaint.thebase.lib.leonhard.storage.Config;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CommandResource extends SimpleConfiguration {
    @Getter
    final String identifier;
    @Getter @Setter
    StreamlineCommand command;
    @Getter @Setter
    ModuleLike module;


    public CommandResource(StreamlineCommand command, File parentDirectory) {
        super(command.getIdentifier() + ".yml", parentDirectory, false);
        this.identifier = command.getIdentifier();
        this.command = command;

        if (this.exists()) {
            if (this.empty()) {
                init();
            }
        } else {
            init();
        }

        if (! getResource().getBoolean("basic.enabled") && getResource().getOrDefault("DO-NOT-TOUCH.version", 0d) < 1d) {
            init();
        }

        syncCommand();
    }

    public CommandResource(StreamlineModule module, StreamlineCommand command, File parentDirectory) {
        this(command, parentDirectory);
        this.module = module;
    }

    @Override
    public void init() {
        write("DO-NOT-TOUCH.version", 1d);
        write("basic.enabled", true);
        write("basic.label", command.getBase());
        write("basic.permissions.default", command.getPermission());
        write("basic.aliases", Arrays.stream(command.getAliases()).toList());
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
