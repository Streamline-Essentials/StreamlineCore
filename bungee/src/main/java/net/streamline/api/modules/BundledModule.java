package net.streamline.api.modules;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.events.server.ModuleDisableEvent;
import net.streamline.api.events.server.ModuleEnableEvent;
import net.streamline.base.Streamline;
import net.streamline.utils.MessagingUtils;

import java.io.File;
import java.util.List;

public abstract class BundledModule {
    @Getter
    private final String identifier;
    @Getter
    private final List<String> authors;
    @Getter
    private final List<String> softDepends;
    @Getter
    private final List<String> hardDepends;
    @Getter
    private final File dataFolder;
    @Getter
    private boolean initialized;
    @Getter
    private File moduleFile;
    @Getter
    private ClassLoader classLoader;

    public BundledModule(String identifier, List<String> authors, List<String> softDepends, List<String> hardDepends) {
        this.identifier = identifier;
        this.authors = authors;
        this.softDepends = softDepends;
        this.hardDepends = hardDepends;
        this.dataFolder = new File(Streamline.getModuleFolder(), identifier + File.separator);
        logInfo("Loaded!");
        onLoad();
    }

    public void start() {
        Streamline.fireEvent(new ModuleEnableEvent(this));
        onEnable();
    }

    public void stop() {
        Streamline.fireEvent(new ModuleDisableEvent(this));
        onDisable();
    }

    protected abstract void onLoad();

    protected abstract void onEnable();

    protected abstract void onDisable();

    public String getAuthorsStringed() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < this.authors.size(); i ++) {
            String author = this.authors.get(i);
            if (author == null) continue;

            builder.append(author);

            if (i < this.authors.size() - 1) {
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
            throw new RuntimeException("The module " + identifier + " was already initialized.");

        initialized = true;

        if (! dataFolder.exists() && ! dataFolder.mkdirs())
            throw new RuntimeException("Cannot create module folder for " + identifier + ".");

//        this.logger = new ModuleLogger(this);
//
//        if (moduleFile != null && classLoader != null)
//            this.moduleResources = new ModuleResources(this.moduleFile, this.moduleFolder, this.classLoader);
//
//        onPluginInit(plugin);
    }

    public final void initModuleLoader(File moduleFile, ClassLoader classLoader) {
        if (initialized)
            throw new RuntimeException("The module " + identifier + " was already initialized.");

        this.moduleFile = moduleFile;
        this.classLoader = classLoader;
    }
}
