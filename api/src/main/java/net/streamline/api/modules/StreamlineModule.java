package net.streamline.api.modules;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.command.ModuleCommand;
import net.streamline.api.events.modules.ModuleDisableEvent;
import net.streamline.api.events.modules.ModuleEnableEvent;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;
import tv.quaint.thebase.lib.pf4j.Plugin;
import tv.quaint.thebase.lib.pf4j.PluginWrapper;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class StreamlineModule extends Plugin implements ModuleLike {
    @Getter
    private final File dataFolder;
    @Getter @Setter
    private boolean initialized;
    @Getter @Setter
    private boolean enabled;
    @Getter @Setter
    private boolean malleable = true;

    @Getter @Setter
    private List<ModuleCommand> commands = new ArrayList<>();

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
        return new ConcurrentSkipListSet<>(Arrays.stream(wrapper.getDescriptor().getProvider().replace(", ", ",").split(",")).toList());
    }

    protected abstract void registerCommands();

    public StreamlineModule(PluginWrapper wrapper) {
        super(wrapper);
        this.dataFolder = new File(SLAPI.getModuleSaveFolder(), getIdentifier() + File.separator);
        ModuleManager.registerModule(this);
        onLoad();
    }

    @Override
    public void start() {
        if (isEnabled()) return;
        if (getCommands().isEmpty()) registerCommands();

        for (ModuleCommand command : this.getCommands()) {
            command.register();
        }

        ModuleUtils.fireEvent(new ModuleEnableEvent(this));
        onEnable();
        setEnabled(true);
        ModuleManager.getEnabledModules().put(getIdentifier(), this);
    }

    @Override
    public void stop() {
        if (! isEnabled()) return;

        for (ModuleCommand command : this.getCommands()) {
            ModuleUtils.logInfo(this, "Unregistering command: " + command.getIdentifier());
            command.unregister();
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
    public int compareTo(@NotNull ModuleLike o) {
        return CharSequence.compare(getIdentifier(), o.getIdentifier());
    }
}
