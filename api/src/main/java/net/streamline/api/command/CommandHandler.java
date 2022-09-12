package net.streamline.api.command;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.interfaces.IProperCommand;

import java.util.concurrent.ConcurrentSkipListMap;

public class CommandHandler {
    @Getter @Setter
    private static ConcurrentSkipListMap<String, ModuleCommand> loadedModuleCommands = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static ConcurrentSkipListMap<String, StreamlineCommand> loadedStreamlineCommands = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static ConcurrentSkipListMap<String, IProperCommand> properlyRegisteredCommands = new ConcurrentSkipListMap<>();

    private static void registerCommandRaw(StreamlineCommand command) {
        IProperCommand properCommand = SLAPI.getInstance().getPlatform().createCommand(command);
        properCommand.register();
        getProperlyRegisteredCommands().put(command.getIdentifier(), properCommand);
        getLoadedStreamlineCommands().put(command.getBase(), command);
    }

    private static void unregisterCommandRaw(String identifier) {
        IProperCommand c = getProperlyRegisteredCommands().get(identifier);
        if (c == null) return;
        c.unregister();
        getProperlyRegisteredCommands().remove(identifier);
        getLoadedStreamlineCommands().remove(identifier);
    }

    public static void registerStreamlineCommand(StreamlineCommand command) {
        if (isProperCommandRegistered(command.getIdentifier())) {
            SLAPI.getInstance().getMessenger().logWarning("Command with identifier '" + command.getIdentifier() + "' is already registered!");
            return;
        }

        registerCommandRaw(command);
    }

    public static void unregisterStreamlineCommand(StreamlineCommand command) {
        unregisterCommandRaw(command.getIdentifier());
    }

    public static void registerModuleCommand(ModuleCommand command) {
        if (isProperCommandRegistered(command.getIdentifier())) {
            SLAPI.getInstance().getMessenger().logWarning(command.getOwningModule(), "Command with identifier '" + command.getIdentifier() + "' is already registered!");
            return;
        }
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

    public static StreamlineCommand getStreamlineCommand(String identifier) {
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
            iProperCommand.unregister();
        });
    }
}
