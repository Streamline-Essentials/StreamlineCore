package gg.drak.tacoessentials.data;

import gg.drak.tacoessentials.TacoEssentials;
import singularity.configs.ModularizedConfig;

/**
 * {@code config.yml} in the module's folder. Values are re-read on every use, so edits apply
 * without a restart.
 */
public class TacoConfig extends ModularizedConfig {

    public TacoConfig() {
        super(TacoEssentials.getInstance(), "config.yml", false);
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
    }

    private int integer(String key, int def) {
        reloadResource();
        getOrSetDefault(key, def);
        return getResource().getInt(key);
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
        return integer("nick.max-length", 24);
    }

    /** Whether nicknames may use &-style color and format codes. */
    public boolean nickAllowColors() {
        return bool("nick.allow-colors", true);
    }
}
