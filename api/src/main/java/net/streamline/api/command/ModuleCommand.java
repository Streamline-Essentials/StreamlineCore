package net.streamline.api.command;

import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.modules.StreamlineModule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class ModuleCommand extends StreamlineCommand {
    @Getter
    private final StreamlineModule owningModule;

    public ModuleCommand(StreamlineModule module, String base, String permission, String... aliases) {
        this(module, base, permission, new File(module.getDataFolder(), SLAPI.getInstance().getPlatform().getCommandsFolderChild()), withLabel(module, base, aliases));
    }


    public ModuleCommand(StreamlineModule module, String base, String permission, File parentDirectory, String... aliases) {
        super(base, permission, parentDirectory, withLabel(module, base, aliases));
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
        if (! isEnabled()) return;

        SLAPI.getInstance().getPlatform().registerModuleCommand(this);
    }

    @Override
    public void unregister() {
        if (! isEnabled()) if (! SLAPI.getInstance().getPlatform().getLoadedModuleCommands().containsKey(getIdentifier())) return;

        SLAPI.getInstance().getPlatform().unregisterModuleCommand(this);
    }

    @Override
    public void disable() {
        SLAPI.getInstance().getPlatform().unregisterModuleCommand(this);
    }
}