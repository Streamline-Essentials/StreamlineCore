package net.streamline.api.base.modules;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ModuleLogger extends Logger {
    private String moduleName;
    public ModuleLogger(Module context) {
        super(context.getClass().getCanonicalName(), null);
        String prefix = context.getDescription().getPrefix();
        moduleName = prefix != null ? new StringBuilder().append("[").append(prefix).append("] ").toString() : "[" + context.getDescription().getName() + "] ";
        setParent(context.getBase().getLogger());
        setLevel(Level.ALL);
    }
    @Override
    public void log(LogRecord logRecord) {
        logRecord.setMessage(moduleName + logRecord.getMessage());
        super.log(logRecord);
    }
}
