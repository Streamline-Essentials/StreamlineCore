package net.streamline.api.base.ratapi;

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
import tv.quaint.utils.MatcherUtils;

import java.util.ArrayList;
import java.util.Optional;

public class StreamlineExpansion extends RATExpansion {
    public StreamlineExpansion() {
        super(new RATExpansionBuilder("streamline"));
        BaseModule.getInstance().logInfo(getClass().getSimpleName() + " is registered!");
    }

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
        new IdentifiedUserReplaceable(this, "user_server", (s, user) -> String.valueOf(user.getServerName())).register();
        new IdentifiedUserReplaceable(this, "user_tags", (s, user) -> user.getMeta().getTagsAsString()).register();

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
