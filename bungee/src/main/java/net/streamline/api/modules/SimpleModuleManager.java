package net.streamline.api.modules;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import lombok.Getter;
import net.streamline.api.BasePlugin;
import net.streamline.api.command.Command;
import net.streamline.api.command.ModuleCommandYamlParser;
import net.streamline.api.command.SimpleCommandMap;
import net.streamline.api.configs.StorageUtils;
import net.streamline.api.permissions.Permissible;
import net.streamline.api.permissions.Permission;
import net.streamline.api.permissions.PermissionDefault;
import net.streamline.base.Streamline;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

public class SimpleModuleManager implements ModuleManager {
    private final BasePlugin server;
    private final Map<Pattern, ModuleLoader> fileAssociations = new HashMap<Pattern, ModuleLoader>();
    private final List<Module> Modules = new ArrayList<Module>();
    private final Map<String, Module> lookupNames = new HashMap<String, Module>();
//    private MutableGraph<String> dependencyGraph = GraphBuilder.directed().build();
    private File updateDirectory;
    @Getter
    private final SimpleCommandMap commandMap;
    private final Map<String, Permission> permissions = new HashMap<>();
    private final Map<Boolean, Set<Permission>> defaultPerms = new LinkedHashMap<Boolean, Set<Permission>>();
    private final Map<String, Map<Permissible, Boolean>> permSubs = new HashMap<String, Map<Permissible, Boolean>>();
    private final Map<Boolean, Map<Permissible, Boolean>> defSubs = new HashMap<Boolean, Map<Permissible, Boolean>>();
    private boolean useTimings = false;

    public SimpleModuleManager(@NotNull BasePlugin instance, @NotNull SimpleCommandMap commandMap) {
        server = instance;
        this.commandMap = commandMap;

        defaultPerms.put(true, new LinkedHashSet<Permission>());
        defaultPerms.put(false, new LinkedHashSet<Permission>());
    }

    /**
     * Registers the specified Module loader
     *
     * @param loader Class name of the ModuleLoader to register
     * @throws IllegalArgumentException Thrown when the given Class is not a
     *     valid ModuleLoader
     */
    @Override
    public void registerInterface(@NotNull Class<? extends ModuleLoader> loader) throws IllegalArgumentException {
        ModuleLoader instance;

        if (ModuleLoader.class.isAssignableFrom(loader)) {
            Constructor<? extends ModuleLoader> constructor;

            try {
                constructor = loader.getConstructor(BasePlugin.class);
                instance = constructor.newInstance(server);
            } catch (NoSuchMethodException ex) {
                String className = loader.getName();

                throw new IllegalArgumentException(String.format("Class %s does not have a public %s(Server) constructor", className, className), ex);
            } catch (Exception ex) {
                throw new IllegalArgumentException(String.format("Unexpected exception %s while attempting to construct a new instance of %s", ex.getClass().getName(), loader.getName()), ex);
            }
        } else {
            throw new IllegalArgumentException(String.format("Class %s does not implement interface ModuleLoader", loader.getName()));
        }

        Pattern[] patterns = instance.getModuleFileFilters();

        synchronized (this) {
            for (Pattern pattern : patterns) {
                fileAssociations.put(pattern, instance);
            }
        }
    }

    /**
     * Loads the Modules contained within the specified directory
     *
     * @param directory Directory to check for Modules
     * @return A list of all Modules loaded
     */
    @Override
    @NotNull
    public Module[] loadModules(@NotNull File directory) {
        Preconditions.checkArgument(directory != null, "Directory cannot be null");
        Preconditions.checkArgument(directory.isDirectory(), "Directory must be a directory");

        List<Module> result = new ArrayList<Module>();
        Set<Pattern> filters = fileAssociations.keySet();

        if (! (BasePlugin.getInstance().getUpdateFolderFile() == null)) {
            updateDirectory = BasePlugin.getInstance().getUpdateFolderFile();
        }

        Map<String, File> Modules = new HashMap<String, File>();
        Set<String> loadedModules = new HashSet<String>();
        Map<String, String> ModulesProvided = new HashMap<>();
        Map<String, Collection<String>> dependencies = new HashMap<String, Collection<String>>();
        Map<String, Collection<String>> softDependencies = new HashMap<String, Collection<String>>();

        // This is where it figures out all possible Modules
        for (File file : directory.listFiles()) {
            ModuleLoader loader = null;
            for (Pattern filter : filters) {
                Matcher match = filter.matcher(file.getName());
                if (match.find()) {
                    loader = fileAssociations.get(filter);
                }
            }

            if (loader == null) continue;

            ModuleDescriptionFile description = null;
            try {
                description = loader.getModuleDescription(file);
                String name = description.getName();
                if (name.equalsIgnoreCase("bukkit") || name.equalsIgnoreCase("minecraft") || name.equalsIgnoreCase("mojang")) {
                    server.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "': Restricted Name");
                    continue;
                } else if (description.rawName.indexOf(' ') != -1) {
                    server.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "': uses the space-character (0x20) in its name");
                    continue;
                }
            } catch (InvalidDescriptionException ex) {
                server.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", ex);
                continue;
            }

