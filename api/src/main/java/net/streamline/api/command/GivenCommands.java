package net.streamline.api.command;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.base.commands.*;

public class GivenCommands {
    @Getter @Setter
    private static ModulesCommand modulesCommand;
    @Getter @Setter
    private static ParseCommand parseCommand;
    @Getter @Setter
    private static PlaytimeCommand playtimeCommand;
    @Getter @Setter
    private static PointsCommand pointsCommand;
    @Getter @Setter
    private static PXPCommand pxpCommand;
    @Getter @Setter
    private static ReloadCommand reloadCommand;

    public static void init() {
        setModulesCommand(new ModulesCommand());
        setParseCommand(new ParseCommand());
        setPlaytimeCommand(new PlaytimeCommand());
        setPointsCommand(new PointsCommand());
        setPxpCommand(new PXPCommand());
        setReloadCommand(new ReloadCommand());

        getModulesCommand().register();
        getParseCommand().register();
        getPlaytimeCommand().register();
        getPointsCommand().register();
        getPxpCommand().register();
        getReloadCommand().register();
    }
}
