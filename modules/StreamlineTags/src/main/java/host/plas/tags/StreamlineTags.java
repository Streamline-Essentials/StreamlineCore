package host.plas.tags;

import gg.drak.thebase.events.BaseEventHandler;
import host.plas.tags.commands.TagCommand;
import host.plas.tags.commands.TagsCommand;
import host.plas.tags.data.TagStore;
import lombok.Getter;
import org.pf4j.PluginWrapper;
import singularity.data.players.CosmicPlayer;
import singularity.modules.SimpleModule;
import singularity.scheduler.ModuleRunnable;
import singularity.utils.UserUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Chat tags: staff create tags and tag groups with {@code /tag}, players pick their active tag
 * from the {@code /tags} menu, and chat formats show it through {@code %streamlinetags_tag%},
 * {@code %streamlinetags_tag_left%} and {@code %streamlinetags_tag_right%}.
 */
public class StreamlineTags extends SimpleModule {
    @Getter
    private static StreamlineTags instance;
    @Getter
    private static TagsConfig mainConfig;

    private TagsListener listener;
    private TagsExpansion expansion;
    private ModuleRunnable refreshTimer;

    public StreamlineTags(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void onEnable() {
        instance = this;
        mainConfig = new TagsConfig();

        TagStore.start();
        TagStore.ensureTables();
        TagStore.loadDefinitions();

        listener = new TagsListener();
        expansion = new TagsExpansion();

        for (CosmicPlayer player : UserUtils.getOnlinePlayers().values()) {
            String uuid = player.getUuid();
            CompletableFuture.runAsync(() -> TagStore.loadPlayer(uuid));
        }

        restartRefresh();
    }

    @Override
    public void onDisable() {
        if (refreshTimer != null) refreshTimer.cancel();
        refreshTimer = null;
        if (expansion != null) expansion.stop();
        if (listener != null) BaseEventHandler.unbake(listener);
        TagStore.stop();
    }

    @Override
    public void registerCommands() {
        setCommands(List.of(new TagsCommand(), new TagCommand()));
    }

    /** Re-reads tags and groups from the database every {@code refresh-interval-seconds}. */
    public void restartRefresh() {
        if (refreshTimer != null) refreshTimer.cancel();
        refreshTimer = null;
        int seconds = mainConfig.getRefreshIntervalSeconds();
        if (seconds <= 0) return;

        refreshTimer = new ModuleRunnable(this, seconds * 20L, seconds * 20L) {
            @Override
            public void run() {
                CompletableFuture.runAsync(TagStore::loadDefinitions);
            }
        };
    }

    public static String message(String key) {
        return mainConfig.getMessage(key);
    }
}
