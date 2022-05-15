package net.streamline.api.base.modules;

import java.io.File;

public interface ModuleManager {
    public void registerInterface(Class<? extends ModuleLoader> loader) throws IllegalArgumentException;
    public Module getModule(String name);
    public Module[] getModules();
    public boolean isModuleEnabled(String name);
    public boolean isModuleEnabled(Module module);
    public Module loadModule(File file) throws InvalidModuleException, InvalidDescriptionException, UnknownDependencyException;
    public Module[] loadModules(File directory);
    public void disableModules();
    public void clearModules();
    public void enableModule(Module module);
    public void disableModule(Module module);
}
