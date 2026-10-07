package gg.drak.tacoessentials.data;

import gg.drak.tacoessentials.TacoEssentials;
import singularity.configs.ModularizedConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@code config.yml} in the module's folder, written from the module jar's commented copy on first
 * run. Values are re-read on every use, so edits apply without a restart.
 */
public class TacoConfig extends ModularizedConfig {

    public TacoConfig() {
        super(TacoEssentials.getInstance(), "config.yml", true);
        init();
    }

    /** Writes every setting's default into the file, so admins can see what is configurable. */
    @Override
    public void init() {
        tpaTimeoutSeconds();
        backHistorySize();
        backOnDeath();
        deathHistorySize();
        jumpMaxDistance();
        defaultMaxHomes();
        rtpMaxAttempts();
        rtpCooldownSeconds();
        nickMaxLength();
        nickAllowColors();
        chatEnabled();
        chatFormat();
        chatHoverEnabled();
        chatHover();
        chatClickableLinks();
        displayNameFormat();
    }

    private int integer(String key, int def) {
        reloadResource();
        getOrSetDefault(key, def);
        return getResource().getInt(key);
    }

    private String string(String key, String def) {
        reloadResource();
        return getOrSetDefault(key, def);
    }

    private boolean bool(String key, boolean def) {
        reloadResource();
        getOrSetDefault(key, def);
        return getResource().getBoolean(key);
    }

    /** Seconds before a pending /tpa or /tpahere request expires. */
    public int tpaTimeoutSeconds() {
        return integer("teleport.tpa-timeout-seconds", 120);
    }

    /** How many previous locations /back remembers per player. */
    public int backHistorySize() {
        return integer("teleport.back-history-size", 10);
    }

    /** Whether dying records the death location for /back. */
    public boolean backOnDeath() {
        return bool("teleport.back-on-death", true);
    }

    /** How many past death locations /dback remembers per player. */
    public int deathHistorySize() {
        return integer("teleport.death-history-size", 10);
    }

    /** Maximum reach, in blocks, of /jump. */
    public int jumpMaxDistance() {
        return integer("teleport.jump-max-distance", 256);
    }

    /** Homes a player may own; tacoessentials.homes.unlimited lifts the limit (operators have it). */
    public int defaultMaxHomes() {
        return integer("homes.default-max", 5);
    }

    /** Candidate columns /rtp tries before giving up; each may generate a chunk. */
    public int rtpMaxAttempts() {
        return integer("rtp.max-attempts", 24);
    }

    /** Seconds between uses of /rtp; tacoessentials.rtp.bypasscooldown skips it. */
    public int rtpCooldownSeconds() {
        return integer("rtp.cooldown-seconds", 300);
    }

    /** Maximum visible length of a nickname, not counting & color codes. */
    public int nickMaxLength() {
        return integer("nick.max-length", 16);
    }

    /** Whether nicknames may use &-style color and format codes. */
    public boolean nickAllowColors() {
        return bool("nick.allow-colors", true);
    }

    /** Whether TacoEssentials formats public chat; off leaves chat to the server or other modules. */
    public boolean chatEnabled() {
        return bool("chat.enabled", true);
    }

    /** The chat line; see config.yml for its tokens. */
    public String chatFormat() {
        return string("chat.format", DEFAULT_CHAT_FORMAT);
    }

    /** Whether hovering the sender's part of a chat line shows {@link #chatHover()}, and clicking it starts a /msg. */
    public boolean chatHoverEnabled() {
        return bool("chat.hover-enabled", true);
    }

    /** The lines shown when hovering a sender in chat. */
    public List<String> chatHover() {
        reloadResource();
        List<String> lines = getOrSetDefault("chat.hover", new ArrayList<>(DEFAULT_CHAT_HOVER));
        return lines == null ? new ArrayList<>(DEFAULT_CHAT_HOVER) : lines;
    }

    /** Whether links in chat messages are clickable. */
    public boolean chatClickableLinks() {
        return bool("chat.clickable-links", true);
    }

    /** What {@code %displayname%} is; see config.yml for its tokens. */
    public String displayNameFormat() {
        return string("display-name.format", "%prefix%%nickname%%suffix%");
    }

    public static final String DEFAULT_CHAT_FORMAT =
            "%streamlinecollections_stars%%displayname%%streamlinetags_tag_left% &8» &r%message%";

    public static final List<String> DEFAULT_CHAT_HOVER = Arrays.asList(
            "&7%displayname%",
            "&7&m------------",
            "#FFD700★ &fStars #FFD700%streamlinecollections_star_level%",
            "&7&m------------",
            "&7Click to message");
}
