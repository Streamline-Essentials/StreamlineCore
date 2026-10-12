package host.plas.linkparser;

import gg.drak.thebase.events.BaseEventHandler;
import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.modules.ModuleUtils;
import singularity.modules.SimpleModule;

/**
 * Makes links in chat clickable, on every platform, for players allowed to post them.
 */
public class LinkParser extends SimpleModule {
    @Getter @Setter
    private static LinkParser instance;
    @Getter @Setter
    private static LinkParserConfig config;
    @Getter @Setter
    private static ChatListener listener;

    public LinkParser(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void onEnable() {
        instance = this;
        config = new LinkParserConfig();
        listener = new ChatListener();
        ModuleUtils.listen(listener, this);
    }

    @Override
    public void onDisable() {
        if (listener != null) BaseEventHandler.unbake(listener);
        listener = null;
    }
}
