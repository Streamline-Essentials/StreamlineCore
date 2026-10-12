package host.plas.tags;

import singularity.configs.ModularizedConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code config.yml}: the refresh interval, the menu title and every message.
 */
public class TagsConfig extends ModularizedConfig {

    public TagsConfig() {
        super(StreamlineTags.getInstance(), "config.yml", true);
        init();
    }

    @Override
    public void init() {
        getRefreshIntervalSeconds();
        getMenuTitle();
    }

    public int getRefreshIntervalSeconds() {
        reloadResource();
        return Math.max(0, getOrSetDefault("refresh-interval-seconds", 60));
    }

    public String getMenuTitle() {
        reloadResource();
        return getOrSetDefault("menu.title", "#38C1FCᴄʜᴀᴛ ᴛᴀɢѕ");
    }

    public String getMessage(String key) {
        reloadResource();
        String message = getResource().getString("messages." + key);
        return message == null ? "" : message;
    }

    public List<String> getMessageLines(String key) {
        reloadResource();
        List<String> lines = getResource().getStringList("messages." + key);
        if (lines == null || lines.isEmpty()) {
            lines = new ArrayList<>();
            String single = getResource().getString("messages." + key);
            if (single != null && ! single.isEmpty()) lines.add(single);
        }
        return lines;
    }
}
