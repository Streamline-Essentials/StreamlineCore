package singularity.command;

import lombok.Getter;
import singularity.Singularity;
import singularity.modules.ModuleLike;

import java.io.File;

@Getter
public abstract class ModuleCommand extends CosmicCommand {
    private final ModuleLike owningModule;

    public ModuleCommand(ModuleLike module, String base, String permission, String... aliases) {
        this(module, base, permission, new File(module.getDataFolder(), Singularity.getCommandsFolderChild()), aliases);
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

    public void modulize() {
        getOwningModule().addCommand(this);
    }

    public void demodulize() {
        getOwningModule().removeCommand(this);
    }

    @Override
    public void register() {
        if (! isEnabled()) return;

        CommandHandler.registerModuleCommand(this);

        modulize();
    }

    @Override
    public void unregister() {
        if (! isEnabled()) if (! CommandHandler.getLoadedModuleCommands().containsKey(getIdentifier())) return;

        CommandHandler.unregisterModuleCommand(this);

        demodulize();
    }

    @Override
    public void disable() {
        CommandHandler.unregisterModuleCommand(this);
    }
}