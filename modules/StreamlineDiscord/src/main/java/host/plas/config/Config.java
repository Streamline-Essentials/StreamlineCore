package host.plas.config;

import host.plas.discord.data.BotLayout;
import net.dv8tion.jda.api.entities.Activity;
import host.plas.StreamlineDiscord;
import singularity.configs.ModularizedConfig;

import java.util.ArrayList;
import java.util.List;

public class Config extends ModularizedConfig {
    public Config() {
        super(StreamlineDiscord.getInstance(), "config.yml", true);
        init();
    }

    @Override
    public void init() {
        fullDisable();

        getBotLayout();
        getAvatarUrl();

        getDefaultFormatFromMinecraft();
        getDefaultFormatFromDiscord();

        allowStreamlineChannelsToDiscord();
        allowStreamlineGuildsToDiscord();
        allowStreamlinePartiesToDiscord();

        allowDiscordToStreamlineChannels();
        allowDiscordToStreamlineGuilds();
        allowDiscordToStreamlineParties();

        serverEventAllEventsOnDiscordRoute();

        serverEventStreamlineLogin();
        serverEventStreamlineLogout();

        serverEventSpigotAdvancement();
        serverEventSpigotDeath();

        moduleForwardsEventsToProxy();

        verificationResponsesPrivate();

        verificationEventVerifiedMinecraftEnabled();
        verificationEventVerifiedCommandsList();

        verificationEventVerifiedDiscordEnabled();
        verificationEventVerifiedDiscordRoles();

        verificationEventUnVerifiedMinecraftEnabled();
        verificationEventUnVerifiedCommandsList();

        verificationEventUnVerifiedDiscordEnabled();
        verificationEventUnVerifiedDiscordRoles();
    }

    public boolean fullDisable() {
        reloadResource();

        return getResource().getOrSetDefault("bot.full-disable", false);
    }

    public BotLayout getBotLayout() {
        String token = getOrSetDefault("bot.token", "<put token here -- DO NOT GIVE THIS TO ANYONE>");
        String prefix = getOrSetDefault("bot.prefix", ">>");
        Activity.ActivityType activityType;
        try {
            activityType = Activity.ActivityType.valueOf(getOrSetDefault("bot.activity.type", Activity.ActivityType.WATCHING.toString()));
        } catch (Exception e) {
            activityType = Activity.ActivityType.WATCHING;
        }
        String activityValue = getOrSetDefault("bot.activity.value", "**" + prefix + "help** for help!");
        String avatarUrl = getOrSetDefault("bot.avatar-url", "https://raw.githubusercontent.com/Streamline-Essentials/StreamlineWiki/main/s.png");
        boolean slashCommandsEnabled = getOrSetDefault("bot.slash-commands", true);
        long mainGuildId = getOrSetDefault("bot.main-guild-id", 0L);

        return new BotLayout(token, prefix, activityType, activityValue, avatarUrl, slashCommandsEnabled, mainGuildId);
    }

    public void saveBotLayout(BotLayout layout) {
        write("bot.token", layout.getToken());
        write("bot.prefix", layout.getPrefix());
        write("bot.activity.type", layout.getActivityType().toString());
        write("bot.activity.value", layout.getActivityValue());
        write("bot.avatar-url", layout.getAvatarUrl());
    }

    public String getAvatarUrl() {
        reloadResource();

        return getOrSetDefault("messaging.avatar-url", "https://minotar.net/helm/%streamline_user_uuid%/1024.png");
    }

    public String getDefaultFormatFromMinecraft() {
        reloadResource();

        return getOrSetDefault("messaging.default-format.from-minecraft", "%streamline_user_absolute%: %this_message%");
    }

    public String getDefaultFormatFromDiscord() {
        reloadResource();

        return getOrSetDefault("messaging.default-format.from-discord", "&8[&9&lDiscord&8] &d%streamline_user_absolute% &9>> &r%this_message%");
    }

    public boolean allowStreamlineChannelsToDiscord() {
        reloadResource();

        return getOrSetDefault("messaging.to-discord.streamline-channels", true) && StreamlineDiscord.getMessagingDependency().isPresent();
    }

    public boolean allowDiscordToStreamlineChannels() {
        reloadResource();

        return getOrSetDefault("messaging.to-minecraft.streamline-channels", true) && StreamlineDiscord.getMessagingDependency().isPresent();
    }

