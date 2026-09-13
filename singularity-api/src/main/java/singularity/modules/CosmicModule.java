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

/**
 * Base class for all Streamline modules loaded via PF4J.
 *
 * <p>A {@code CosmicModule} extends {@link ModuleLike} (which itself wraps
 * {@link org.pf4j.Plugin}) and adds the full Streamline lifecycle:
 * {@link #onLoad()}, {@link #onEnable()}, {@link #onDisable()}, command
 * registration, and data-folder management.
 *
 * <p>Concrete modules must implement {@link #onEnable()}, {@link #onDisable()},
 * and {@link #registerCommands()}.  The {@link #onLoad()} hook is optional and
 * defaults to a no-op.
 */
@Getter
public abstract class CosmicModule extends ModuleLike {

    /** Folder dedicated to this module's persistent configuration and data files. */
    private final File dataFolder;

    /**
     * Whether this module has completed its initialisation sequence.
     * Set to {@code true} by the framework after the module is fully set up.
     */
    @Setter
    private boolean initialized;

    /**
     * Whether this module is currently in the enabled state.
     * Toggled by {@link #start()} and {@link #stop()}.
     */
    @Setter
    private boolean enabled;

    /**
     * Whether this module can be replaced by another module with the same
     * identifier.  Immalleable ({@code false}) modules block duplicate
     * registrations with a warning.
     */
    @Setter
    private boolean malleable = true;

    /**
     * The set of {@link ModuleCommand} instances registered by this module.
     * Commands are registered during {@link #start()} if the set is empty.
     */
    private ConcurrentSkipListSet<ModuleCommand> commands = new ConcurrentSkipListSet<>();

    /**
     * Replaces this module's command set with the elements of the given
     * {@link Collection}.
     *
     * @param <C>      a collection type that holds {@link ModuleCommand} instances
     * @param commands the new command collection; must not be {@code null}
     */
    public <C extends Collection<ModuleCommand>> void setCommands(C commands) {
        this.commands = new ConcurrentSkipListSet<>(commands);
    }

    /**
     * Replaces this module's command set with the elements of the given
     * {@link List}.
     *
     * @param <C>      a list type that holds {@link ModuleCommand} instances
     * @param commands the new command list; must not be {@code null}
     */
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

    /**
     * Called by the framework during {@link #start()} when no commands have
     * been registered yet.  Implementations should call
     * {@link #addCommand(ModuleCommand)} for each command they wish to expose.
     */
    protected abstract void registerCommands();

