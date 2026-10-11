package net.streamline.platform.modules;

import net.streamline.platform.BasePlugin;
import org.pf4j.PluginWrapper;
import singularity.client.module.WorldModules;
import singularity.modules.ModuleManager;
import singularity.registry.ModuleRegistry;
import singularity.utils.MessageUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Loads and unloads a game client's world modules through {@link ModuleManager} once
 * Streamline runs for a hosted world, so the client's module menu and
 * {@code /slclient modules} take effect without leaving the world. Module changes run on the
 * server thread, like the server's own {@code /modules ecloud}.
 */
public final class WorldModuleHost implements WorldModules.Host {

    private final BasePlugin plugin;

    public WorldModuleHost(BasePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isRunning() {
        return plugin.isRunning();
    }

    @Override
    public boolean isLoaded(Path jar) {
        return ModuleManager.getPluginWrapperByJarName(jar.getFileName().toString()) != null;
    }

    @Override
    public CompletableFuture<String> install(Path oldJar, Path temp, Path target) {
        return onMainThread(() -> {
            List<String> dependents = new ArrayList<>();
            if (oldJar != null) {
                PluginWrapper wrapper = ModuleManager.getPluginWrapperByJarName(oldJar.getFileName().toString());
                if (wrapper != null) dependents = ModuleManager.getDependentJarNames(wrapper.getPluginId());
                ModuleManager.unloadModuleJar(oldJar);
                if (! oldJar.equals(target)) {
                    try {
                        Files.deleteIfExists(oldJar);
                    } catch (IOException e) {
                        // Windows refuses to delete a jar while any handle to it is still open.
                        oldJar.toFile().deleteOnExit();
                    }
                }
            }

            try {
                ModuleRegistry.move(temp, target);
            } catch (IOException e) {
                throw new IllegalStateException("Could not save " + target.getFileName() + ": " + e.getMessage(), e);
            }

            String refusal;
            try {
                refusal = ModuleManager.tryRegisterExternalModule(target.getFileName().toString());
            } catch (Throwable e) {
                refusal = "'" + target.getFileName() + "' did not load: " + e.getMessage();
            }

            for (String dependent : dependents) {
                if (ModuleManager.getPluginWrapperByJarName(dependent) != null) continue;
                try {
                    ModuleManager.registerExternalModule(dependent);
                } catch (Throwable e) {
                    MessageUtils.logWarning("Could not load '" + dependent + "' again: " + e.getMessage());
                }
            }
            return refusal;
        });
    }

    @Override
    public CompletableFuture<Void> remove(Path jar) {
        return onMainThread(() -> {
            ModuleManager.deleteModule(jar.getFileName().toString());
            return null;
        });
    }

    private <T> CompletableFuture<T> onMainThread(Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        plugin.runOnMainThread(() -> {
            try {
                future.complete(task.get());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
