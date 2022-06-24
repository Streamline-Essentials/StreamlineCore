package net.streamline.api.modules;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * The ModuleLogger class is a modified {@link Logger} that prepends all
 * logging calls with the name of the module doing the logging. The API for
 * ModuleLogger is exactly the same as {@link Logger}.
 *
 * @see Logger
 */
public class ModuleLogger extends Logger {
    private String moduleName;

    /**
     * Creates a new ModuleLogger that extracts the name from a module.
     *
     * @param context A reference to the module
     */
    public ModuleLogger(@NotNull Module context) {
        super(context.getClass().getCanonicalName(), null);
        String prefix = context.getDescription().getPrefix();
        moduleName = prefix != null ? new StringBuilder().append("[").append(prefix).append("] ").toString() : "[" + context.getDescription().getName() + "] ";
        setParent(context.getBase().getLogger());
        setLevel(Level.ALL);
    }

    @Override
    public void log(@NotNull LogRecord logRecord) {
        logRecord.setMessage(moduleName + logRecord.getMessage());
        super.log(logRecord);
    }

}