            File replacedFile = Modules.put(description.getName(), file);
            if (replacedFile != null) {
                server.getLogger().severe(String.format(
                        "Ambiguous Module name `%s' for files `%s' and `%s' in `%s'",
                        description.getName(),
                        file.getPath(),
                        replacedFile.getPath(),
                        directory.getPath()
                ));
            }

            String removedProvided = ModulesProvided.remove(description.getName());
            if (removedProvided != null) {
                server.getLogger().warning(String.format(
                        "Ambiguous Module name `%s'. It is also provided by `%s'",
                        description.getName(),
                        removedProvided
                ));
            }

            for (String provided : description.getProvides()) {
                File ModuleFile = Modules.get(provided);
                if (ModuleFile != null) {
                    server.getLogger().warning(String.format(
                            "`%s provides `%s' while this is also the name of `%s' in `%s'",
                            file.getPath(),
                            provided,
                            ModuleFile.getPath(),
                            directory.getPath()
                    ));
                } else {
                    String replacedModule = ModulesProvided.put(provided, description.getName());
                    if (replacedModule != null) {
                        server.getLogger().warning(String.format(
                                "`%s' is provided by both `%s' and `%s'",
                                provided,
                                description.getName(),
                                replacedModule
                        ));
                    }
                }
            }

            Collection<String> softDependencySet = description.getSoftDepend();
            if (softDependencySet != null && !softDependencySet.isEmpty()) {
                if (softDependencies.containsKey(description.getName())) {
                    // Duplicates do not matter, they will be removed together if applicable
                    softDependencies.get(description.getName()).addAll(softDependencySet);
                } else {
                    softDependencies.put(description.getName(), new LinkedList<String>(softDependencySet));
                }

                for (String depend : softDependencySet) {
//                    dependencyGraph.putEdge(description.getName(), depend);
                }
            }

            Collection<String> dependencySet = description.getDepend();
            if (dependencySet != null && !dependencySet.isEmpty()) {
                dependencies.put(description.getName(), new LinkedList<String>(dependencySet));

                for (String depend : dependencySet) {
//                    dependencyGraph.putEdge(description.getName(), depend);
                }
            }

