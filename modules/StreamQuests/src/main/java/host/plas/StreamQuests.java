package host.plas;

import host.plas.adapters.spigot.SpigotAdapter;
import host.plas.commands.GrantCommand;
import host.plas.commands.ReloadCommand;
import host.plas.commands.RevokeCommand;
import host.plas.configs.QuestConfig;
import host.plas.database.Keeper;
import host.plas.database.MyLoader;
import host.plas.events.QuestsListener;
import host.plas.ratapi.QuestExpansion;
import lombok.Getter;
import lombok.Setter;
import singularity.modules.SimpleModule;
import org.pf4j.PluginWrapper;

public class StreamQuests extends SimpleModule {
    @Getter @Setter
    private static StreamQuests instance; // This will be used to access the module instance from anywhere in the plugin.

    @Getter @Setter
    private static QuestConfig exampleConfig; // This will be used to access the config instance from anywhere in the plugin.
    @Getter @Setter
    private static QuestsListener questsListener; // This will be used to access the listener instance from anywhere in the plugin.
    @Getter @Setter
    private static QuestExpansion exampleExpansion; // This will be used to access the expansion instance from anywhere in the plugin.

    @Getter @Setter
    private static Keeper keeper; // This will be used to access the database instance from anywhere in the plugin.
    @Getter @Setter
    private static MyLoader loader; // This will be used to access the loader instance from anywhere in the plugin.

    @Getter @Setter
    private static SpigotAdapter spigotAdapter;

    public StreamQuests(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void registerCommands() {
    }

    @Override
    public void onLoad() {
        instance = this; // Set the instance to this module upon enabling.
    }

    @Override
    public void onEnable() {
        exampleConfig = new QuestConfig(); // Initialize the config.

        keeper = new Keeper(); // Initialize the database.
        loader = new MyLoader(); // Initialize the loader.

        spigotAdapter = new SpigotAdapter();

        questsListener = new QuestsListener(); // Initialize the listener.

        exampleExpansion = new QuestExpansion(); // Initialize the expansion.

        new GrantCommand().register();
        new ReloadCommand().register();
        new RevokeCommand().register();
    }

    @Override
    public void onDisable() {
        getLoader().getLoaded().forEach(qp -> {
            qp.saveAndUnload(false);
        });
    }
}
