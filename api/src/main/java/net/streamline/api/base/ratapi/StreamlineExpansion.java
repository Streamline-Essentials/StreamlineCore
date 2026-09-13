package net.streamline.api.base.ratapi;

import gg.drak.thebase.utils.MatcherUtils;
import singularity.Singularity;
import net.streamline.api.base.module.BaseModule;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleManager;
import singularity.modules.ModuleUtils;
import singularity.placeholders.expansions.RATExpansion;
import singularity.placeholders.replaceables.IdentifiedReplaceable;
import singularity.placeholders.replaceables.IdentifiedUserReplaceable;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Optional;

/**
 * RAT placeholder expansion that exposes core Streamline server and user data.
 *
 * <p>The expansion is registered under the {@code streamline} namespace.
 * Categories of placeholders provided (all prefixed with {@code %streamline_}):
 * <ul>
 *   <li>Server stats — {@code version}, {@code players_max},
 *       {@code players_online}, {@code users_online},
 *       {@code players_loaded}, {@code users_loaded}.</li>
 *   <li>Module stats — {@code modules_loaded}, {@code modules_enabled},
 *       {@code modules_colorized} and their {@code _count} variants.</li>
 *   <li>Default message aliases — {@code null}, {@code true}, {@code false},
 *       {@code online}, {@code offline} and their {@code placeholders_} prefixed
 *       counterparts.</li>
 *   <li>Parse helpers — {@code parse_NAME:::expr} (evaluate an
 *       expression in a named player's context) and {@code ?L:expr}
 *       (evaluate on the proxy).</li>
 *   <li>Per-user placeholders — ping, online status, UUID, display names,
 *       prefix/suffix, play-time, IP, server, location coordinates, and
 *       {@code ?R:expr} for per-user proxy evaluation.</li>
 * </ul>
 */
public class StreamlineExpansion extends RATExpansion {

