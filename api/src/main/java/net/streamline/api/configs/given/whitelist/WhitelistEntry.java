package net.streamline.api.configs.given.whitelist;

import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;

import java.util.Date;

public record WhitelistEntry(String whitelistedUuid, Date whitelistedAt, String whitelistedBy) {
    public void add() {
        GivenConfigs.getWhitelistConfig().addEntry(this);
    }

    public void remove() {
        GivenConfigs.getWhitelistConfig().removeEntry(this);
    }

    public boolean applied() {
        return GivenConfigs.getWhitelistConfig().isEntryApplied(whitelistedUuid());
    }
}
