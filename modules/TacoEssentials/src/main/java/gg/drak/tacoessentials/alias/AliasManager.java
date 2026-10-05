package gg.drak.tacoessentials.alias;

import gg.drak.tacoessentials.data.TacoDatabase;
import singularity.command.CommandHandler;
import singularity.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * The custom aliases, as stored in {@code taco_aliases} / {@code taco_alias_commands}, and the
 * command registered for each.
 */
public final class AliasManager {

    /** The editor's own label, which an alias may never take. */
    public static final String EDITOR_COMMAND = "aliaseditor";

    private static final Map<String, CustomAlias> ALIASES = new ConcurrentSkipListMap<>();
    private static final Map<String, AliasCommand> COMMANDS = new ConcurrentSkipListMap<>();

    private AliasManager() {}

    /**
     * Loads every alias from the database and registers its command. An alias whose name
     * another command took in the meantime is kept, so it can be edited or deleted, but not
     * registered.
     */
    public static void load() {
        unloadAll();
        ALIASES.putAll(TacoDatabase.aliases());
        for (CustomAlias alias : ALIASES.values()) {
            if (isTaken(alias.getName())) {
                MessageUtils.logWarning("[TacoEssentials] The alias /" + alias.getName()
                        + " is not registered: another command already uses that name.");
                continue;
            }
            register(alias.getName());
        }
    }

    public static void unloadAll() {
        for (AliasCommand command : new ArrayList<>(COMMANDS.values())) command.unregister();
        COMMANDS.clear();
        ALIASES.clear();
    }

    public static CustomAlias get(String name) {
        return name == null ? null : ALIASES.get(name.toLowerCase());
    }

    public static Collection<CustomAlias> all() {
        return ALIASES.values();
    }

    /** Whether {@code name} belongs to a command other than this module's alias of that name. */
    public static boolean isTaken(String name) {
        if (EDITOR_COMMAND.equals(name)) return true;
        if (COMMANDS.containsKey(name)) return false;
        for (String label : CommandHandler.getAllAliases()) {
            if (label.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /** Stores the alias, registering its command the first time it is saved. */
    public static void save(CustomAlias alias) {
        TacoDatabase.saveAlias(alias);
        ALIASES.put(alias.getName(), alias);
        if (! COMMANDS.containsKey(alias.getName()) && ! isTaken(alias.getName())) register(alias.getName());
    }

    public static void delete(String name) {
        TacoDatabase.deleteAlias(name);
        ALIASES.remove(name);
        AliasCommand command = COMMANDS.remove(name);
        if (command != null) command.unregister();
    }

    private static void register(String name) {
        AliasCommand command = new AliasCommand(name);
        command.register();
        COMMANDS.put(name, command);
    }
}
