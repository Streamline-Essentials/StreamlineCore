package net.streamline.api.interfaces;

import tv.quaint.objects.handling.IEventable;

import java.io.File;
import java.io.InputStream;

public interface ModuleLike extends IEventable, Comparable<ModuleLike> {
    String getIdentifier();

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

    boolean isMalleable();
}
