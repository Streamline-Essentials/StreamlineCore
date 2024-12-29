package net.streamline.api.base.commands;

import lombok.Getter;
import lombok.Setter;

public class GivenCommands {
    @Getter @Setter
    private static ModulesCommand modulesCommand;
    @Getter @Setter
    private static ParseCommand parseCommand;
    @Getter @Setter
    private static PlaytimeCommand playtimeCommand;
    @Getter @Setter
    private static PTagCommand pTagCommand;
    @Getter @Setter
    private static ReloadCommand reloadCommand;
    @Getter @Setter
    private static SetServerCommand setServerCommand;
    @Getter @Setter
    private static SyncCommand syncCommand;

    public static void init() {
        setModulesCommand(new ModulesCommand());
        setParseCommand(new ParseCommand());
        setPlaytimeCommand(new PlaytimeCommand());
        setPTagCommand(new PTagCommand());
        setReloadCommand(new ReloadCommand());
        setSetServerCommand(new SetServerCommand());
        setSyncCommand(new SyncCommand());

        getModulesCommand().register();
        getParseCommand().register();
        getPlaytimeCommand().register();
        getPTagCommand().register();
        getReloadCommand().register();
        getSetServerCommand().register();
        getSyncCommand().register();
    }
}
