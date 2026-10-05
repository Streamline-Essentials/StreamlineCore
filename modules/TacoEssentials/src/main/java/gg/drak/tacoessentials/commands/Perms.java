package gg.drak.tacoessentials.commands;

import singularity.permissions.DefaultPermissions;

/**
 * Permission nodes. Nodes in {@link #EVERYONE} are granted to all players by default; the rest
 * belong to operators. A permission plugin (LuckPerms on the mod loaders) can override either.
 */
public final class Perms {

    private static final String PREFIX = "tacoessentials.";

    private Perms() {}

    public static String command(String name) {
        return PREFIX + "command." + name;
    }

    public static final String LISTHOMES_OTHERS = command("listhomes.others");
    public static final String DBACK_OTHERS = command("dback.others");
    public static final String RTP_BYPASS_COOLDOWN = PREFIX + "rtp.bypasscooldown";
    public static final String HOMES_UNLIMITED = PREFIX + "homes.unlimited";

    /** Running any custom alias; an alias with its permission toggle on also needs {@link #aliasUse}. */
    public static final String ALIAS = PREFIX + "alias";

    public static String aliasUse(String alias) {
        return PREFIX + "alias.use." + alias;
    }

    /** Commands every player may use. */
    public static final String[] EVERYONE = {
            "tpa", "tpahere", "tpaccept", "tpadeny", "back", "dback", "rtp",
            "sethome", "home", "delhome", "listhomes",
            "warp", "listwarps", "spawn",
            "kickme", "trashcan", "hat", "nick",
    };

    public static void registerDefaults() {
        for (String name : EVERYONE) DefaultPermissions.grantByDefault(command(name));
        DefaultPermissions.grantByDefault(ALIAS);
    }
}
