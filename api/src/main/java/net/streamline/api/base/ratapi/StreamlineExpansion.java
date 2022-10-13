package net.streamline.api.base.ratapi;

import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UserUtils;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.placeholder.RATExpansion;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StreamlineExpansion extends RATExpansion {
    public StreamlineExpansion() {
        super("streamline", "Quaint", "0.0.0.1");
        BaseModule.getInstance().logInfo(getClass().getSimpleName() + " is registered!");
    }

    @Override
    public String onLogic(String params) {
        if (params.equals("version")) return SLAPI.getInstance().getPlatform().getVersion();
        if (params.equals("players_online")) return String.valueOf(UserUtils.getOnlinePlayers().size());
        if (params.equals("users_online")) return String.valueOf(UserUtils.getOnlineUsers().size());
        if (params.equals("players_loaded")) return String.valueOf(UserUtils.getLoadedPlayers().size());
        if (params.equals("users_loaded")) return String.valueOf(UserUtils.getLoadedUsers().size());

        if (params.equals("null")) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }
        if (params.equals("true")) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_TRUE.get();
        }
        if (params.equals("false")) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_FALSE.get();
        }
        if (params.equals("online")) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_ONLINE.get();
        }
        if (params.equals("offline")) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_OFFLINE.get();
        }
        if (params.equals("placeholders_null")) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        }
        if (params.equals("placeholders_true")) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_TRUE.get();
        }
        if (params.equals("placeholders_false")) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_FALSE.get();
        }
        if (params.equals("placeholders_online")) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_ONLINE.get();
        }
        if (params.equals("placeholders_offline")) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_OFFLINE.get();
        }
        if (params.startsWith("parse_")) {
            try {
                String p = params.substring("parse_".length());
                String[] things = p.split(":::", 2);
                StreamlineUser user = UserUtils.getOrGetUserByName(things[0]);
                String parse = things[1].replace("*/*", "%");
                return SLAPI.getRatAPI().parseAllPlaceholders(user, parse).join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (params.equals("modules_loaded")) {
            return String.valueOf(ModuleManager.getLoadedModules().size());
        }
        if (params.startsWith("?L:")) {
            params = params.substring("?L:".length());
            params = params
                    .replace("{", "%")
                    .replace("}", "%")
                    .replace("*/*", "%")
            ;
            return ModuleUtils.parseOnProxy(params);
        }

        return null;
    }

    @Override
    public String onRequest(StreamlineUser user, String params) {
        if (params.equals("user_ping")) {
            if (user.updateOnline())
                return String.valueOf(SLAPI.getInstance().getUserManager().getPlayerPing(user.getUuid()));
            else return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_OFFLINE.get();
        }
        if (params.equals("user_online")) return user.updateOnline() ?
                MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_ONLINE.get() :
                MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_OFFLINE.get();
        if (params.equals("user_uuid")) return user.getUuid();

        if (params.equals("user_absolute")) return UserUtils.getAbsolute(user);
        if (params.equals("user_absolute_onlined")) return UserUtils.getOffOnAbsolute(user);
        if (params.equals("user_formatted")) return UserUtils.getFormatted(user);
        if (params.equals("user_formatted_onlined")) return UserUtils.getOffOnFormatted(user);

        if (params.equals("user_prefix")) return UserUtils.getLuckPermsPrefix(user.getLatestName());
        if (params.equals("user_suffix")) return UserUtils.getLuckPermsSuffix(user.getLatestName());

        if (params.equals("user_points")) return String.valueOf(user.getPoints());

        if (user instanceof StreamlinePlayer sp) {
            if (params.equals("user_level")) return String.valueOf(sp.getLevel());
            if (params.equals("user_xp_current")) return String.valueOf(sp.getCurrentXP());
            if (params.equals("user_xp_total")) return String.valueOf(sp.getTotalXP());

            if (params.equals("user_play_seconds")) return sp.getPlaySecondsAsString();
            if (params.equals("user_play_minutes")) return sp.getPlayMinutesAsString();
            if (params.equals("user_play_hours")) return sp.getPlayHoursAsString();
            if (params.equals("user_play_days")) return sp.getPlayDaysAsString();

            if (params.equals("user_ip")) return sp.getLatestIP();
        }

        if (params.equals("user_server")) return user.getLatestServer();

        if (params.equals("user_tags")) {
            List<String> tags = user.getTagList().stream().toList();
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < tags.size(); i++) {
                String tag = tags.get(i);

                if (i < tags.size() - 1) {
                    builder.append(MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.LISTS_BASE.get().replace("%value%", tag));
                } else {
                    builder.append(MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.LISTS_LAST.get().replace("%value%", tag));
                }
            }

            return builder.toString();
        }
        if (params.startsWith("?R:")) {
            params = params.substring("?R:".length());
            params = params
                    .replace("{", "%")
                    .replace("}", "%")
                    .replace("*/*", "%")
            ;
            return ModuleUtils.parseOnProxy(user, params);
        }

        return null;
    }
}
