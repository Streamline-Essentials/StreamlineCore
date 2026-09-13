package host.plas.discord.data.channeling;

public enum EndPointType {
    GLOBAL_NATIVE, // Requires: NOTHING
    SPECIFIC_NATIVE, // Requires: identifier
    PERMISSION, // Requires: permission

    GUILD, // Requires: identifier
    PARTY, // Requires: identifier

    SPECIFIC_HANDLED, // Requires: identifier

    DISCORD_TEXT, // Requires: channel id
    ;
}
