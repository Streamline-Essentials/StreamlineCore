package singularity.configs.given;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import singularity.Singularity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainConfigHandler extends SimpleConfiguration {
    public MainConfigHandler() {
        super("main-config.yml", Singularity.getInstance().getDataFolder(), true);
        init();
    }

    public void init() {
        getConsoleName();
        getConsoleDiscriminator();
        getConsoleServer();
        getConsoleDisplayName();

        updatePlayerFormattedNames();

        playerLevelingEquation();
        playerOfflineName();
        playerOnlineName();
        playerTagsDefault();
        playerStartingLevel();
        playerPayoutExperienceAmount();
        playerPayoutExperienceEvery();
        playerStartingExperienceAmount();

        debugNotifyNoModules();

        debugConsoleInfoDisabled();
        debugConsoleInfoPrefix();
        debugConsoleWarningsDisabled();
        debugConsoleWarningsPrefix();
        debugConsoleErrorsDisabled();
        debugConsoleErrorsPrefix();
        debugConsoleDebugDisabled();
        debugConsoleDebugPrefix();

//        getHexPolicies();
    }

    // CONSOLE

    public String getConsoleName() {
        reloadResource();

        return getResource().getOrSetDefault("console.name", "Console");
    }

    public String getConsoleDisplayName() {
        reloadResource();

        return getResource().getOrSetDefault("console.display-name", "&c&lConsole&r");
    }

    public String getConsoleDiscriminator() {
        reloadResource();

        return getResource().getOrSetDefault("console.discriminator", "%");
    }

    public String getConsoleServer() {
        reloadResource();

        return getResource().getOrSetDefault("console.server", "space");
    }

    // USER

    public long getPlayerDataSaveInterval() {
        reloadResource();

        return getResource().getOrSetDefault("players.save-interval", 1000L * 10 / 20); // 10 seconds (in ticks)
    }

    public String getDefaultMetaNickname() {
        reloadResource();

        return getResource().getOrSetDefault("players.meta.nickname", "");
    }

    public String getDefaultMetaPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("players.meta.prefix", "");
    }

    public String getDefaultMetaSuffix() {
        reloadResource();

        return getResource().getOrSetDefault("players.meta.suffix", "");
    }

    public boolean updatePlayerFormattedNames() {
        reloadResource();

        return getResource().getOrSetDefault("players.name.formatted", true);
    }

    public String playerOnlineName() {
        reloadResource();

        return getResource().getOrSetDefault("players.name.online", "&d%streamline_user_formatted% &a&l•&r");
    }

    public String playerOfflineName() {
        reloadResource();

        return getResource().getOrSetDefault("players.name.offline", "&d%streamline_user_formatted% &c&l•&r");
    }

    public boolean announceLevelChangeTitle() {
        reloadResource();

        return getResource().getOrSetDefault("players.experience.announce.level-change.title", true);
    }

    public boolean announceLevelChangeChat() {
        reloadResource();

        return getResource().getOrSetDefault("players.experience.announce.level-change.chat", false);
    }

    public double playerPayoutExperienceAmount() {
        reloadResource();

        return getResource().getOrSetDefault("players.experience.payout.amount", 1.0);
    }

    public int playerPayoutExperienceEvery() {
        reloadResource();

        return getResource().getOrSetDefault("players.experience.payout.every", 400);
    }

    public int playerStartingLevel() {
        reloadResource();

        return getResource().getOrSetDefault("players.experience.starting.level", 1);
    }

    public double playerStartingExperienceAmount() {
        reloadResource();

        return getResource().getOrSetDefault("players.experience.starting.xp", 0);
    }

    public String playerLevelingEquation() {
        reloadResource();

        return getResource().getOrSetDefault("players.experience.equation", "2500 + (2500 * (%streamline_user_level% - 1))");
    }

    public List<String> playerTagsDefault() {
        reloadResource();

        return getResource().getOrSetDefault("players.tags.default", new ArrayList<>());
    }

    // DEBUG

    public boolean debugNotifyNoModules() {
        reloadResource();

        return getResource().getOrSetDefault("debug.notify-on.no-modules", true);
    }

    public boolean debugConsoleInfoDisabled() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.info.full-disable", false);
    }

    public String debugConsoleInfoPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.info.prefix", "&f[&3StreamlineCore&f] &r");
    }

    public boolean debugConsoleWarningsDisabled() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.warnings.full-disable", false);
    }

    public String debugConsoleWarningsPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.warnings.prefix", "&f[&3StreamlineCore&f] &6");
    }

    public boolean debugConsoleErrorsDisabled() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.errors.full-disable", false);
    }

    public String debugConsoleErrorsPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.errors.prefix", "&f[&3StreamlineCore&f] &c");
    }

    public boolean debugConsoleDebugDisabled() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.debug.full-disable", true);
    }

    public String debugConsoleDebugPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.debug.prefix", "&f[&3StreamlineCore&f] &f[&cDEBUG&f] &r");
    }
}
