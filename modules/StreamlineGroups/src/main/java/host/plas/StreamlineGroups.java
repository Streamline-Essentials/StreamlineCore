package host.plas;

import host.plas.commands.PCCommand;
import host.plas.commands.PartyCommand;
import host.plas.configs.Configs;
import host.plas.configs.DefaultRoles;
import host.plas.configs.Messages;
import host.plas.database.PlayerKeeper;
import host.plas.database.PlayerLoader;
import host.plas.listeners.MainListener;
import host.plas.placeholders.GroupsExpansion;
import host.plas.data.GroupManager;
import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.modules.ModuleUtils;
import singularity.modules.SimpleModule;

import java.io.File;

public class StreamlineGroups extends SimpleModule {
    @Getter @Setter
    private static StreamlineGroups instance;

    @Getter @Setter
    private static File usersFolder;
    @Getter @Setter
    private static File groupsFolder;

    @Getter @Setter
    private static Configs configs;
    @Getter @Setter
    private static Messages messages;
    @Getter @Setter
    private static DefaultRoles defaultRoles;

    @Getter @Setter
    private static MainListener mainListener;

    @Getter @Setter
    private static GroupsExpansion groupsExpansion;

    @Getter @Setter
    private static PlayerKeeper playerKeeper;
    @Getter @Setter
    private static PlayerLoader playerLoader;

    public StreamlineGroups(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void onLoad() {
        instance = this;
        groupsExpansion = new GroupsExpansion();
    }

    @Override
    public void onEnable() {
        configs = new Configs();
        messages = new Messages();
        defaultRoles = new DefaultRoles();

        usersFolder = new File(getDataFolder(), "users" + File.separator);
        groupsFolder = new File(getDataFolder(), "groups" + File.separator);
        usersFolder.mkdirs();
        groupsFolder.mkdirs();

        mainListener = new MainListener();
        ModuleUtils.listen(mainListener, this);

        getGroupsExpansion().init();

        new PartyCommand(this).register();
        new PCCommand().register();

        playerKeeper = new PlayerKeeper();
        playerLoader = new PlayerLoader();
    }

    @Override
    public void onDisable() {
        GroupManager.getLoadedParties().forEach((party) -> {
            GroupManager.disbandParty(party.getOwner(), party.getOwner());
        });

        GroupManager.getLoadedParties().clear();

        getGroupsExpansion().stop();
    }
}
