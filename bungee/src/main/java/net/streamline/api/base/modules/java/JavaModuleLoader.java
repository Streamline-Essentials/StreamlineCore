package net.streamline.api.base.modules.java;

import net.md_5.bungee.api.ProxyServer;
import net.streamline.api.base.modules.*;
import net.streamline.api.base.modules.Module;
import org.apache.commons.lang3.Validate;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class JavaModuleLoader implements ModuleLoader {
    final ProxyServer server;
    private final Pattern[] fileFilters = new Pattern[] { Pattern.compile("\\.jar$"), };
    private final Map<String, Class<?>> classes = new java.util.concurrent.ConcurrentHashMap<String, Class<?>>(); // Streamline
    private final Map<String, ModuleClassLoader> loaders = new LinkedHashMap<String, ModuleClassLoader>();

    @Deprecated
    public JavaModuleLoader(ProxyServer instance) {
        Validate.notNull(instance, "Server cannot be null");
        server = instance;
    }

    public Module loadPlugin(final File file) throws InvalidModuleException {
        Validate.notNull(file, "File cannot be null");
        if(!file.exists()) {throw new InvalidModuleException(new FileNotFoundException(file.getPath() + " does not exist"));}
        final ModuleDescriptionFile description;
        try {
            description = getModuleDescription(file);
        } catch (InvalidDescriptionException ex) {
            throw new InvalidModuleException(ex);
        }

        final File parentFile = file.getParentFile();
        final File dataFolder = new File(parentFile, description.getName());
        @SuppressWarnings("deprecation")
        final File oldDataFolder = new File(parentFile, description.getRawName());

        // Found old data folder
        if(dataFolder.equals(oldDataFolder)) {
            // They are equal -- nothing needs to be done!
        } else if(dataFolder.isDirectory() && oldDataFolder.isDirectory()) {
            server.getLogger().warning(String.format(
                    "While loading %s (%s) found old-data folder: `%s' next to the new one `%s'",
                    description.getFullName(),
                    file,
                    oldDataFolder,
                    dataFolder
            ));
        } else if(oldDataFolder.isDirectory() && !dataFolder.exists()) {
            if(!oldDataFolder.renameTo(dataFolder)) {
                throw new InvalidModuleException("Unable to rename old data folder : `"+oldDataFolder+"' to: `"+dataFolder+"'");
            }
            server.getLogger().log(Level.INFO, String.format(
                    "While loading %s (%s) renamed data folder: `%s' to `%s'",
                    description.getFullName(), file, oldDataFolder,dataFolder
            ));
        }
        if (dataFolder.exists() && !dataFolder.isDirectory()) {
            throw new InvalidModuleException(String.format(
                    "Projected dataFolder: `%s' for %s (%s) exists and is not a directory",
                    dataFolder,
                    description.getFullName(),
                    file
            ));
        }

        for(final String moduleName : description.getDepend()) {
            if(loaders == null) {throw new UnknownDependencyException(moduleName);}
            ModuleClassLoader current = loaders.get(moduleName);
            if(current == null) {throw new UnknownDependencyException(moduleName);}
        }
        final ModuleClassLoader loader;
        try {loader = new ModuleClassLoader(this, getClass().getClassLoader(), description, dataFolder, file);}
        catch(InvalidModuleException ex) {throw ex;}
        catch(Throwable ex) {throw new InvalidModuleException(ex);}

        loaders.put(description.getName(), loader);
        return loader.plugin;
    }

    public ModuleDescriptionFile getModuleDescription(File file) throws InvalidDescriptionException {
        Validate.notNull(file, "File cannot be null");

        JarFile jar = null;
        InputStream stream = null;

        try {
            jar = new JarFile(file);
            JarEntry entry = jar.getJarEntry("module.yml");

            if(entry == null) {
                throw new InvalidDescriptionException(new FileNotFoundException("Jar does not contain module.yml"));
            }

            stream = jar.getInputStream(entry);

            return new ModuleDescriptionFile(stream);
        }
        catch (IOException | YAMLException ex) {throw new InvalidDescriptionException(ex);}
        finally {
            if (jar != null) {try {jar.close();} catch (IOException ignore) {}}
            if (stream != null) {try {stream.close();} catch (IOException ignore) {}}
        }
    }

    public Pattern[] getModuleFileFilters() {return fileFilters.clone();}

    Class<?> getClassByName(final String name) {
        Class<?> cachedClass = classes.get(name);
        if(cachedClass != null) {return cachedClass;}
        else {
            for (String current : loaders.keySet()) {
                ModuleClassLoader loader = loaders.get(current);
                try {cachedClass = loader.findClass(name, false);}
                catch(ClassNotFoundException ignored) {}
                if(cachedClass != null) {return cachedClass;}
            }
        }
        return null;
    }

    void setClass(final String name, final Class<?> clazz) {if(!classes.containsKey(name)) {classes.put(name, clazz);}}
    private void removeClass(String name) {Class<?> clazz = classes.remove(name);}
    public void enableModule(final Module module) {
        Validate.isTrue(module instanceof JavaModule, "Module is not associated with this ModuleLoader");
        if(!module.isEnabled()) {
            module.getLogger().info("Enabling " + module.getDescription().getFullName());

            JavaModule jModule = (JavaModule) module;
            String moduleName = jModule.getDescription().getName();

            if(!loaders.containsKey(moduleName)) {
                loaders.put(moduleName, (ModuleClassLoader) jModule.getClassLoader());
            }

            try {jModule.setEnabled(true);}
            catch (Throwable ex) {server.getLogger().log(Level.SEVERE, "Error occured while enabling " +
                    module.getDescription().getFullName() + " (Is it up to date?)", ex);}
            //Call plugin enable event -- Here
        }
    }
    public void disableModule(Module module) {
        Validate.isTrue(module instanceof JavaModule, "Module is not associated with this ModuleLoader");
        if(module.isEnabled()) {
            module.getLogger().info(String.format("Disabling %s", module.getDescription().getFullName()));

            //Call plugin Disable Event -- Here

            JavaModule jModule = (JavaModule) module;
            ClassLoader cloader = jModule.getClassLoader();

            try {jModule.setEnabled(false);}
            catch (Throwable ex) {server.getLogger().log(Level.SEVERE, "Error occured while enabling " +
                    module.getDescription().getFullName() + " (Is it up to date?)", ex);}

            loaders.remove(jModule.getDescription().getName());
        }
    }
}
