package host.plas.configs;

import gg.drak.thebase.storage.StorageUtils;
import singularity.configs.ModularizedConfig;
import host.plas.StreamlineGroups;

import java.util.HashMap;

public class Configs extends ModularizedConfig {
    public Configs() {
        super(StreamlineGroups.getInstance(), "config.yml", true);
        init();
    }

    public void init() {
        if (getResource().contains("groups.saving.databases")) {
            getResource().remove("groups.saving.databases");
        }
        if (getResource().contains("grouped-users.saving.databases")) {
            getResource().remove("grouped-users.saving.databases");
        }

        getOrSetDefault("groups.base.maximum", new HashMap<>());
        getOrSetDefault("groups.party.maximum", new HashMap<>());
        getOrSetDefault("groups.guild.maximum", new HashMap<>());

        inviteTimeout();

        announceLevelChangeTitle();
        announceLevelChangeChat();
        guildPayoutExperienceAmount();
        guildPayoutExperienceEvery();
        guildStartingLevel();
        guildLevelingEquation();
    }

    public int baseMax(String group) {
        reloadResource();

        if (! getResource().singleLayerKeySet("groups.base.maximum").contains(group)) group = "default";
        if (! getResource().singleLayerKeySet("groups.base.maximum").contains(group)) return 8;

        return getResource().getInt("groups.base.maximum." + group);
    }

    public long inviteTimeout() {
        reloadResource();

        return getResource().getLong("groups.base.invites.timeout");
    }

    public int partyMax(String group) {
        reloadResource();

        if (! getResource().singleLayerKeySet("groups.party.maximum").contains(group)) group = "default";
        if (! getResource().singleLayerKeySet("groups.party.maximum").contains(group)) return 8;
        if (getResource().getInt("groups.party.maximum." + group) == -1) baseMax(group);

        return getResource().getInt("groups.party.maximum." + group);
    }

    public int guildMax(String group) {
        reloadResource();

        if (! getResource().singleLayerKeySet("groups.guild.maximum").contains(group)) group = "default";
        if (! getResource().singleLayerKeySet("groups.guild.maximum").contains(group)) return 8;
        if (getResource().getInt("groups.guild.maximum." + group) == -1) baseMax(group);

        return getResource().getInt("groups.guild.maximum." + group);
    }

    public boolean announceLevelChangeTitle() {
        reloadResource();

        return getResource().getBoolean("groups.guild.experience.announce.level-change.title");
    }

    public boolean announceLevelChangeChat() {
        reloadResource();

        return getResource().getBoolean("groups.guild.experience.announce.level-change.chat");
    }

    public double guildPayoutExperienceAmount() {
        reloadResource();

        return getResource().getOrSetDefault("groups.guild.experience.payout.amount", 1.0D);
    }

    public int guildPayoutExperienceEvery() {
        reloadResource();

        return getResource().getOrSetDefault("groups.guild.experience.payout.every", 400);
    }

    public int guildStartingLevel() {
        reloadResource();

        return getResource().getOrSetDefault("groups.guild.experience.starting.level", 1);
    }

    public double guildStartingExperienceAmount() {
        reloadResource();

        return getResource().getOrSetDefault("groups.guild.experience.starting.xp", 0.0D);
    }

    public String guildLevelingEquation() {
        reloadResource();

        return getResource().getString("groups.guild.experience.equation");
    }

    public StorageUtils.SupportedStorageType getSavingUseUsers() {
        reloadResource();

        return getResource().getEnum("groups.saving.use", StorageUtils.SupportedStorageType.class);
    }
}
