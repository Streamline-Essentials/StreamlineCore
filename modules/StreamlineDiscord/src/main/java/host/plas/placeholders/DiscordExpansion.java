package host.plas.placeholders;

import host.plas.StreamlineDiscord;
import host.plas.config.VerifiedUsers;
import host.plas.discord.DiscordHandler;
import host.plas.discord.data.channeling.RouteLoader;
import host.plas.discord.data.verified.VerifiedUser;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.ModuleUtils;
import singularity.placeholders.expansions.RATExpansion;
import singularity.placeholders.replaceables.IdentifiedReplaceable;
import singularity.placeholders.replaceables.IdentifiedUserReplaceable;

import java.util.concurrent.ConcurrentSkipListSet;

public class DiscordExpansion extends RATExpansion {
    public DiscordExpansion() {
        super(new RATExpansionBuilder("discord"));
    }

    @Override
    public void init() {
        new IdentifiedReplaceable(this, "bot_ping", (s) -> {
            double millis = DiscordHandler.safeDiscordAPI().getGatewayPing();
            String r = String.valueOf(millis);
            if (r.contains(".")) r = r.substring(0, r.indexOf(".") + 2);
            return r;
        }).register();
        new IdentifiedReplaceable(this, "bot_api", (s) -> "JDA (Java Discord API)").register();
        new IdentifiedReplaceable(this, "bot_prefix", (s) -> StreamlineDiscord.getConfig().getBotLayout().getPrefix()).register();
        new IdentifiedReplaceable(this, "bot_name", (s) -> DiscordHandler.getBotUser().getName()).register();
        new IdentifiedReplaceable(this, "bot_name_tagged", (s) -> DiscordHandler.getBotUser().getName()
                + "#" + DiscordHandler.getBotUser().getDiscriminator()).register();
        new IdentifiedReplaceable(this, "bot_author_name", (s) -> DiscordHandler.getUser(138397636955865089L).getName()).register();
        new IdentifiedReplaceable(this, "bot_author_name_tagged", (s) -> DiscordHandler.getUser(138397636955865089L).getName()
                + "#" + DiscordHandler.getUser(138397636955865089L).getDiscriminator()).register();
        new IdentifiedReplaceable(this, "bot_avatar_url", (s) -> StreamlineDiscord.getConfig().getBotLayout().getAvatarUrl()).register();
        new IdentifiedReplaceable(this, "bot_joined_guilds", (s) -> String.valueOf(DiscordHandler.getJoinedServers().size())).register();
        new IdentifiedReplaceable(this, "bot_author_avatar_url", (s) -> DiscordHandler.getUser(138397636955865089L).getAvatarUrl()).register();

        new IdentifiedReplaceable(this, "route_normal_count", (s) -> {
            return String.valueOf(RouteLoader.getLoadedRoutes().size());
        }).register();

        new IdentifiedUserReplaceable(this, "user_avatar_url", (s, u) -> ModuleUtils.replacePlaceholders(u, StreamlineDiscord.getConfig().getAvatarUrl())).register();
        new IdentifiedUserReplaceable(this, "user_verification_code", (s, u) -> DiscordHandler.getOrGetVerification(u)).register();

        new IdentifiedUserReplaceable(this, "user_name", (s, u) -> {
            VerifiedUser user = VerifiedUsers.getOrGet(u.getUuid()).orElse(null);
            if (user == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();

            long discordId = user.getDiscordId();
            if (discordId == -1L) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();

            return DiscordHandler.getUser(discordId).getName();
        }).register();
        new IdentifiedUserReplaceable(this, "user_name_tagged", (s, u) -> {
            VerifiedUser user = VerifiedUsers.getOrGet(u.getUuid()).orElse(null);
            if (user == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();

            long discordId = user.getDiscordId();
            if (discordId == -1L) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();

            return DiscordHandler.getUser(discordId).getName() + "#" + DiscordHandler.getUser(discordId).getDiscriminator();
        }).register();
    }
}