    /**
     * Constructs the expansion and logs its registration to the base module's
     * logger.
     */
    public StreamlineExpansion() {
        super(new RATExpansionBuilder("streamline"));
        BaseModule.getInstance().logInfo(getClass().getSimpleName() + " is registered!");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Registers all Streamline placeholder replaceables with the RAT engine,
     * covering server statistics, module info, constant message aliases,
     * on-the-fly parse helpers, and per-user data fields.</p>
     */
    @Override
    public void init() {
        new IdentifiedReplaceable(this, "version", (s) -> Singularity.getInstance().getPlatform().getVersion()).register();
        new IdentifiedReplaceable(this, "players_max", (s) -> String.valueOf(Singularity.getInstance().getPlatform().getMaxPlayers())).register();
        new IdentifiedReplaceable(this, "players_online", (s) -> String.valueOf(UserUtils.getOnlinePlayers().size())).register();
        new IdentifiedReplaceable(this, "users_online", (s) -> String.valueOf(UserUtils.getOnlineSenders().size())).register();
        new IdentifiedReplaceable(this, "players_loaded", (s) -> String.valueOf(UserUtils.getLoadedPlayers().size())).register();
        new IdentifiedReplaceable(this, "users_loaded", (s) -> String.valueOf(UserUtils.getLoadedSenders().size())).register();

        new IdentifiedReplaceable(this, "modules_loaded", (s) -> ModuleUtils.getListAsFormattedString(new ArrayList<>(ModuleManager.getLoadedModuleIdentifiers()))).register();
        new IdentifiedReplaceable(this, "modules_enabled", (s) -> ModuleUtils.getListAsFormattedString(new ArrayList<>(ModuleManager.getEnabledModuleIdentifiers()))).register();
        new IdentifiedReplaceable(this, "modules_colorized", (s) -> ModuleUtils.getListAsFormattedString(new ArrayList<>(ModuleManager.getColorizedLoadedModuleIdentifiers()))).register();

        new IdentifiedReplaceable(this, "modules_loaded_count", (s) -> String.valueOf(ModuleManager.getLoadedModuleIdentifiers().size())).register();
        new IdentifiedReplaceable(this, "modules_enabled_count", (s) -> String.valueOf(ModuleManager.getEnabledModuleIdentifiers().size())).register();
        new IdentifiedReplaceable(this, "modules_colorized_count", (s) -> String.valueOf(ModuleManager.getColorizedLoadedModuleIdentifiers().size())).register();

        new IdentifiedReplaceable(this, "null", (s) -> MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get()).register();
        new IdentifiedReplaceable(this, "true", (s) -> MainMessagesHandler.MESSAGES.DEFAULTS.IS_TRUE.get()).register();
        new IdentifiedReplaceable(this, "false", (s) -> MainMessagesHandler.MESSAGES.DEFAULTS.IS_FALSE.get()).register();
        new IdentifiedReplaceable(this, "online", (s) -> MainMessagesHandler.MESSAGES.DEFAULTS.IS_ONLINE.get()).register();
        new IdentifiedReplaceable(this, "offline", (s) -> MainMessagesHandler.MESSAGES.DEFAULTS.IS_OFFLINE.get()).register();

        new IdentifiedReplaceable(this, "placeholders_null", (s) -> MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get()).register();
        new IdentifiedReplaceable(this, "placeholders_true", (s) -> MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_TRUE.get()).register();
        new IdentifiedReplaceable(this, "placeholders_false", (s) -> MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_FALSE.get()).register();
        new IdentifiedReplaceable(this, "placeholders_online", (s) -> MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_ONLINE.get()).register();
        new IdentifiedReplaceable(this, "placeholders_offline", (s) -> MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_OFFLINE.get()).register();

        new IdentifiedReplaceable(this, MatcherUtils.makeLiteral("parse_") + "(.*?)", 1, (s) -> {
            try {
                if (s.get().contains(":::")) {
                    String[] things = s.get().split(":::", 2);
                    Optional<CosmicSender> userOptional = UserUtils.getOrCreateSenderByName(things[0]);
                    if (userOptional.isEmpty()) return s.string();
                    String parse = things[1].replace("*/*", "%");
                    return ModuleUtils.replacePlaceholders(userOptional.get(), parse);
                } else {
                    return ModuleUtils.replacePlaceholders(s.get());
                }
            } catch (Exception e) {
                e.printStackTrace();
                return s.string();
            }
        }).register();

        new IdentifiedReplaceable(this, "[?][L][:](.*?)", 1, (s) -> {
            try {
                String params = s.get();
                params = params
                        .replace("[[", "%")
                        .replace("]]", "%")
                        .replace("{{", "%")
                        .replace("}}", "%")
                        .replace("*/*", "%")
                ;
                return ModuleUtils.parseOnProxy(params);
            } catch (Exception e) {
                e.printStackTrace();
                return s.string();
            }
        }).register();

        new IdentifiedUserReplaceable(this, "user_ping", (s, user) -> {
            if (user.isConsole()) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_OFFLINE.get();
            else return String.valueOf(Singularity.getInstance().getUserManager().getPlayerPing(user.getUuid()));
        }).register();
        new IdentifiedUserReplaceable(this, "user_online", (s, user) -> user.isOnline() ?
                MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_ONLINE.get() :
                MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_OFFLINE.get()).register();
        new IdentifiedUserReplaceable(this, "user_uuid", (s, user) -> user.getUuid()).register();

        new IdentifiedUserReplaceable(this, "user_absolute", (s, user) -> UserUtils.getAbsolute(user)).register();
        new IdentifiedUserReplaceable(this, "user_absolute_onlined", (s, user) -> UserUtils.getOffOnAbsolute(user)).register();
        new IdentifiedUserReplaceable(this, "user_formatted", (s, user) -> UserUtils.getFormatted(user)).register();
        new IdentifiedUserReplaceable(this, "user_formatted_onlined", (s, user) -> UserUtils.getOffOnFormatted(user)).register();

        new IdentifiedUserReplaceable(this, "user_prefix", (s, user) -> UserUtils.getPrefix(user)).register();
        new IdentifiedUserReplaceable(this, "user_suffix", (s, user) -> UserUtils.getSuffix(user)).register();

        new IdentifiedUserReplaceable(this, "user_play_seconds",
                (s, user) -> user instanceof CosmicPlayer ? String.valueOf(((CosmicPlayer) user).getPlaySecondsAsString()) : s.string()).register();
        new IdentifiedUserReplaceable(this, "user_play_minutes",
                (s, user) -> user instanceof CosmicPlayer ? String.valueOf(((CosmicPlayer) user).getPlayMinutesAsString()) : s.string()).register();
        new IdentifiedUserReplaceable(this, "user_play_hours",
                (s, user) -> user instanceof CosmicPlayer ? String.valueOf(((CosmicPlayer) user).getPlayHoursAsString()) : s.string()).register();
        new IdentifiedUserReplaceable(this, "user_play_days",
                (s, user) -> user instanceof CosmicPlayer ? String.valueOf(((CosmicPlayer) user).getPlayDaysAsString()) : s.string()).register();
        new IdentifiedUserReplaceable(this, "user_ip",
                (s, user) -> user instanceof CosmicPlayer ? String.valueOf(((CosmicPlayer) user).getCurrentIp()) : s.string()).register();
        new IdentifiedUserReplaceable(this, "user_server", (s, user) -> user.getServerName()).register();
        new IdentifiedUserReplaceable(this, "user_tags", (s, user) -> user.getMeta().getTagsAsString()).register();

        new IdentifiedUserReplaceable(this, "user_location_server", (s, user) -> user.getServerName()).register();
        new IdentifiedUserReplaceable(this, "user_location_world",
                (s, user) -> user instanceof CosmicPlayer ? String.valueOf(((CosmicPlayer) user).getWorld()) : s.string()).register();
        new IdentifiedUserReplaceable(this, "user_location_x",
                (s, user) -> user instanceof CosmicPlayer ? String.valueOf(((CosmicPlayer) user).getX()) : s.string()).register();
        new IdentifiedUserReplaceable(this, "user_location_y",
                (s, user) -> user instanceof CosmicPlayer ? String.valueOf(((CosmicPlayer) user).getY()) : s.string()).register();
        new IdentifiedUserReplaceable(this, "user_location_z",
                (s, user) -> user instanceof CosmicPlayer ? String.valueOf(((CosmicPlayer) user).getZ()) : s.string()).register();
        new IdentifiedUserReplaceable(this, "user_location_pitch",
                (s, user) -> user instanceof CosmicPlayer ? String.valueOf(((CosmicPlayer) user).getPitch()) : s.string()).register();
        new IdentifiedUserReplaceable(this, "user_location_yaw",
                (s, user) -> user instanceof CosmicPlayer ? String.valueOf(((CosmicPlayer) user).getYaw()) : s.string()).register();

        new IdentifiedUserReplaceable(this, "[?][R][:](.*?)", 1, (s, user) -> {
            try {
                String params = s.get();
                params = params
                        .replace("[[", "%")
                        .replace("]]", "%")
                        .replace("{{", "%")
                        .replace("}}", "%")
                        .replace("*/*", "%")
                ;
                return ModuleUtils.parseOnProxy(user, params);
            } catch (Exception e) {
                e.printStackTrace();
                return s.string();
            }
        }).register();
    }
}
