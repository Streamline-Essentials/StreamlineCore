package net.streamline.api.interfaces;

import net.streamline.api.command.ModuleCommand;
import tv.quaint.objects.handling.derived.IModifierEventable;

import java.io.File;
import java.io.InputStream;

public interface IModuleLike extends IModifierEventable, Comparable<IModuleLike> {
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

    void setMalleable(boolean malleable);

    void addCommand(ModuleCommand command);

    void removeCommand(ModuleCommand command);
}
