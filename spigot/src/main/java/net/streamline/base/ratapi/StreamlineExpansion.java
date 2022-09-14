package net.streamline.base.ratapi;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UserUtils;
import net.streamline.platform.BasePlugin;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.placeholder.RATExpansion;
import net.streamline.base.Streamline;

import java.util.List;

public class StreamlineExpansion extends RATExpansion {
    public StreamlineExpansion() {
        super("streamline", "Quaint", "0.0.0.1");
    }

    @Override
    public String onLogic(String params) {
        if (params.equals("version")) return SLAPI.getInstance().getPlatform().getVersion();
        if (params.equals("players_online")) return String.valueOf(BasePlugin.onlinePlayers().size());
        if (params.equals("players_loaded")) return String.valueOf(UserUtils.getLoadedUsers().size());

        if (params.matches("([a][u][t][h][o][r][\\[]([0-2])[\\]])")) {
            Pattern pattern = Pattern.compile("([a][u][t][h][o][r][\\[]([0-9])[\\]])");
            Matcher matcher = pattern.matcher(params);
            while (matcher.find()) {
                return Streamline.getInstance().getDescription().getAuthors().get(0);
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
                StreamlineUser user = UserUtils.getOrGetUser(UserUtils.getUUIDFromName(things[0]));
                String parse = things[1].replace("*/*", "%");
                return SLAPI.getInstance().getMessenger().replaceAllPlayerBungee(user, parse);
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
    public String onRequest(StreamlineUser user, String params) {
        if (params.equals("user_ping")) {
            if (user.updateOnline()) return String.valueOf(BasePlugin.getPlayer(user.getUuid()).getPing());
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
