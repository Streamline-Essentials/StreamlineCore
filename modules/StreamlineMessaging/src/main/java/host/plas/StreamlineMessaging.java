package host.plas;

import host.plas.commands.ChannelCommand;
import host.plas.commands.FriendCommand;
import host.plas.commands.MessageCommand;
import host.plas.commands.ReplyCommand;
import host.plas.configs.ChatChannelConfig;
import host.plas.configs.Configs;
import host.plas.configs.Messages;
import host.plas.database.Keeper;
import host.plas.database.MyLoader;
import host.plas.listeners.MainListener;
import host.plas.ratapi.MessagingExpansion;
import host.plas.savables.SavableChatter;
import host.plas.timers.ChatterSaver;
import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;
import singularity.modules.SimpleModule;
import singularity.utils.UserUtils;
import org.pf4j.PluginWrapper;

import java.io.File;

public class StreamlineMessaging extends SimpleModule {
    @Getter
    static StreamlineMessaging instance;

    @Getter
    static Configs configs;
    @Getter
    static Messages messages;
    @Getter
    static ChatChannelConfig chatChannelConfig;

    @Getter
    static MainListener mainListener;

    @Getter
    static File usersFolder;

    @Getter
    static ChatterSaver chatterSaver;

    @Getter
    static MessagingExpansion messagingExpansion;

    @Getter @Setter
    static Keeper keeper;

    public StreamlineMessaging(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void onLoad() {
        instance = this;
        usersFolder = new File(getDataFolder(), "users" + File.separator);
        usersFolder.mkdirs();
        messagingExpansion = new MessagingExpansion();
    }

    @Override
    public void onEnable() {
        keeper = new Keeper();

        configs = new Configs();
        messages = new Messages();
        chatChannelConfig = new ChatChannelConfig();

        chatterSaver = new ChatterSaver();

        mainListener = new MainListener();
        getMessagingExpansion().init();

        CosmicSender console = UserUtils.getConsole();
        if (console == null) {
            logSevere("Console is null");
        } else MyLoader.getInstance().getConsole();

        new ChannelCommand().register();
        new MessageCommand().register();
        new ReplyCommand().register();
        new FriendCommand().register();
    }

    @Override
    public void onDisable() {
        MyLoader.getInstance().getLoaded().forEach(SavableChatter::unregister);
    }
}
