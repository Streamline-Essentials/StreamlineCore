package gg.drak.tacoessentials.commands;

import singularity.permissions.DefaultPermissions;

/**
 * Permission nodes. Nodes in {@link #EVERYONE} are granted to all players by default; the rest
 * belong to operators. A permission plugin (LuckPerms on the mod loaders) can override either.
 * A command's aliases share its node, {@code tacoessentials.command.<primary name>}.
 */
public final class Perms {

    private static final String PREFIX = "tacoessentials.";

    private Perms() {}

    public static String command(String name) {
        return PREFIX + "command." + name;
    }

    /** Opening another player's {@code /homes} menu and {@code /homeother}. */
    public static final String HOMES_OTHERS = command("homes.others");
    /** Using another player's {@code /back} history. */
    public static final String BACK_OTHERS = command("back.others");
    /** Sending another player to a {@code /back} location. */
    public static final String BACK_TELEPORT_OTHERS = command("back.teleportothers");
    /** Using another player's death history with {@code /dback}. */
    public static final String DBACK_OTHERS = command("dback.others");
    /** Sending another player to a death location. */
    public static final String DBACK_TELEPORT_OTHERS = command("dback.teleportothers");
    public static final String EC_OTHERS = command("ec.others");
    /** Setting or clearing another player's nickname with {@code /nick}. */
    public static final String NICK_OTHERS = command("nick.others");
    /**
     * Using {@code /heal}, {@code /feed}, {@code /fly}, {@code /god}, the game mode and speed
     * commands on another player, online or offline.
     */
    public static final String STAFF_OTHERS = PREFIX + "staff.others";
    /** Teleporting without the stand-still countdown. */
    public static final String BYPASS_WARMUP = PREFIX + "teleport.bypasswarmup";
    public static final String RTP_BYPASS_COOLDOWN = PREFIX + "rtp.bypasscooldown";
    public static final String HOMES_UNLIMITED = PREFIX + "homes.unlimited";
    /** Highest N checked for {@link #homes(int)}. */
    public static final int HOMES_SCAN_LIMIT = 100;
    /** Colour codes in chat messages count instead of being stripped. */
    public static final String CHAT_COLOR = PREFIX + "chat.color";

    /** Running any custom alias; an alias with its permission toggle on also needs {@link #aliasUse}. */
    public static final String ALIAS = PREFIX + "alias";

    public static String aliasUse(String alias) {
        return PREFIX + "alias.use." + alias;
    }

    /** Allows {@code n} homes; the highest granted {@code n} wins. */
    public static String homes(int n) {
        return PREFIX + "homes." + n;
    }

    /** Commands every player may use. */
    public static final String[] EVERYONE = {
            "tpa", "tpahere", "tpaccept", "tpdeny", "tpacancel", "tpauto", "back", "dback", "rtp",
            "sethome", "home", "delhome", "homes",
            "warp", "listwarps", "spawn",
            "kickme", "trashcan", "hat", "nick", "center", "rotate",
            "afk", "ping", "playtime",
    };

    public static void registerDefaults() {
        for (String name : EVERYONE) DefaultPermissions.grantByDefault(command(name));
        DefaultPermissions.grantByDefault(ALIAS);
    }
}
