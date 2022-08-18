package net.streamline.platform.users;

import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.base.Streamline;

import java.util.ArrayList;
import java.util.List;

public class SavableConsole extends SavableUser implements StreamlineConsole {
    public List<String> savedKeys = new ArrayList<>();

    public SavableConsole() {
        super("%");

        if (this.uuid == null) return;
        if (this.uuid.equals("")) return;
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
