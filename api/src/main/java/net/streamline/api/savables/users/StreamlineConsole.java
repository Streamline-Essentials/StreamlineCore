package net.streamline.api.savables.users;

import net.streamline.api.configs.given.GivenConfigs;

import java.util.ArrayList;
import java.util.List;

public class StreamlineConsole extends StreamlineUser {
    public List<String> savedKeys = new ArrayList<>();

    public StreamlineConsole() {
        super("%", null);

        if (this.getUuid() == null) return;
        if (this.getUuid().isEmpty()) return;
    }

    @Override
    public List<String> getTagsFromConfig(){
        return GivenConfigs.getMainConfig().userConsoleDefaultTags();
    }

    @Override
    public void populateMoreDefaults() {
        setLatestName(getOrSetDefault("profile.latest.name", GivenConfigs.getMainConfig().userConsoleNameRegular()));
        setDisplayName(getOrSetDefault("profile.display-name", GivenConfigs.getMainConfig().userConsoleNameFormatted()));
    }

    @Override
    public void loadMoreValues() {

    }

    @Override
    public void saveMore() {

    }
}
