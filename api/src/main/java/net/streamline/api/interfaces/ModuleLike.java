package net.streamline.api.interfaces;

import tv.quaint.objects.handling.IEventable;

import java.io.File;
import java.io.InputStream;

public interface ModuleLike extends IEventable {
    String identifier();

    String getAuthorsStringed();

    void logInfo(String message);

    void logWarning(String message);

    void logSevere(String message);

    boolean isEnabled();

    void start();

    void stop();

    void restart();

    File getDataFolder();

    InputStream getResourceAsStream(String fileName);
}
