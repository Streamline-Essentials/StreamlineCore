package gg.drak.thebase.storage.managers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import gg.drak.thebase.objects.Classifiable;
import gg.drak.thebase.storage.resources.StorageResource;

import java.io.File;

public interface IStorageManager<T extends StorageResource<?>> extends Classifiable<T> {
    /**
     * Gets the directory associated with these resources.
     * @return The directory associated with these resources.
     */
    File getParentDirectory();

    /**
     * Checks if the resource identified by the identifier is loaded already.
     * @param identifier The resource identifier to check.
     * @return whether it is already loaded.
     */
    boolean isAlreadyLoaded(String identifier);

    /**
     * Loads the resource into memory.
     * @param resource The resource to load into memory.
     */
    void load(T resource);

    /**
     * Instantiates a resource identified by the identifier and loads it into memory.
     * @param identifier The resource identifier.
     */
    void instantiateAndLoad(String identifier);

    /**
     * Instantiates a resource identified by the identifier.
     * @param identifier The resource identifier.
     */
    @NotNull
    T instantiate(String identifier);

    /**
     * Unloads the resource identified by the given identifier.
     * @param identifier The resource identifier.
     */
    void unload(String identifier);

    /**
     * Gets a resource from memory by the identifier.
      * @param identifier The resource identifier.
     * @return The resource identified by the identifier.
     */
    @Nullable
    T get(String identifier);

    /**
     * Gets a resource from memory or instantiates and loads a new one identified by the given identifier.
     * @param identifier The resource identifier.
     * @return The resource identified by the identifier.
     */
    @NotNull
    T getOrLoad(String identifier);
}
