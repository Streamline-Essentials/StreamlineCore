package net.streamline.api.configs.given.whitelist;

import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;

import java.util.Date;

public class WhitelistEntry {
    final String whitelistedUuid;
    final Date whitelistedAt;
    final String whitelistedBy;

    public WhitelistEntry(String whitelistedUuid, Date whitelistedAt, String whitelistedBy) {
        this.whitelistedUuid = whitelistedUuid;
        this.whitelistedAt = whitelistedAt;
        this.whitelistedBy = whitelistedBy;
    }

    public String whitelistedUuid() {
        return whitelistedUuid;
    }

    public Date whitelistedAt() {
        return whitelistedAt;
    }

    public String whitelistedBy() {
        return whitelistedBy;
    }

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
