package host.plas.config;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.StreamersUnite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainConfig extends SimpleConfiguration {
    public MainConfig() {
        super("config.yml", StreamersUnite.getInstance(), false);
    }

    @Override
    public void init() {
        // Nothing to do here
        announceGoLive();
        announceGoOffline();

        getLivePrefix();
        getLiveSuffix();

        getOfflinePrefix();
        getOfflineSuffix();

        getLiveMessage();
        getGoLiveMessage();
        getGoOfflineMessage();
        getLinkHover();

        getAnnounceCooldownSeconds();
        getImLiveAnnouncement();
        for (String key : MESSAGE_DEFAULTS.keySet()) getMessage(key);
    }

    public boolean announceGoLive() {
        reloadResource();

        return getOrSetDefault("announce.go-live", true);
    }

    public boolean announceGoOffline() {
        reloadResource();

        return getOrSetDefault("announce.go-offline", false);
    }

    public String getLivePrefix() {
        reloadResource();

        return getOrSetDefault("prefix.live", "&f[&c&lLIVE&f] &r");
    }

    public String getLiveSuffix() {
        reloadResource();

        return getOrSetDefault("suffix.live", "&r &f[&c&lLIVE&f]");
    }

    public String getOfflinePrefix() {
        reloadResource();

        return getOrSetDefault("prefix.offline", "&f[&7&lOFFLINE&f] &r");
    }

    public String getOfflineSuffix() {
        reloadResource();

        return getOrSetDefault("suffix.offline", "&r &f[&7&lOFFLINE&f]");
    }

    public String getLiveMessage() {
        reloadResource();

        return getOrSetDefault("messages.live-now.live", "%display_name% &eis currently &alive &eat &b&o%link%");
    }

    public String getGoLiveMessage() {
        reloadResource();

        return getOrSetDefault("messages.go-live.main", "&5&k!! &4\u23fa &c&lLIVE NOW &4\u23fa &5&k!! &b%display_name% &ehas just gone &alive &eat&8: &d&o%link%");
    }

    public String getGoOfflineMessage() {
        reloadResource();

        return getOrSetDefault("messages.go-offline.main", "&b%display_name% &ehas just gone &coffline&8. &eThey were live at&8: &d&o%link%&8!");
    }

    /** The tooltip on a clickable stream link; {@code %link%} is the URL. */
    public String getLinkHover() {
        reloadResource();

        return getOrSetDefault("messages.link-hover", "#AAAAAAClick to watch #FFFFFF%link%");
    }

    /** Seconds a streamer waits between their own {@code /imlive} announcements. */
    public int getAnnounceCooldownSeconds() {
        reloadResource();

        return Math.max(0, getOrSetDefault("imlive.announce-cooldown-seconds", 600));
    }

    /** The lines {@code /imlive} broadcasts; {@code %player%}, {@code %display_name%} and {@code %link%} are filled in. */
    public List<String> getImLiveAnnouncement() {
        reloadResource();

        return getOrSetDefault("imlive.announcement", new ArrayList<>(List.of(
                "",
                "#A503FC&l● LIVE #FFFFFF%player% #AAAAAAis live now!",
                "#38C1FC&n%link%",
                ""
        )));
    }

    /** Defaults for {@link #getMessage(String)}, under {@code messages.streaming.}. */
    private static final Map<String, String> MESSAGE_DEFAULTS = new LinkedHashMap<>();

    static {
        MESSAGE_DEFAULTS.put("usage", "#FF5555Usage: /imlive <link> (streamer)");
        MESSAGE_DEFAULTS.put("usage-streamer", "#FF5555Usage: /streamer <add|remove|list> (player)");
        MESSAGE_DEFAULTS.put("invalid-link", "#FF5555That doesn't look like a link (https://...).");
        MESSAGE_DEFAULTS.put("not-registered", "#FF5555%player% is not a registered streamer.");
        MESSAGE_DEFAULTS.put("unknown-player", "#FF5555No player named %player% has joined before.");
        MESSAGE_DEFAULTS.put("cooldown", "#FF5555You can announce again in %time%.");
        MESSAGE_DEFAULTS.put("no-permission-others", "#FF5555You do not have permission to announce for other streamers.");
        MESSAGE_DEFAULTS.put("players-only", "#FF5555Name a streamer when running this from the console.");
        MESSAGE_DEFAULTS.put("announced", "#00FC88Announced #38C1FC%player%#00FC88's stream.");
        MESSAGE_DEFAULTS.put("added", "#00FC88Added #38C1FC%player% #00FC88as a streamer.");
        MESSAGE_DEFAULTS.put("already-added", "#FF5555%player% is already a streamer.");
        MESSAGE_DEFAULTS.put("removed", "#00FC88Removed #38C1FC%player% #00FC88from the streamers.");
        MESSAGE_DEFAULTS.put("list", "#AAAAAAStreamers: #FFFFFF%streamers%");
    }

    /** A {@code /imlive} or {@code /streamer} message, from {@code messages.streaming.<key>}. */
    public String getMessage(String key) {
        reloadResource();

        return getOrSetDefault("messages.streaming." + key, MESSAGE_DEFAULTS.getOrDefault(key, ""));
    }
}