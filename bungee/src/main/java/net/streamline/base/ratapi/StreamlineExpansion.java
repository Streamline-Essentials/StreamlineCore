package net.streamline.base.ratapi;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import net.streamline.api.BasePlugin;
import net.streamline.api.modules.ModuleManager;
import net.streamline.base.Streamline;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.api.placeholder.RATExpansion;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.utils.MessagingUtils;

import java.util.List;

public class StreamlineExpansion extends RATExpansion {
    public StreamlineExpansion() {
        super("streamline", "Quaint", "0.0.0.1");
    }

    @Override
    public String onLogic(String params) {
        if (params.equals("version")) return BasePlugin.getVersion();
        if (params.equals("players_online")) return String.valueOf(BasePlugin.onlinePlayers().size());
        if (params.equals("players_loaded")) return String.valueOf(UserManager.getLoadedUsers().size());

        if (params.matches("([a][u][t][h][o][r][\\[]([0-2])[\\]])")) {
            Pattern pattern = Pattern.compile("([a][u][t][h][o][r][\\[]([0-9])[\\]])");
            Matcher matcher = pattern.matcher(params);
            while (matcher.find()) {
                return Streamline.getInstance().getDescription().getAuthor();
            }
        }

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
                SavableUser user = UserManager.getOrGetUser(BasePlugin.getUUIDFromName(things[0]));
                String parse = things[1].replace("*/*", "%");
                return MessagingUtils.replaceAllPlayerBungee(user, parse);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (params.equals("modules_loaded")) {
            return String.valueOf(ModuleManager.loadedModules.size());
        }

        return null;
    }

    @Override
    public String onRequest(SavableUser user, String params) {
        if (params.equals("user_ping")) {
            if (user.updateOnline()) return String.valueOf(BasePlugin.getPlayer(user.uuid).getPing());
            else return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_OFFLINE.get();
        }
        if (params.equals("user_online")) return user.updateOnline() ?
                MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_ONLINE.get() :
                MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_OFFLINE.get();
        if (params.equals("user_uuid")) return user.uuid;

        if (params.equals("user_absolute")) return UserManager.getAbsolute(user);
        if (params.equals("user_absolute_onlined")) return UserManager.getOffOnAbsolute(user);
        if (params.equals("user_formatted")) return UserManager.getFormatted(user);
        if (params.equals("user_formatted_onlined")) return UserManager.getOffOnFormatted(user);

        if (params.equals("user_prefix")) return UserManager.getLuckPermsPrefix(user.latestName);
        if (params.equals("user_suffix")) return UserManager.getLuckPermsSuffix(user.latestName);

        if (params.equals("user_points")) return String.valueOf(user.points);

        if (user instanceof SavablePlayer sp) {
            if (params.equals("user_level")) return String.valueOf(sp.level);
            if (params.equals("user_xp_current")) return String.valueOf(sp.currentXP);
            if (params.equals("user_xp_total")) return String.valueOf(sp.totalXP);

            if (params.equals("user_play_seconds")) return sp.getPlaySecondsAsString();
            if (params.equals("user_play_minutes")) return sp.getPlayMinutesAsString();
            if (params.equals("user_play_hours")) return sp.getPlayHoursAsString();
            if (params.equals("user_play_days")) return sp.getPlayDaysAsString();

            if (params.equals("user_ip")) return sp.latestIP;
        }

        if (params.equals("user_server")) return user.latestServer;

        if (params.equals("user_tags")) {
            List<String> tags = user.tagList;
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < tags.size(); i ++) {
                String tag = tags.get(i);

                if (i < tags.size() - 1) {
                    builder.append(MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.LISTS_BASE.get().replace("%value%", tag));
                } else {
                    builder.append(MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.LISTS_LAST.get().replace("%value%", tag));
                }
            }
        }

        return null;
    }
}
