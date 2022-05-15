package net.streamline.api.base.modules;

import java.io.File;
import java.util.regex.Pattern;

public interface ModuleLoader {
    public Module loadModule(File file) throws InvalidModuleException, UnknownDependencyException;
    public ModuleDescriptionFile getModuleDescription(File file) throws InvalidDescriptionException;
    public Pattern[] getModuleFileFilters();
    /*public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener, Module module);*/
    public void enableModule(Module module);
    public void disableModule(Module module);

}