    /**
     * Constructs a new module, determines its data folder, registers it with
     * the {@link ModuleManager}, and invokes {@link #onLoad()}.
     *
     * @param wrapper the PF4J plugin wrapper provided by the plugin manager
     */
    public CosmicModule(PluginWrapper wrapper) {
        super(wrapper);
        this.dataFolder = new File(Singularity.getModuleSaveFolder(), getIdentifier() + File.separator);
        ModuleManager.registerModule(this);
        onLoad();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Fires a {@link ModuleEnableEvent}, calls {@link #onEnable()}, marks
     * the module as enabled, registers it in the enabled-modules map, and
     * registers (and activates) all commands.  Does nothing if the module is
     * already enabled.
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Unregisters all commands (logging warnings for failures), fires a
     * {@link ModuleDisableEvent}, calls {@link #onDisable()}, marks the module
     * as disabled, and removes it from the enabled-modules map.  Does nothing
     * if the module is already disabled.
     */
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

    /**
     * Stops, then immediately starts this module, effectively reloading it
     * without unloading the JAR.
     */
    public void restart() {
        stop();
        start();
    }

    /**
     * Removes this module from the {@link ModuleManager}'s loaded-module
     * registry.
     */
    public void unregister() {
        ModuleManager.unregisterModule(this);
    }

    /**
     * Re-registers this module with the {@link ModuleManager}.
     */
    public void register() {
        ModuleManager.registerModule(this);
    }

    /**
     * Invoked immediately after the module is constructed, before
     * {@link #onEnable()} is called.  The default implementation is a no-op;
     * subclasses may override to perform early initialisation.
     */
    public void onLoad() {
        /*
        Nothing as of right now.
        This is so that the developer might
        be able to implement their own method.
         */
    }

    /**
     * Called when this module is enabled.  Subclasses must implement all
     * setup logic here (registering listeners, loading config, etc.).
     */
    public abstract void onEnable();

    /**
     * Called when this module is disabled.  Subclasses must implement all
     * teardown logic here (releasing resources, saving state, etc.).
     */
    public abstract void onDisable();

    /**
     * Returns the authors of this module as a comma-separated string.
     *
     * @return formatted author list, e.g. {@code "Alice, Bob"}
     */
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

    /**
     * Logs a message at the INFO level, prefixed with this module's identifier.
     *
     * @param message the message to log
     */
    public void logInfo(String message) {
        MessageUtils.logInfo(this, message);
    }

    /**
     * Logs a message at the WARNING level, prefixed with this module's
     * identifier.
     *
     * @param message the message to log
     */
    public void logWarning(String message) {
        MessageUtils.logWarning(this, message);
    }

    /**
     * Logs a message at the SEVERE level, prefixed with this module's
     * identifier.
     *
     * @param message the message to log
     */
    public void logSevere(String message) {
        MessageUtils.logSevere(this, message);
    }

    /**
     * Logs a stack trace at the DEBUG level, prefixed with this module's
     * identifier.
     *
     * @param elements the stack trace elements to log
     */
    public void logDebug(StackTraceElement[] elements) {
        MessageUtils.logDebug(this, elements);
    }

    /**
     * Logs a stack trace at the INFO level, prefixed with this module's
     * identifier.
     *
     * @param elements the stack trace elements to log
     */
    public void logInfo(StackTraceElement[] elements) {
        MessageUtils.logInfo(this, elements);
    }

    /**
     * Logs a stack trace at the WARNING level, prefixed with this module's
     * identifier.
     *
     * @param elements the stack trace elements to log
     */
    public void logWarning(StackTraceElement[] elements) {
        MessageUtils.logWarning(this, elements);
    }

    /**
     * Logs a stack trace at the SEVERE level, prefixed with this module's
     * identifier.
     *
     * @param elements the stack trace elements to log
     */
    public void logSevere(StackTraceElement[] elements) {
        MessageUtils.logSevere(this, elements);
    }

    /**
     * Logs a message at the DEBUG level, prefixed with this module's
     * identifier.
     *
     * @param message the message to log
     */
    public void logDebug(String message) {
        MessageUtils.logDebug(this, message);
    }

    /**
     * Returns an {@link InputStream} for a resource bundled inside this
     * module's JAR file.
     *
     * @param filename the resource path relative to the JAR root
     * @return the resource stream, or {@code null} if not found
     */
    public InputStream getResourceAsStream(String filename) {
        return wrapper.getPluginClassLoader().getResourceAsStream(filename);
    }

    /**
     * Returns {@code true} if this module instance is present in the
     * {@link ModuleManager}'s loaded-module value set.
     *
     * @return {@code true} when this exact instance is registered
     */
    public boolean isRegisteredForSure() {
        return ModuleManager.getLoadedModules().containsValue(this);
    }

    /**
     * Returns {@code true} if a module with this module's identifier is
     * present in the {@link ModuleManager}'s loaded-module key set.
     *
     * @return {@code true} when any module with the same identifier is loaded
     */
    public boolean isRegisteredByIdentifier() {
        return ModuleManager.getLoadedModules().containsKey(getIdentifier());
    }

    /** {@inheritDoc} */
    @Override
    public ModifierType getModifierType() {
        return ModifierType.STREAMLINE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPlugin() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMod() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isStreamline() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates the module's data folder (and any missing parent directories)
     * if it does not already exist.
     */
    @Override
    public void initializeDataFolder() {
        if (! getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
    }

    /**
     * Finds and returns the {@link ModuleCommand} registered under the given
     * identifier (case-insensitive), or {@code null} if none exists.
     *
     * @param identifier the command identifier to search for
     * @return the matching command, or {@code null}
     */
    public ModuleCommand getCommand(String identifier) {
        AtomicReference<ModuleCommand> command = new AtomicReference<>(null);

        getCommands().forEach(moduleCommand -> {
            if (moduleCommand.getIdentifier().equalsIgnoreCase(identifier)) {
                command.set(moduleCommand);
            }
        });

        return command.get();
    }

    /**
     * Adds a command to this module's command set, first removing any
     * existing command with the same identifier.
     *
     * @param command the command to add; must not be {@code null}
     */
    public void addCommand(ModuleCommand command) {
        removeCommand(command.getIdentifier());

        getCommands().add(command);
    }

    /**
     * Removes the command with the given identifier (case-insensitive) from
     * this module's command set.
     *
     * @param identifier the identifier of the command to remove
     */
    public void removeCommand(String identifier) {
        getCommands().removeIf(moduleCommand -> moduleCommand.getIdentifier().equalsIgnoreCase(identifier));
    }

    /**
     * Removes the given command from this module's command set by identifier.
     *
     * @param command the command whose identifier is used for removal
     */
    public void removeCommand(ModuleCommand command) {
        removeCommand(command.getIdentifier());
    }
}
