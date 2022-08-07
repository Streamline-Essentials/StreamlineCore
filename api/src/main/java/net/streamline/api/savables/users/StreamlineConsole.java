package net.streamline.api.savables.users;

import java.util.List;

public interface StreamlineConsole extends StreamlineUser {

    @Override
    List<String> getTagsFromConfig();

    @Override
    void populateMoreDefaults();

    @Override
    void loadMoreValues();

    @Override
    void saveMore();
}
