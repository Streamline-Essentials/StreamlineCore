package net.streamline.api.base.commands;

import lombok.Getter;
import lombok.Setter;

/**
 * Central registry that holds and initialises all built-in Streamline commands.
 *
 * <p>Call {@link #init()} once during plugin startup to instantiate every
 * command and register it with the platform's command dispatcher. Each command
 * instance is also stored as a static field so other components can obtain a
 * reference without re-creating it.</p>
 */
public class GivenCommands {

    /**
     * The singleton instance of {@link ModulesCommand} registered at startup.
     */
    @Getter @Setter
    private static ModulesCommand modulesCommand;

    /**
     * The singleton instance of {@link ParseCommand} registered at startup.
     */
    @Getter @Setter
    private static ParseCommand parseCommand;

    /**
     * The singleton instance of {@link PlaytimeCommand} registered at startup.
     */
    @Getter @Setter
    private static PlaytimeCommand playtimeCommand;

    /**
     * The singleton instance of {@link PTagCommand} registered at startup.
     */
    @Getter @Setter
    private static PTagCommand pTagCommand;

    /**
     * The singleton instance of {@link ReloadCommand} registered at startup.
     */
    @Getter @Setter
    private static ReloadCommand reloadCommand;

    /**
     * The singleton instance of {@link SetServerCommand} registered at startup.
     */
    @Getter @Setter
    private static SetServerCommand setServerCommand;

    /**
     * The singleton instance of {@link SyncCommand} registered at startup.
     */
    @Getter @Setter
    private static SyncCommand syncCommand;

    /**
     * The singleton instance of {@link DebugCommand} registered at startup.
     */
    @Getter @Setter
    private static DebugCommand debugCommand;

    /**
     * Instantiates every built-in command and registers each one with the
     * platform command dispatcher.
     *
     * <p>This method must be called exactly once during plugin initialisation.
     * Subsequent calls will overwrite the stored instances and re-register the
     * commands.</p>
     */
    public static void init() {
        setModulesCommand(new ModulesCommand());
        setParseCommand(new ParseCommand());
        setPlaytimeCommand(new PlaytimeCommand());
        setPTagCommand(new PTagCommand());
        setReloadCommand(new ReloadCommand());
        setSetServerCommand(new SetServerCommand());
        setSyncCommand(new SyncCommand());
        setDebugCommand(new DebugCommand());

        getModulesCommand().register();
        getParseCommand().register();
        getPlaytimeCommand().register();
        getPTagCommand().register();
        getReloadCommand().register();
        getSetServerCommand().register();
        getSyncCommand().register();
        getDebugCommand().register();
    }
}
