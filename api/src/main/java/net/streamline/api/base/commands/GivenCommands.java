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
    private static ReloadCommand reloadCommand;
    @Getter @Setter
    private static SyncCommand syncCommand;

    public static void init() {
        setModulesCommand(new ModulesCommand());
        setParseCommand(new ParseCommand());
        setPlaytimeCommand(new PlaytimeCommand());
        setReloadCommand(new ReloadCommand());
        setSyncCommand(new SyncCommand());

        getModulesCommand().register();
        getParseCommand().register();
        getPlaytimeCommand().register();
        getReloadCommand().register();
        getSyncCommand().register();
    }
}
