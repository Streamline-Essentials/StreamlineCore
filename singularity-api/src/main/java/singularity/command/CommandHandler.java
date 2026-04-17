package singularity.command;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.interfaces.IProperCommand;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Central registry for all cross-platform commands in Streamline.
 *
 * <p>Maintains three sorted maps:</p>
 * <ul>
 *   <li>{@link #loadedModuleCommands} — commands contributed by PF4J modules.</li>
 *   <li>{@link #loadedStreamlineCommands} — built-in Streamline commands.</li>
 *   <li>{@link #properlyRegisteredCommands} — platform-wrapped {@link IProperCommand}
 *       instances that are actively registered with the server platform.</li>
 * </ul>
 */
public class CommandHandler {

    /**
     * All {@link ModuleCommand} instances contributed by loaded PF4J modules,
     * keyed by command base name.
     */
    @Getter @Setter
    private static ConcurrentSkipListMap<String, ModuleCommand> loadedModuleCommands = new ConcurrentSkipListMap<>();

    /**
     * All built-in {@link CosmicCommand} instances registered with Streamline,
     * keyed by command base name.
     */
    @Getter @Setter
    private static ConcurrentSkipListMap<String, CosmicCommand> loadedStreamlineCommands = new ConcurrentSkipListMap<>();

    /**
     * The platform-specific {@link IProperCommand} wrappers that are actively
     * registered with the server platform, keyed by command identifier.
     */
    @Getter @Setter
    private static ConcurrentSkipListMap<String, IProperCommand> properlyRegisteredCommands = new ConcurrentSkipListMap<>();

    /**
     * Registers a {@link CosmicCommand} with the platform, replacing any existing
     * registration for the same identifier. The platform wrapper is created via
     * {@link singularity.interfaces.ISingularityExtension#createCommand} and then
     * registered immediately.
     *
     * @param command the command to register
     */
    private static void registerCommandRaw(CosmicCommand command) {
        if (isProperCommandRegistered(command.getIdentifier())) {
            unregisterCommandRaw(command.getIdentifier());
        }

        IProperCommand properCommand = Singularity.getInstance().getPlatform().createCommand(command);
        properCommand.registerThis();
        getProperlyRegisteredCommands().put(command.getIdentifier(), properCommand);
        getLoadedStreamlineCommands().put(command.getBase(), command);
    }

    /**
     * Unregisters the platform command for the given identifier, removing it from
     * both the properly-registered and loaded-streamline-commands maps.
     *
     * @param identifier the command identifier to unregister
     */
    private static void unregisterCommandRaw(String identifier) {
        IProperCommand c = getProperlyRegisteredCommands().get(identifier);
        if (c == null) return;
        c.unregisterThis();
        getProperlyRegisteredCommands().remove(identifier);
        getLoadedStreamlineCommands().remove(identifier);
    }

    /**
     * Registers a built-in Streamline command with the platform.
     *
     * @param command the command to register
     */
    public static void registerStreamlineCommand(CosmicCommand command) {
        registerCommandRaw(command);
    }

    /**
     * Unregisters a built-in Streamline command from the platform.
     *
     * @param command the command to unregister
     */
    public static void unregisterStreamlineCommand(CosmicCommand command) {
        unregisterCommandRaw(command.getIdentifier());
    }

    /**
     * Registers a module-contributed command with the platform and adds it to the
     * loaded module commands map.
     *
     * @param command the module command to register
     */
    public static void registerModuleCommand(ModuleCommand command) {
        registerCommandRaw(command);
        getLoadedModuleCommands().put(command.getBase(), command);
    }

    /**
     * Unregisters a module-contributed command from the platform and removes it from
     * the loaded module commands map.
     *
     * @param command the module command to unregister
     */
    public static void unregisterModuleCommand(ModuleCommand command) {
        unregisterCommandRaw(command.getIdentifier());
        getLoadedModuleCommands().remove(command.getIdentifier());
    }

    /**
     * Returns the platform-level {@link IProperCommand} for the given identifier, or
     * {@code null} if no such command is registered.
     *
     * @param identifier the command identifier to look up
     * @return the registered {@link IProperCommand}, or {@code null}
     */
    public static IProperCommand getProperCommand(String identifier) {
        return getProperlyRegisteredCommands().get(identifier);
    }

    /**
     * Returns the built-in {@link CosmicCommand} for the given identifier, or
     * {@code null} if no such command is loaded.
     *
     * @param identifier the command identifier (typically the base name) to look up
     * @return the matching {@link CosmicCommand}, or {@code null}
     */
    public static CosmicCommand getStreamlineCommand(String identifier) {
        return getLoadedStreamlineCommands().get(identifier);
    }

    /**
     * Returns the {@link ModuleCommand} for the given identifier, or {@code null} if no
     * module command with that identifier is loaded.
     *
     * @param identifier the command identifier to look up
     * @return the matching {@link ModuleCommand}, or {@code null}
     */
    public static ModuleCommand getModuleCommand(String identifier) {
        return getLoadedModuleCommands().get(identifier);
    }

    /**
     * Returns {@code true} if a platform-level command with the given identifier is
     * currently registered.
     *
     * @param identifier the command identifier to check
     * @return {@code true} if registered; {@code false} otherwise
     */
    public static boolean isProperCommandRegistered(String identifier) {
        return getProperlyRegisteredCommands().containsKey(identifier);
    }

    /**
     * Returns {@code true} if a built-in Streamline command with the given identifier
     * is currently loaded.
     *
     * @param identifier the command identifier to check
     * @return {@code true} if loaded; {@code false} otherwise
     */
    public static boolean isStreamlineCommandRegistered(String identifier) {
        return getLoadedStreamlineCommands().containsKey(identifier);
    }

    /**
     * Returns {@code true} if a module command with the given identifier is currently loaded.
     *
     * @param identifier the command identifier to check
     * @return {@code true} if loaded; {@code false} otherwise
     */
    public static boolean isModuleCommandRegistered(String identifier) {
        return getLoadedModuleCommands().containsKey(identifier);
    }

    /**
     * Unregisters every currently registered platform command. Intended for use
     * during server shutdown or full reload to cleanly remove all commands.
     */
    public static void flushProperCommands() {
        getProperlyRegisteredCommands().forEach((s, iProperCommand) -> {
            iProperCommand.unregisterThis();
        });
    }

    /**
     * Collects every registered alias and base name from both the Streamline command
     * map and the module command map into a single sorted set.
     *
     * @return a sorted set containing all known command aliases and base names
     */
    public static ConcurrentSkipListSet<String> getAllAliases() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getLoadedStreamlineCommands().forEach((s, command) -> {
            r.addAll(Arrays.asList(command.getAliases()));

            r.add(command.getBase());
        });

        getLoadedModuleCommands().forEach((s, command) -> {
            r.addAll(Arrays.asList(command.getAliases()));

            r.add(command.getBase());
        });

        return r;
    }

    /**
     * Finds the {@link CosmicCommand} that owns the given alias. Searches built-in
     * Streamline commands first, then module commands. The comparison is
     * case-insensitive for alias entries but case-sensitive for the base-name map
     * lookup.
     *
     * @param alias the alias or base name to search for
     * @return the matching {@link CosmicCommand}, or {@code null} if not found
     */
    public static CosmicCommand getCommandByAlias(String alias) {
        CosmicCommand command = getStreamlineCommand(alias);
        if (command != null) return command;

        AtomicReference<CosmicCommand> commandRef = new AtomicReference<>(null);

        getLoadedStreamlineCommands().forEach((s, c) -> {
            if (commandRef.get() != null) return;

            for (String a : c.getAliases()) {
                if (a.equalsIgnoreCase(alias)) {
                    commandRef.set(c);
                    break;
                }
            }
        });

        if (commandRef.get() != null) return commandRef.get();

        getLoadedModuleCommands().forEach((s, c) -> {
            if (commandRef.get() != null) return;

            for (String a : c.getAliases()) {
                if (a.equalsIgnoreCase(alias)) {
                    commandRef.set(c);
                    break;
                }
            }
        });

        return commandRef.get();
    }
}
