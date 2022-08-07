package net.streamline.platform.users;

import net.streamline.api.SLAPI;
import net.streamline.api.savables.users.StreamlineConsole;

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
        return SLAPI.getInstance().getPlatform().getMainConfig().userConsoleDefaultTags();
    }

    @Override
    public void populateMoreDefaults() {
        setLatestName(getOrSetDefault("profile.latest.name", SLAPI.getInstance().getPlatform().getMainConfig().userConsoleNameRegular()));
        setDisplayName(getOrSetDefault("profile.display-name", SLAPI.getInstance().getPlatform().getMainConfig().userConsoleNameFormatted()));
    }

    @Override
    public void loadMoreValues() {

    }

    @Override
    public void saveMore() {

    }
}
