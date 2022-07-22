package net.streamline.api.command;

import lombok.Getter;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.base.Streamline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class ModuleCommand extends StreamlineCommand {
    @Getter
    private final StreamlineModule owningModule;

    public ModuleCommand(StreamlineModule module, String base, String permission, String... aliases) {
        super(base, permission, new File(module.getDataFolder(), Streamline.getCommandsFolderChild()), withLabel(module, base, aliases));
        this.owningModule = module;
    }

    public static String[] withLabel(StreamlineModule module, String base, String... before) {
        List<String> names = new ArrayList<>(List.of(before));
        names.add(base);

        List<String> newAliases = new ArrayList<>();
        for (String name : names) {
            newAliases.add(name);
            newAliases.add(module.identifier() + ":" + name);
        }

        return newAliases.toArray(String[]::new);
    }

    @Override
    public void register() {
        Streamline.registerModuleCommand(this);
    }

    @Override
    public void unregister() {
        Streamline.unregisterModuleCommand(this);
    }

    @Override
    public void disable() {
        Streamline.unregisterModuleCommand(this);
    }
}