    public boolean allowStreamlineGuildsToDiscord() {
        reloadResource();

        return getOrSetDefault("messaging.to-discord.streamline-guilds", true)/* && DiscordModule.getGroupsDependency().isPresent()*/;
    }

    public boolean allowDiscordToStreamlineGuilds() {
        reloadResource();

        return getOrSetDefault("messaging.to-minecraft.streamline-guilds", true)/* && DiscordModule.getGroupsDependency().isPresent()*/;
    }

    public boolean allowStreamlinePartiesToDiscord() {
        reloadResource();

        return getOrSetDefault("messaging.to-discord.streamline-parties", true)/* && DiscordModule.getGroupsDependency().isPresent()*/;
    }

    public boolean allowDiscordToStreamlineParties() {
        reloadResource();

        return getOrSetDefault("messaging.to-minecraft.streamline-parties", true)/* && DiscordModule.getGroupsDependency().isPresent()*/;
    }

    public boolean serverEventAllEventsOnDiscordRoute() {
        reloadResource();

        return getOrSetDefault("server-events.add-all-events-on-discord-route-creation", true);
    }

    public boolean serverEventStreamlineLogin() {
        reloadResource();

        return getOrSetDefault("server-events.streamline.login", true);
    }

    public boolean serverEventStreamlineLogout() {
        reloadResource();

        return getOrSetDefault("server-events.streamline.logout", true);
    }

    public boolean serverEventSpigotAdvancement() {
        reloadResource();

        return getOrSetDefault("server-events.spigot.advancement", true);
    }

    public boolean serverEventSpigotDeath() {
        reloadResource();

        return getOrSetDefault("server-events.spigot.death", true);
    }

    public boolean moduleForwardsEventsToProxy() {
        reloadResource();

        return getOrSetDefault("module.forward-events-to-proxy", true);
    }

    public boolean verificationOnlyCommand() {
        reloadResource();

        return getOrSetDefault("verification.only-command", true);
    }

    public boolean verificationResponsesPrivate() {
        reloadResource();

        return getOrSetDefault("verification.response.private-thread", true);
    }

    public boolean verificationEventVerifiedMinecraftEnabled() {
        reloadResource();

        return getOrSetDefault("verification.events.verified.minecraft.enabled", false);
    }

    public List<String> verificationEventVerifiedCommandsList() {
        reloadResource();

        return getOrSetDefault("verification.events.verified.minecraft.commands",
                List.of("luckperms user %streamline_user_absolute% parent add verified"));
    }

    public boolean verificationEventVerifiedDiscordEnabled() {
        reloadResource();

        return getOrSetDefault("verification.events.verified.discord.enabled", false);
    }

    public List<Long> verificationEventVerifiedDiscordRoles() {
        reloadResource();

        List<String> r;
        r = getResource().getStringList("verification.events.verified.discord.roles-to-add");
        if (r.isEmpty()) {
            r.add("000000000000000000");
            r.add("000000000000000000");
            getResource().set("verification.events.verified.discord.roles-to-add", r);
        }

        List<Long> roles = new ArrayList<>();
        for (String s : r) {
            try {
                roles.add(Long.parseLong(s));
            } catch (Exception e) {
                // do nothing
            }
        }

        return roles;
    }

    public boolean verificationEventUnVerifiedMinecraftEnabled() {
        reloadResource();

        return getOrSetDefault("verification.events.unverified.minecraft.enabled", false);
    }

    public List<String> verificationEventUnVerifiedCommandsList() {
        reloadResource();

        return getOrSetDefault("verification.events.unverified.minecraft.commands",
                List.of("luckperms user %streamline_user_absolute% parent add verified"));
    }

    public boolean verificationEventUnVerifiedDiscordEnabled() {
        reloadResource();

        return getOrSetDefault("verification.events.unverified.discord.enabled", false);
    }

    public List<Long> verificationEventUnVerifiedDiscordRoles() {
        reloadResource();

        List<String> r;
        r = getResource().getStringList("verification.events.unverified.discord.roles-to-remove");
        if (r.isEmpty()) {
            r.add("000000000000000000");
            r.add("000000000000000000");
            getResource().set("verification.events.unverified.discord.roles-to-remove", r);
        }

        List<Long> roles = new ArrayList<>();
        for (String s : r) {
            try {
                roles.add(Long.parseLong(s));
            } catch (Exception e) {
                // do nothing
            }
        }

        return roles;
    }
}
