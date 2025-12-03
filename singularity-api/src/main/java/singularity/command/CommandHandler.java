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

public class CommandHandler {
    @Getter @Setter
    private static ConcurrentSkipListMap<String, ModuleCommand> loadedModuleCommands = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static ConcurrentSkipListMap<String, CosmicCommand> loadedStreamlineCommands = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static ConcurrentSkipListMap<String, IProperCommand> properlyRegisteredCommands = new ConcurrentSkipListMap<>();

    private static void registerCommandRaw(CosmicCommand command) {
        if (isProperCommandRegistered(command.getIdentifier())) {
            unregisterCommandRaw(command.getIdentifier());
        }

        IProperCommand properCommand = Singularity.getInstance().getPlatform().createCommand(command);
        properCommand.registerThis();
        getProperlyRegisteredCommands().put(command.getIdentifier(), properCommand);
        getLoadedStreamlineCommands().put(command.getBase(), command);
    }

    private static void unregisterCommandRaw(String identifier) {
        IProperCommand c = getProperlyRegisteredCommands().get(identifier);
        if (c == null) return;
        c.unregisterThis();
        getProperlyRegisteredCommands().remove(identifier);
        getLoadedStreamlineCommands().remove(identifier);
    }

    public static void registerStreamlineCommand(CosmicCommand command) {
        registerCommandRaw(command);
    }

    public static void unregisterStreamlineCommand(CosmicCommand command) {
        unregisterCommandRaw(command.getIdentifier());
    }

    public static void registerModuleCommand(ModuleCommand command) {
        registerCommandRaw(command);
        getLoadedModuleCommands().put(command.getBase(), command);
    }

    public static void unregisterModuleCommand(ModuleCommand command) {
        unregisterCommandRaw(command.getIdentifier());
        getLoadedModuleCommands().remove(command.getIdentifier());
    }

    public static IProperCommand getProperCommand(String identifier) {
        return getProperlyRegisteredCommands().get(identifier);
    }

    public static CosmicCommand getStreamlineCommand(String identifier) {
        return getLoadedStreamlineCommands().get(identifier);
    }

    public static ModuleCommand getModuleCommand(String identifier) {
        return getLoadedModuleCommands().get(identifier);
    }

    public static boolean isProperCommandRegistered(String identifier) {
        return getProperlyRegisteredCommands().containsKey(identifier);
    }

    public static boolean isStreamlineCommandRegistered(String identifier) {
        return getLoadedStreamlineCommands().containsKey(identifier);
    }

    public static boolean isModuleCommandRegistered(String identifier) {
        return getLoadedModuleCommands().containsKey(identifier);
    }

    public static void flushProperCommands() {
        getProperlyRegisteredCommands().forEach((s, iProperCommand) -> {
            iProperCommand.unregisterThis();
        });
    }

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
