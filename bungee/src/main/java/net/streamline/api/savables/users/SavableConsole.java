package net.streamline.api.savables.users;

import net.streamline.base.Streamline;

import java.util.ArrayList;
import java.util.List;

public class SavableConsole extends SavableUser {
    public List<String> savedKeys = new ArrayList<>();

    public SavableConsole() {
        super("%");

        if (this.uuid == null) return;
        if (this.uuid.equals("")) return;
    }

    @Override
    public List<String> getTagsFromConfig(){
        return Streamline.getMainConfig().userConsoleDefaultTags();
    }

    @Override
    public void populateMoreDefaults() {
        latestName = getOrSetDefault("profile.latest.name", Streamline.getMainConfig().userConsoleNameRegular());
        displayName = getOrSetDefault("profile.display-name", Streamline.getMainConfig().userConsoleNameFormatted());
    }

    @Override
    public void loadMoreValues() {

    }

    @Override
    public void saveMore() {

    }
}
