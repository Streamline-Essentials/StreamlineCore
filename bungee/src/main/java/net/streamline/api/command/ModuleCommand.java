package net.streamline.api.command;

import net.streamline.api.modules.BundledModule;

public abstract class ModuleCommand extends StreamlineCommand {
    private final BundledModule owningModule;

    public ModuleCommand(BundledModule module, String base, String permission, String... aliases) {
        super(module, base, permission, aliases);
        this.owningModule = module;
    }
}