package net.streamline.api.modules;

import lombok.Getter;
import net.streamline.api.command.ModuleCommand;
import net.streamline.api.events.modules.ModuleDisableEvent;
import net.streamline.api.events.modules.ModuleEnableEvent;
import net.streamline.api.modules.dependencies.Dependency;
import net.streamline.base.Streamline;
import net.streamline.utils.MessagingUtils;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public abstract class BundledModule {
    @Getter
    private static BundledModule instance;
    @Getter
    private final File dataFolder;
    @Getter
    private boolean initialized;
    @Getter
    private File moduleFile;
    @Getter
    private ClassLoader classLoader;

    /**
     * This allows you to set the {@link Module}'s identifier.
     *
     * @return The {@link Module}'s identifier;
     */
    public abstract String identifier();

    /**
     * This allows you to set the {@link Module}'s authors.
     *
     * @return The {@link Module}'s authors;
     */
    public abstract List<String> authors();

    /**
     * This allows you to set the {@link Module}'s dependencies.
     *
     * @return The {@link Module}'s dependencies;
     */
    public abstract List<Dependency> dependencies();

    /**
     * This allows you to set the {@link Module}'s commands.
     *
     * @return The {@link Module}'s commands;
     */
    public abstract List<ModuleCommand> commands();

    public BundledModule() {
        instance = this;
        this.dataFolder = new File(Streamline.getModuleFolder(), identifier() + File.separator);
        logInfo("Loaded!");
        onLoad();
    }

    public void start() {
        for (ModuleCommand command : this.commands()) {
            command.register();
        }

        ModuleUtils.fireEvent(new ModuleEnableEvent(this));
        onEnable();
    }

    public void stop() {
        for (ModuleCommand command : this.commands()) {
            command.unregister();
        }

        ModuleUtils.fireEvent(new ModuleDisableEvent(this));
        onDisable();
    }

    public void restart() {
        stop();
        start();
    }

    public void onLoad() {
        /*
        Nothing as of right now.
        This is so that the developer might
        be able to implement their own method.
         */
    }

    public abstract void onEnable();

    public abstract void onDisable();

    public String getAuthorsStringed() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < this.authors().size(); i ++) {
            String author = this.authors().get(i);
            if (author == null) continue;

            builder.append(author);

            if (i < this.authors().size() - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    public void logInfo(String message) {
        MessagingUtils.logInfo(this, message);
    }

    public void logWarning(String message) {
        MessagingUtils.logWarning(this, message);
    }

    public void logSevere(String message) {
        MessagingUtils.logSevere(this, message);
    }

    public final void initModule() {
        if (initialized)
            throw new RuntimeException("The module " + identifier() + " was already initialized.");

        initialized = true;

        if (! dataFolder.exists() && ! dataFolder.mkdirs())
            throw new RuntimeException("Cannot create module folder for " + identifier() + ".");

//        this.logger = new ModuleLogger(this);
//
//        if (moduleFile != null && classLoader != null)
//            this.moduleResources = new ModuleResources(this.moduleFile, this.moduleFolder, this.classLoader);
//
//        onPluginInit(plugin);
    }

    public final void initModuleLoader(File moduleFile, ClassLoader classLoader) {
        if (initialized)
            throw new RuntimeException("The module " + identifier() + " was already initialized.");

        this.moduleFile = moduleFile;
        this.classLoader = classLoader;
    }

    public InputStream getResourceAsStream(String filename) {
        return getClassLoader().getResourceAsStream(filename);
    }
}
