package net.streamline.api.base;

import net.streamline.api.base.modules.InvalidModuleException;
import net.streamline.api.base.modules.ModuleDescriptionFile;

/**
 * This interface provides value conversions that may be specific to a
 * runtime, or have arbitrary meaning (read: magic values).
 * <p>
 * Their existence and behavior is not guaranteed across future versions. They
 * may be poorly named, throw exceptions, have misleading parameters, or any
 * other bad programming practice.
 */
@Deprecated
public interface UnsafeValues {
    int getDataVersion();

    void checkSupported(ModuleDescriptionFile pdf) throws InvalidModuleException;

    byte[] processClass(ModuleDescriptionFile pdf, String path, byte[] clazz);
}
