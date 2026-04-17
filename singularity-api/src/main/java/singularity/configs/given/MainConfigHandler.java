package singularity.configs.given;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import singularity.Singularity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Configuration handler for the primary Streamline settings file, backed by
 * {@code main-config.yml} in the plugin's data folder.
 *
 * <p>Covers console identity, player data, experience/levelling, tag defaults,
 * debug console output settings, network/IP spoofing, and the no-internet flag.
 * All getter methods reload the resource before reading so they always reflect
 * the latest on-disk values.</p>
 */
public class MainConfigHandler extends SimpleConfiguration {

    /**
     * Constructs the handler, loading (or creating) {@code main-config.yml}
     * in the Singularity plugin's data folder as a self-contained resource.
     */
    public MainConfigHandler() {
        super("main-config.yml", Singularity.getInstance().getDataFolder(), true);
    }

    /**
     * Eagerly reads every configuration key so that default values are written
     * to the file when it is first created.
     */
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

        isNoInternet();

        isSpoofIPs();
        getSpoofedIP();

//        getHexPolicies();
    }

    // CONSOLE

    /**
     * Returns the internal name used to identify the console sender,
     * defaulting to {@code "Console"}. Reloads the resource before reading.
     *
     * @return the console name string
     */
    public String getConsoleName() {
        reloadResource();

        return getResource().getOrSetDefault("console.name", "Console");
    }

    /**
     * Returns the formatted display name shown for the console in chat,
     * defaulting to {@code "&c&lConsole&r"}. Reloads the resource before reading.
     *
     * @return the console display name with colour codes
     */
    public String getConsoleDisplayName() {
        reloadResource();

        return getResource().getOrSetDefault("console.display-name", "&c&lConsole&r");
    }

    /**
     * Returns the single-character or short string used as the console's UUID
     * discriminator (e.g. to distinguish it from player UUIDs), defaulting to
     * {@code "%"}. Reloads the resource before reading.
     *
     * @return the console discriminator string
     */
    public String getConsoleDiscriminator() {
        reloadResource();

        return getResource().getOrSetDefault("console.discriminator", "%");
    }

    /**
     * Returns the logical server name reported for the console sender,
     * defaulting to {@code "space"}. Reloads the resource before reading.
     *
     * @return the console's server name
     */
    public String getConsoleServer() {
        reloadResource();

        return getResource().getOrSetDefault("console.server", "space");
    }

    // USER

    /**
     * Returns the interval (in ticks) at which player data is auto-saved,
     * defaulting to {@code 500} (10 seconds). Reloads the resource before reading.
     *
     * @return the save interval in ticks
     */
    public long getPlayerDataSaveInterval() {
        reloadResource();

        return getResource().getOrSetDefault("players.save-interval", 1000L * 10 / 20); // 10 seconds (in ticks)
    }

    /**
     * Returns the default nickname meta value for new players, defaulting to
     * an empty string. Reloads the resource before reading.
     *
     * @return the default meta nickname
     */
    public String getDefaultMetaNickname() {
        reloadResource();

        return getResource().getOrSetDefault("players.meta.nickname", "");
    }

    /**
     * Returns the default chat prefix meta value for new players, defaulting
     * to an empty string. Reloads the resource before reading.
     *
     * @return the default meta prefix
     */
    public String getDefaultMetaPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("players.meta.prefix", "");
    }

    /**
     * Returns the default chat suffix meta value for new players, defaulting
     * to an empty string. Reloads the resource before reading.
     *
     * @return the default meta suffix
     */
    public String getDefaultMetaSuffix() {
        reloadResource();

        return getResource().getOrSetDefault("players.meta.suffix", "");
    }

    /**
     * Returns whether player formatted names should be kept up to date
     * automatically, defaulting to {@code true}. Reloads the resource before reading.
     *
     * @return {@code true} if formatted name updates are enabled
     */
    public boolean updatePlayerFormattedNames() {
        reloadResource();

        return getResource().getOrSetDefault("players.name.formatted", true);
    }

    /**
     * Returns the format string used to display a player's name while online,
     * defaulting to {@code "&d%streamline_user_formatted% &a&l•&r"}.
     * Reloads the resource before reading.
     *
     * @return the online player name format string
     */
    public String playerOnlineName() {
        reloadResource();

        return getResource().getOrSetDefault("players.name.online", "&d%streamline_user_formatted% &a&l•&r");
    }

    /**
     * Returns the format string used to display a player's name while offline,
     * defaulting to {@code "&d%streamline_user_formatted% &c&l•&r"}.
     * Reloads the resource before reading.
     *
     * @return the offline player name format string
     */
    public String playerOfflineName() {
        reloadResource();

        return getResource().getOrSetDefault("players.name.offline", "&d%streamline_user_formatted% &c&l•&r");
    }

    /**
     * Returns whether a title should be sent to the player when they level up,
     * defaulting to {@code true}. Reloads the resource before reading.
     *
     * @return {@code true} if level-up title announcements are enabled
     */
    public boolean announceLevelChangeTitle() {
        reloadResource();

        return getResource().getOrSetDefault("players.experience.announce.level-change.title", true);
    }

    /**
     * Returns whether a chat message should be sent to the player when they
     * level up, defaulting to {@code false}. Reloads the resource before reading.
     *
     * @return {@code true} if level-up chat announcements are enabled
     */
    public boolean announceLevelChangeChat() {
        reloadResource();

        return getResource().getOrSetDefault("players.experience.announce.level-change.chat", false);
    }

    /**
     * Returns the amount of experience awarded per periodic payout tick,
     * defaulting to {@code 1.0}. Reloads the resource before reading.
     *
     * @return the experience payout amount
     */
    public double playerPayoutExperienceAmount() {
        reloadResource();

        return getResource().getOrSetDefault("players.experience.payout.amount", 1.0);
    }

    /**
     * Returns how frequently (in ticks) the experience payout is triggered,
     * defaulting to {@code 400}. Reloads the resource before reading.
     *
     * @return the payout frequency in ticks
     */
    public int playerPayoutExperienceEvery() {
        reloadResource();

        return getResource().getOrSetDefault("players.experience.payout.every", 400);
    }

    /**
     * Returns the starting level for newly created players, defaulting to
     * {@code 1}. Reloads the resource before reading.
     *
     * @return the starting player level
     */
    public int playerStartingLevel() {
        reloadResource();

        return getResource().getOrSetDefault("players.experience.starting.level", 1);
    }

    /**
     * Returns the amount of experience new players begin with, defaulting to
     * {@code 0}. Reloads the resource before reading.
     *
     * @return the starting experience amount
     */
    public double playerStartingExperienceAmount() {
        reloadResource();

        return getResource().getOrSetDefault("players.experience.starting.xp", 0);
    }

    /**
     * Returns the mathematical equation string used to calculate the experience
     * required to reach the next level, defaulting to
     * {@code "2500 + (2500 * (%streamline_user_level% - 1))"}.
     * Reloads the resource before reading.
     *
     * @return the levelling equation string (may reference placeholders)
     */
    public String playerLevelingEquation() {
        reloadResource();

        return getResource().getOrSetDefault("players.experience.equation", "2500 + (2500 * (%streamline_user_level% - 1))");
    }

    /**
     * Returns the list of tags that are applied to players by default,
     * defaulting to an empty list. Reloads the resource before reading.
     *
     * @return the default player tag list
     */
    public List<String> playerTagsDefault() {
        reloadResource();

        return getResource().getOrSetDefault("players.tags.default", new ArrayList<>());
    }

    // DEBUG

    /**
     * Returns whether a warning notification is logged when no modules are
     * loaded, defaulting to {@code true}. Reloads the resource before reading.
     *
     * @return {@code true} if the no-modules notification is enabled
     */
    public boolean debugNotifyNoModules() {
        reloadResource();

        return getResource().getOrSetDefault("debug.notify-on.no-modules", true);
    }

    /**
     * Returns whether informational console messages are fully suppressed,
     * defaulting to {@code false}. Reloads the resource before reading.
     *
     * @return {@code true} if info-level console output is disabled
     */
    public boolean debugConsoleInfoDisabled() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.info.full-disable", false);
    }

    /**
     * Returns the prefix prepended to informational console messages,
     * defaulting to {@code "&f[&3StreamlineCore&f] &r"}.
     * Reloads the resource before reading.
     *
     * @return the info message prefix with colour codes
     */
    public String debugConsoleInfoPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.info.prefix", "&f[&3StreamlineCore&f] &r");
    }

    /**
     * Returns whether warning console messages are fully suppressed,
     * defaulting to {@code false}. Reloads the resource before reading.
     *
     * @return {@code true} if warning-level console output is disabled
     */
    public boolean debugConsoleWarningsDisabled() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.warnings.full-disable", false);
    }

    /**
     * Returns the prefix prepended to warning console messages,
     * defaulting to {@code "&f[&3StreamlineCore&f] &6"}.
     * Reloads the resource before reading.
     *
     * @return the warning message prefix with colour codes
     */
    public String debugConsoleWarningsPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.warnings.prefix", "&f[&3StreamlineCore&f] &6");
    }

    /**
     * Returns whether error console messages are fully suppressed,
     * defaulting to {@code false}. Reloads the resource before reading.
     *
     * @return {@code true} if error-level console output is disabled
     */
    public boolean debugConsoleErrorsDisabled() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.errors.full-disable", false);
    }

    /**
     * Returns the prefix prepended to error console messages,
     * defaulting to {@code "&f[&3StreamlineCore&f] &c"}.
     * Reloads the resource before reading.
     *
     * @return the error message prefix with colour codes
     */
    public String debugConsoleErrorsPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.errors.prefix", "&f[&3StreamlineCore&f] &c");
    }

    /**
     * Returns whether debug-level console messages are fully suppressed,
     * defaulting to {@code true}. Reloads the resource before reading.
     *
     * @return {@code true} if debug-level console output is disabled
     */
    public boolean debugConsoleDebugDisabled() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.debug.full-disable", true);
    }

    /**
     * Returns the prefix prepended to debug console messages,
     * defaulting to {@code "&f[&3StreamlineCore&f] &f[&cDEBUG&f] &r"}.
     * Reloads the resource before reading.
     *
     * @return the debug message prefix with colour codes
     */
    public String debugConsoleDebugPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.debug.prefix", "&f[&3StreamlineCore&f] &f[&cDEBUG&f] &r");
    }

    /**
     * Returns whether the plugin should operate in no-internet mode (skipping
     * any outbound network calls), defaulting to {@code false}.
     * Reloads the resource before reading.
     *
     * @return {@code true} if no-internet mode is active
     */
    public boolean isNoInternet() {
        reloadResource();

        return getResource().getOrSetDefault("no-internet", false);
    }

    /**
     * Returns whether player IP addresses should be replaced with the configured
     * spoofed IP, defaulting to {@code true}. Reloads the resource before reading.
     *
     * @return {@code true} if IP spoofing is enabled
     */
    public boolean isSpoofIPs() {
        reloadResource();

        return getResource().getOrSetDefault("ips.spoof", true);
    }

    /**
     * Returns the IP address string substituted for all player IPs when spoofing
     * is enabled, defaulting to {@code "0.0.0.0"}.
     * Reloads the resource before reading.
     *
     * @return the spoofed IP address string
     */
    public String getSpoofedIP() {
        reloadResource();

        return getResource().getOrSetDefault("ips.spoofed", "0.0.0.0");
    }
}
