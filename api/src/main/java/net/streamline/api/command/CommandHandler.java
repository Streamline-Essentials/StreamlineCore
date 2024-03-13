package net.streamline.api.command;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.interfaces.IProperCommand;
import net.streamline.api.utils.MessageUtils;

import java.util.concurrent.ConcurrentSkipListMap;

public class CommandHandler {
    @Getter @Setter
    private static ConcurrentSkipListMap<String, ModuleCommand> loadedModuleCommands = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static ConcurrentSkipListMap<String, StreamlineCommand> loadedStreamlineCommands = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static ConcurrentSkipListMap<String, IProperCommand> properlyRegisteredCommands = new ConcurrentSkipListMap<>();

    private static void registerCommandRaw(StreamlineCommand command) {
        if (isProperCommandRegistered(command.getIdentifier())) {
            unregisterCommandRaw(command.getIdentifier());
        }

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
        registerCommandRaw(command);
    }

    public static void unregisterStreamlineCommand(StreamlineCommand command) {
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
