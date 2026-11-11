package host.plas;

import host.plas.database.DiscordMiddleware;
import host.plas.database.VerifiedUserKeeper;
import host.plas.depends.GroupsDependency;
import host.plas.discord.data.channeling.EndPointLoader;
import host.plas.discord.data.channeling.RouteLoader;
import host.plas.discord.data.verified.VerifiedUserLoader;
import host.plas.timers.VerifierTimer;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import org.pf4j.PluginWrapper;
import host.plas.bukkit.BukkitAdapter;
import host.plas.command.CreateChannelCommandMC;
import host.plas.command.StreamlineDiscordCommandMC;
import host.plas.command.UnVerifyCommandMC;
import host.plas.command.VerifyCommandMC;
import host.plas.config.Config;
import host.plas.config.Messages;
import host.plas.config.Messaging;
import host.plas.config.VerifiedUsers;
import host.plas.database.EndPointKeeper;
import host.plas.database.RouteKeeper;
import host.plas.depends.MessagingDependency;
import host.plas.discord.DiscordHandler;
import host.plas.discord.data.events.DiscordEventMessages;
import host.plas.discord.data.events.relay.EventRelay;
import host.plas.discord.messaging.BotMessageMigration;
import host.plas.discord.messaging.BotMessages;
import host.plas.events.MainListener;
import host.plas.placeholders.DiscordExpansion;
import singularity.interfaces.ISingularityExtension;
import singularity.modules.SimpleModule;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class StreamlineDiscord extends SimpleModule {
    @Getter @Setter
    private static StreamlineDiscord instance;

    @Getter @Setter
    private static Config config;
    @Getter @Setter
    private static Messages messages;
    @Getter @Setter
    private static Messaging messaging;
    @Getter @Setter
    private static VerifiedUsers verifiedUsers;

    @Getter @Setter
    private static GroupsDependency groupsDependency;
    @Getter @Setter
    private static MessagingDependency messagingDependency;

    @Getter @Setter
    private static DiscordExpansion discordExpansion;
    @Getter @Setter
    private static MainListener mainListener;

    @Getter @Setter
    private static RouteKeeper routeKeeper;
    @Getter @Setter
    private static EndPointKeeper endPointKeeper;
    @Getter @Setter
    private static VerifiedUserKeeper verifiedUserKeeper;

    @Getter @Setter
    private static RouteLoader routeLoader;
    @Getter @Setter
    private static EndPointLoader endPointLoader;
    @Getter @Setter
    private static VerifiedUserLoader verifiedUserLoader;

    @Getter @Setter
    private static VerifierTimer verifierTimer;
    @Getter @Setter
    private static DiscordMiddleware discordMiddleware;

    public StreamlineDiscord(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        writeHelpGuide();

        setGroupsDependency(new GroupsDependency());
        setMessagingDependency(new MessagingDependency());

        setConfig(new Config());
        setMessages(new Messages());
        setMessaging(new Messaging());
        BotMessageMigration.run();
        BotMessages.registerDefaults();
        setVerifiedUsers(new VerifiedUsers());

        setRouteKeeper(new RouteKeeper());
        setEndPointKeeper(new EndPointKeeper());
        setVerifiedUserKeeper(new VerifiedUserKeeper());

        setRouteLoader(new RouteLoader());
        setEndPointLoader(new EndPointLoader());
        setVerifiedUserLoader(new VerifiedUserLoader());

        // A server whose bot is off or has no token (a backend relaying to the proxy) needs no guild.
        if (! EventRelay.ownBotDisabled() && getConfig().getBotLayout().getMainGuildId() == 0L) {
            logWarning("&bWARNING!&r &dYou need to set the main guild ID in the config.yml file for the Discord Streamline module!&r%newline%&cDisabling the Discord Streamline module...&r");
            super.stop();
            return;
        }

        // Every route, endpoint and verified-user save is queued on the middleware, so it
        // must exist before anything below can fail and leave those saves with nowhere to go.
        setDiscordMiddleware(new DiscordMiddleware());

        RouteLoader.loadAllRoutes();

        setDiscordExpansion(new DiscordExpansion());
        getDiscordExpansion().init();

        DiscordHandler.init(); // init async

        setMainListener(new MainListener());

        EventRelay.start();

        // A proxy loads modules while it initialises and its start event cannot be relied on to reach
        // them, so it counts as started once the module is up; backends wait for the server start event.
        if (DiscordEventMessages.isProxy()) DiscordEventMessages.onServerStarted();

        new VerifyCommandMC().register();
        new CreateChannelCommandMC().register();
        new UnVerifyCommandMC().register();
        new StreamlineDiscordCommandMC().register();

        // Bukkit classes exist only on Spigot; mod-loader backends are not proxies either.
        if (SLAPI.getInstance().getPlatform().getPlatformType() == ISingularityExtension.PlatformType.SPIGOT) {
            BukkitAdapter.init();
        }

        setVerifierTimer(new VerifierTimer());
    }

    public static final String HELP_GUIDE = "NEED-HELP-START-HERE.md";

    /**
     * Copies the shipped setup guide into the data folder whenever it is missing or differs
     * from the jar's copy. It runs before anything that can stop the module (such as an unset
     * main guild id), so a first start always leaves the guide beside config.yml.
     */
    private void writeHelpGuide() {
        try (InputStream in = getWrapper().getPluginClassLoader().getResourceAsStream(HELP_GUIDE)) {
            if (in == null) return;
            byte[] shipped = in.readAllBytes();

            File folder = getDataFolder();
            if (! folder.exists()) folder.mkdirs();

            Path target = new File(folder, HELP_GUIDE).toPath();
            if (Files.isRegularFile(target) && Arrays.equals(Files.readAllBytes(target), shipped)) return;

            Files.write(target, shipped);
        } catch (Exception e) {
            logWarning("Could not write " + HELP_GUIDE + " to the module folder: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        // Waits (up to 5 seconds) for Discord to take the offline message: killing the bot cancels
        // requests still queued.
        DiscordEventMessages.announceStop();
        DiscordEventMessages.shutdown();
        // Its thread, Redis connection and plugin-message listener would outlive an unloaded module.
        EventRelay.stop();

        // The middleware writes in batches every 5 seconds; flush whatever is still queued.
        if (getDiscordMiddleware() != null) {
            getDiscordMiddleware().run();
            getDiscordMiddleware().cancel();
        }
        // Timers live in the core scheduler and would outlive an unloaded module.
        if (getVerifierTimer() != null) getVerifierTimer().cancel();

        DiscordHandler.kill().completeOnTimeout(false, 7, TimeUnit.SECONDS).join();
        if (getDiscordExpansion() != null) getDiscordExpansion().stop();
    }
}
