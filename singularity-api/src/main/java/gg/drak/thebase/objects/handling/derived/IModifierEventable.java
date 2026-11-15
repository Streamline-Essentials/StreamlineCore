package gg.drak.thebase.objects.handling.derived;

import gg.drak.thebase.objects.handling.IEventable;

import java.io.File;

public interface IModifierEventable extends IEventable {
    enum ModifierType {
        PLUGIN,
        MOD,
        STREAMLINE,
        OTHER,
        ;
    }

    ModifierType getModifierType();

    File getDataFolder();

    public default boolean isPlugin() {
        return getModifierType().equals(ModifierType.PLUGIN);
    }

    public default boolean isMod() {
        return getModifierType().equals(ModifierType.MOD);
    }

    public default boolean isStreamline() {
        return getModifierType().equals(ModifierType.STREAMLINE);
    }

    public default boolean isOther() {
        return getModifierType().equals(ModifierType.OTHER);
    }

    public default void initializeDataFolder() {
        getDataFolder().mkdirs();
    }
}