            Collection<String> loadBeforeSet = description.getLoadBefore();
            if (loadBeforeSet != null && !loadBeforeSet.isEmpty()) {
                for (String loadBeforeTarget : loadBeforeSet) {
                    if (softDependencies.containsKey(loadBeforeTarget)) {
                        softDependencies.get(loadBeforeTarget).add(description.getName());
                    } else {
                        // softDependencies is never iterated, so 'ghost' Modules aren't an issue
                        Collection<String> shortSoftDependency = new LinkedList<String>();
                        shortSoftDependency.add(description.getName());
                        softDependencies.put(loadBeforeTarget, shortSoftDependency);
                    }

//                    dependencyGraph.putEdge(loadBeforeTarget, description.getName());
                }
            }
        }

        while (!Modules.isEmpty()) {
            boolean missingDependency = true;
            Iterator<Map.Entry<String, File>> ModuleIterator = Modules.entrySet().iterator();

            while (ModuleIterator.hasNext()) {
                Map.Entry<String, File> entry = ModuleIterator.next();
                String Module = entry.getKey();

                if (dependencies.containsKey(Module)) {
                    Iterator<String> dependencyIterator = dependencies.get(Module).iterator();

                    while (dependencyIterator.hasNext()) {
                        String dependency = dependencyIterator.next();

                        // Dependency loaded
                        if (loadedModules.contains(dependency)) {
                            dependencyIterator.remove();

                            // We have a dependency not found
                        } else if (!Modules.containsKey(dependency) && !ModulesProvided.containsKey(dependency)) {
                            missingDependency = false;
                            ModuleIterator.remove();
                            softDependencies.remove(Module);
                            dependencies.remove(Module);

                            server.getLogger().log(
                                    Level.SEVERE,
                                    "Could not load '" + entry.getValue().getPath() + "' in folder '" + directory.getPath() + "'",
                                    new UnknownDependencyException("Unknown dependency " + dependency + ". Please download and install " + dependency + " to run this Module."));
                            break;
                        }
                    }

                    if (dependencies.containsKey(Module) && dependencies.get(Module).isEmpty()) {
                        dependencies.remove(Module);
                    }
                }
                if (softDependencies.containsKey(Module)) {
                    Iterator<String> softDependencyIterator = softDependencies.get(Module).iterator();

                    while (softDependencyIterator.hasNext()) {
                        String softDependency = softDependencyIterator.next();

                        // Soft depend is no longer around
                        if (!Modules.containsKey(softDependency) && !ModulesProvided.containsKey(softDependency)) {
                            softDependencyIterator.remove();
                        }
                    }

                    if (softDependencies.get(Module).isEmpty()) {
                        softDependencies.remove(Module);
                    }
                }
                if (!(dependencies.containsKey(Module) || softDependencies.containsKey(Module)) && Modules.containsKey(Module)) {
                    // We're clear to load, no more soft or hard dependencies left
                    File file = Modules.get(Module);
                    ModuleIterator.remove();
                    missingDependency = false;

                    try {
                        net.streamline.api.modules.Module loadedModule = loadModule(file);
                        if (loadedModule != null) {
                            result.add(loadedModule);
                            loadedModules.add(loadedModule.getName());
                            loadedModules.addAll(loadedModule.getDescription().getProvides());
                        } else {
                            server.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'");
                        }
                        continue;
                    } catch (InvalidModuleException ex) {
                        server.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", ex);
                    }
                }
            }

            if (missingDependency) {
                // We now iterate over Modules until something loads
                // This loop will ignore soft dependencies
                ModuleIterator = Modules.entrySet().iterator();

                while (ModuleIterator.hasNext()) {
                    Map.Entry<String, File> entry = ModuleIterator.next();
                    String Module = entry.getKey();

                    if (!dependencies.containsKey(Module)) {
                        softDependencies.remove(Module);
                        missingDependency = false;
                        File file = entry.getValue();
                        ModuleIterator.remove();

                        try {
                            net.streamline.api.modules.Module loadedModule = loadModule(file);
                            if (loadedModule != null) {
                                result.add(loadedModule);
                                loadedModules.add(loadedModule.getName());
                                loadedModules.addAll(loadedModule.getDescription().getProvides());
                            } else {
                                server.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'");
                            }
                            break;
                        } catch (InvalidModuleException ex) {
                            server.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", ex);
                        }
                    }
                }
                // We have no Modules left without a depend
                if (missingDependency) {
                    softDependencies.clear();
                    dependencies.clear();
                    Iterator<File> failedModuleIterator = Modules.values().iterator();

                    while (failedModuleIterator.hasNext()) {
                        File file = failedModuleIterator.next();
                        failedModuleIterator.remove();
                        server.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "': circular dependency detected");
                    }
                }
            }
        }

        return result.toArray(new Module[result.size()]);
    }

    /**
     * Loads the Module in the specified file
     * <p>
     * File must be valid according to the current enabled Module interfaces
     *
     * @param file File containing the Module to load
     * @return The Module loaded, or null if it was invalid
     * @throws InvalidModuleException Thrown when the specified file is not a
     *     valid Module
     * @throws UnknownDependencyException If a required dependency could not
     *     be found
     */
    @Override
    @Nullable
    public synchronized Module loadModule(@NotNull File file) throws InvalidModuleException, UnknownDependencyException {
        Preconditions.checkArgument(file != null, "File cannot be null");

        checkUpdate(file);

        Set<Pattern> filters = fileAssociations.keySet();
        Module result = null;

        for (Pattern filter : filters) {
            String name = file.getName();
            Matcher match = filter.matcher(name);

            if (match.find()) {
                ModuleLoader loader = fileAssociations.get(filter);

                result = loader.loadModule(file);
            }
        }

        if (result != null) {
            Modules.add(result);
            lookupNames.put(result.getDescription().getName(), result);
            for (String provided : result.getDescription().getProvides()) {
                lookupNames.putIfAbsent(provided, result);
            }
        }

        return result;
    }

    private void checkUpdate(@NotNull File file) {
        if (updateDirectory == null || !updateDirectory.isDirectory()) {
            return;
        }

        File updateFile = new File(updateDirectory, file.getName());
        if (updateFile.isFile() && StorageUtils.copy(updateFile, file)) {
            updateFile.delete();
        }
    }

    /**
     * Checks if the given Module is loaded and returns it when applicable
     * <p>
     * Please note that the name of the Module is case-sensitive
     *
     * @param name Name of the Module to check
     * @return Module if it exists, otherwise null
     */
    @Override
    @Nullable
    public synchronized Module getModule(@NotNull String name) {
        return lookupNames.get(name.replace(' ', '_'));
    }

    @Override
    @NotNull
    public synchronized Module[] getModules() {
        return Modules.toArray(new Module[Modules.size()]);
    }

    /**
     * Checks if the given Module is enabled or not
     * <p>
     * Please note that the name of the Module is case-sensitive.
     *
     * @param name Name of the Module to check
     * @return true if the Module is enabled, otherwise false
     */
    @Override
    public boolean isModuleEnabled(@NotNull String name) {
        Module Module = getModule(name);

        return isModuleEnabled(Module);
    }

    /**
     * Checks if the given Module is enabled or not
     *
     * @param Module Module to check
     * @return true if the Module is enabled, otherwise false
     */
    @Override
    public boolean isModuleEnabled(@Nullable Module Module) {
        if ((Module != null) && (Modules.contains(Module))) {
            return Module.isEnabled();
        } else {
            return false;
        }
    }

    @Override
    public void enableModule(@NotNull final Module Module) {
        if (!Module.isEnabled()) {
            List<Command> ModuleCommands = ModuleCommandYamlParser.parse(Module);

            if (!ModuleCommands.isEmpty()) {
                commandMap.registerAll(Module.getDescription().getName(), ModuleCommands);
            }

            try {
                Module.getModuleLoader().enableModule(Module);
            } catch (Throwable ex) {
                server.getLogger().log(Level.SEVERE, "Error occurred (in the Module loader) while enabling " + Module.getDescription().getFullName() + " (Is it up to date?)", ex);
            }
        }
    }

    @Override
    public void disableModules() {
        Module[] Modules = getModules();
        for (int i = Modules.length - 1; i >= 0; i--) {
            disableModule(Modules[i]);
        }
    }

    @Override
    public void disableModule(@NotNull final Module Module) {
        if (Module.isEnabled()) {
            try {
                Module.getModuleLoader().disableModule(Module);
            } catch (Throwable ex) {
                server.getLogger().log(Level.SEVERE, "Error occurred (in the Module loader) while disabling " + Module.getDescription().getFullName() + " (Is it up to date?)", ex);
            }

            try {
                server.getScheduler().cancelTasks(Module);
            } catch (Throwable ex) {
                server.getLogger().log(Level.SEVERE, "Error occurred (in the Module loader) while cancelling tasks for " + Module.getDescription().getFullName() + " (Is it up to date?)", ex);
            }

            try {
                server.getServicesManager().unregisterAll(Module);
            } catch (Throwable ex) {
                server.getLogger().log(Level.SEVERE, "Error occurred (in the Module loader) while unregistering services for " + Module.getDescription().getFullName() + " (Is it up to date?)", ex);
            }
        }
    }

    @Override
    public void clearModules() {
        synchronized (this) {
            disableModules();
            Modules.clear();
            lookupNames.clear();
//            dependencyGraph = GraphBuilder.directed().build();
            fileAssociations.clear();
            permissions.clear();
            defaultPerms.get(true).clear();
            defaultPerms.get(false).clear();
        }
    }

    @Override
    @Nullable
    public Permission getPermission(@NotNull String name) {
        return permissions.get(name.toLowerCase(java.util.Locale.ENGLISH));
    }

    @Override
    public void addPermission(@NotNull Permission perm) {
        addPermission(perm, true);
    }

    @Deprecated
    public void addPermission(@NotNull Permission perm, boolean dirty) {
        String name = perm.getName().toLowerCase(java.util.Locale.ENGLISH);

        if (permissions.containsKey(name)) {
            throw new IllegalArgumentException("The permission " + name + " is already defined!");
        }

        permissions.put(name, perm);
        calculatePermissionDefault(perm, dirty);
    }

    @Override
    @NotNull
    public Set<Permission> getDefaultPermissions(boolean op) {
        return ImmutableSet.copyOf(defaultPerms.get(op));
    }

    @Override
    public void removePermission(@NotNull Permission perm) {
        removePermission(perm.getName());
    }

    @Override
    public void removePermission(@NotNull String name) {
        permissions.remove(name.toLowerCase(java.util.Locale.ENGLISH));
    }

    @Override
    public void recalculatePermissionDefaults(@NotNull Permission perm) {
        if (perm != null && permissions.containsKey(perm.getName().toLowerCase(java.util.Locale.ENGLISH))) {
            defaultPerms.get(true).remove(perm);
            defaultPerms.get(false).remove(perm);

            calculatePermissionDefault(perm, true);
        }
    }

    private void calculatePermissionDefault(@NotNull Permission perm, boolean dirty) {
        if ((perm.getDefault() == PermissionDefault.OP) || (perm.getDefault() == PermissionDefault.TRUE)) {
            defaultPerms.get(true).add(perm);
            if (dirty) {
                dirtyPermissibles(true);
            }
        }
        if ((perm.getDefault() == PermissionDefault.NOT_OP) || (perm.getDefault() == PermissionDefault.TRUE)) {
            defaultPerms.get(false).add(perm);
            if (dirty) {
                dirtyPermissibles(false);
            }
        }
    }

    @Deprecated
    public void dirtyPermissibles() {
        dirtyPermissibles(true);
        dirtyPermissibles(false);
    }

    private void dirtyPermissibles(boolean op) {
        Set<Permissible> permissibles = getDefaultPermSubscriptions(op);

        for (Permissible p : permissibles) {
            p.recalculatePermissions();
        }
    }

    @Override
    public void subscribeToPermission(@NotNull String permission, @NotNull Permissible permissible) {
        String name = permission.toLowerCase(java.util.Locale.ENGLISH);
        Map<Permissible, Boolean> map = permSubs.get(name);

        if (map == null) {
            map = new WeakHashMap<Permissible, Boolean>();
            permSubs.put(name, map);
        }

        map.put(permissible, true);
    }

    @Override
    public void unsubscribeFromPermission(@NotNull String permission, @NotNull Permissible permissible) {
        String name = permission.toLowerCase(java.util.Locale.ENGLISH);
        Map<Permissible, Boolean> map = permSubs.get(name);

        if (map != null) {
            map.remove(permissible);

            if (map.isEmpty()) {
                permSubs.remove(name);
            }
        }
    }

    @Override
    @NotNull
    public Set<Permissible> getPermissionSubscriptions(@NotNull String permission) {
        String name = permission.toLowerCase(java.util.Locale.ENGLISH);
        Map<Permissible, Boolean> map = permSubs.get(name);

        if (map == null) {
            return ImmutableSet.of();
        } else {
            return ImmutableSet.copyOf(map.keySet());
        }
    }

    @Override
    public void subscribeToDefaultPerms(boolean op, @NotNull Permissible permissible) {
        Map<Permissible, Boolean> map = defSubs.get(op);

        if (map == null) {
            map = new WeakHashMap<Permissible, Boolean>();
            defSubs.put(op, map);
        }

        map.put(permissible, true);
    }

    @Override
    public void unsubscribeFromDefaultPerms(boolean op, @NotNull Permissible permissible) {
        Map<Permissible, Boolean> map = defSubs.get(op);

        if (map != null) {
            map.remove(permissible);

            if (map.isEmpty()) {
                defSubs.remove(op);
            }
        }
    }

    @Override
    @NotNull
    public Set<Permissible> getDefaultPermSubscriptions(boolean op) {
        Map<Permissible, Boolean> map = defSubs.get(op);

        if (map == null) {
            return ImmutableSet.of();
        } else {
            return ImmutableSet.copyOf(map.keySet());
        }
    }

    @Override
    @NotNull
    public Set<Permission> getPermissions() {
        return new HashSet<Permission>(permissions.values());
    }

    public boolean isTransitiveDepend(@NotNull ModuleDescriptionFile Module, @NotNull ModuleDescriptionFile depend) {
        Preconditions.checkArgument(Module != null, "Module");
        Preconditions.checkArgument(depend != null, "depend");

//        if (dependencyGraph.nodes().contains(Module.getName())) {
//            Set<String> reachableNodes = Graphs.reachableNodes(dependencyGraph, Module.getName());
//            if (reachableNodes.contains(depend.getName())) {
//                return true;
//            }
//            for (String provided : depend.getProvides()) {
//                if (reachableNodes.contains(provided)) {
//                    return true;
//                }
//            }
//        }
        return false;
    }

    @Override
    public boolean useTimings() {
        return useTimings;
    }

    /**
     * Sets whether or not per event timing code should be used
     *
     * @param use True if per event timing code should be used
     */
    public void useTimings(boolean use) {
        useTimings = use;
    }
}
