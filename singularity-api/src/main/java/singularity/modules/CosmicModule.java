package singularity.modules;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.command.ModuleCommand;
import singularity.events.modules.ModuleDisableEvent;
import singularity.events.modules.ModuleEnableEvent;
import singularity.utils.MessageUtils;
import org.pf4j.PluginWrapper;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Getter
public abstract class CosmicModule extends ModuleLike {
    private final File dataFolder;
    @Setter
    private boolean initialized;
    @Setter
    private boolean enabled;
    @Setter
    private boolean malleable = true;

    private ConcurrentSkipListSet<ModuleCommand> commands = new ConcurrentSkipListSet<>();

    public <C extends Collection<ModuleCommand>> void setCommands(C commands) {
        this.commands = new ConcurrentSkipListSet<>(commands);
    }

    public <C extends List<ModuleCommand>> void setCommands(C commands) {
        this.commands = new ConcurrentSkipListSet<>(commands);
    }

    /**
     * This allows you to set the {@link Module}'string identifier.
     *
     * @return The {@link Module}'string identifier;
     */
    public String getIdentifier() {
        return wrapper.getDescriptor().getPluginId();
    }

    /**
     * This allows you to set the {@link Module}'string authors.
     *
     * @return The {@link Module}'string authors;
     */
    public ConcurrentSkipListSet<String> authors() {
        return new ConcurrentSkipListSet<>(Arrays.stream(wrapper.getDescriptor().getProvider().replace(", ", ",").split(",")).collect(Collectors.toList()));
    }

    protected abstract void registerCommands();

    public CosmicModule(PluginWrapper wrapper) {
        super(wrapper);
        this.dataFolder = new File(Singularity.getModuleSaveFolder(), getIdentifier() + File.separator);
        ModuleManager.registerModule(this);
        onLoad();
    }

    @Override
    public void start() {
        if (isEnabled()) return;

        ModuleUtils.fireEvent(new ModuleEnableEvent(this));
        onEnable();
        setEnabled(true);
        ModuleManager.getEnabledModules().put(getIdentifier(), this);

        if (getCommands().isEmpty()) registerCommands();

        for (ModuleCommand command : this.getCommands()) {
            command.register();
        }
    }

    @Override
    public void stop() {
        if (! isEnabled()) return;

        for (ModuleCommand command : this.getCommands()) {
            try {
                ModuleUtils.logInfo(this, "Unregistering command: " + command.getIdentifier());
                command.unregister();
            } catch (Throwable e) {
                ModuleUtils.logWarning(this, "Failed to unregister command: " + command.getIdentifier());
                ModuleUtils.logWarning(this, e.getStackTrace());
            }
        }

        ModuleUtils.fireEvent(new ModuleDisableEvent(this));
        onDisable();
        setEnabled(false);
        ModuleManager.getEnabledModules().remove(getIdentifier());
    }

    public void restart() {
        stop();
        start();
    }

    public void unregister() {
        ModuleManager.unregisterModule(this);
    }

    public void register() {
        ModuleManager.registerModule(this);
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
            String author = this.authors().first();
            if (author == null) continue;

            builder.append(author);

            if (i < this.authors().size() - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    public void logInfo(String message) {
        MessageUtils.logInfo(this, message);
    }

    public void logWarning(String message) {
        MessageUtils.logWarning(this, message);
    }

    public void logSevere(String message) {
        MessageUtils.logSevere(this, message);
    }

    public void logDebug(StackTraceElement[] elements) {
        MessageUtils.logDebug(this, elements);
    }

    public void logInfo(StackTraceElement[] elements) {
        MessageUtils.logInfo(this, elements);
    }

    public void logWarning(StackTraceElement[] elements) {
        MessageUtils.logWarning(this, elements);
    }

    public void logSevere(StackTraceElement[] elements) {
        MessageUtils.logSevere(this, elements);
    }

    public void logDebug(String message) {
        MessageUtils.logDebug(this, message);
    }

    public InputStream getResourceAsStream(String filename) {
        return wrapper.getPluginClassLoader().getResourceAsStream(filename);
    }

    public boolean isRegisteredForSure() {
        return ModuleManager.getLoadedModules().containsValue(this);
    }

    public boolean isRegisteredByIdentifier() {
        return ModuleManager.getLoadedModules().containsKey(getIdentifier());
    }

    @Override
    public ModifierType getModifierType() {
        return ModifierType.STREAMLINE;
    }

    @Override
    public boolean isPlugin() {
        return false;
    }

    @Override
    public boolean isMod() {
        return false;
    }

    @Override
    public boolean isStreamline() {
        return true;
    }

    @Override
    public void initializeDataFolder() {
        if (! getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
    }

    public ModuleCommand getCommand(String identifier) {
        AtomicReference<ModuleCommand> command = new AtomicReference<>(null);

        getCommands().forEach(moduleCommand -> {
            if (moduleCommand.getIdentifier().equalsIgnoreCase(identifier)) {
                command.set(moduleCommand);
            }
        });

        return command.get();
    }

    public void addCommand(ModuleCommand command) {
        removeCommand(command.getIdentifier());

        getCommands().add(command);
    }

    public void removeCommand(String identifier) {
        getCommands().removeIf(moduleCommand -> moduleCommand.getIdentifier().equalsIgnoreCase(identifier));
    }

    public void removeCommand(ModuleCommand command) {
        removeCommand(command.getIdentifier());
    }
}
