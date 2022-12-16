package net.streamline.api.command;

import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.StreamlineModule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class ModuleCommand extends StreamlineCommand {
    @Getter
    private final ModuleLike owningModule;

    public ModuleCommand(ModuleLike module, String base, String permission, String... aliases) {
        this(module, base, permission, new File(module.getDataFolder(), SLAPI.getCommandsFolderChild()), aliases);
    }


    public ModuleCommand(ModuleLike module, String base, String permission, File parentDirectory, String... aliases) {
        super(module.getIdentifier(), base, permission, parentDirectory, aliases);
        this.owningModule = module;
    }

//    public static String[] withLabel(ModuleLike module, String base, String... before) {
//        List<String> names = new ArrayList<>(List.of(before));
//        names.add(base);
//
//        List<String> newAliases = new ArrayList<>();
//        for (String name : names) {
//            newAliases.add(name);
//            newAliases.add(module.getIdentifier() + ":" + name);
//        }
//
//        return newAliases.toArray(String[]::new);
//    }

    @Override
    public void register() {
        if (! isEnabled()) return;

        CommandHandler.registerModuleCommand(this);
    }

    @Override
    public void unregister() {
        if (! isEnabled()) if (! CommandHandler.getLoadedModuleCommands().containsKey(getIdentifier())) return;

        CommandHandler.unregisterModuleCommand(this);
    }

    @Override
    public void disable() {
        CommandHandler.unregisterModuleCommand(this);
    }
}