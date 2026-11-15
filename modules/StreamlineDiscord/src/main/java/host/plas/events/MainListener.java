package host.plas.events;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseEventPriority;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.discord.data.channeling.EndPointType;
import host.plas.discord.data.channeling.RouteLoader;
import host.plas.discord.data.channeling.RoutedUser;
import host.plas.discord.data.events.EventClassifier;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import host.plas.StreamlineDiscord;
import host.plas.discord.DiscordCommand;
import host.plas.discord.DiscordHandler;
import host.plas.discord.messaging.BotMessageConfig;
import host.plas.discord.messaging.DiscordMessenger;
import host.plas.discord.messaging.DiscordProxiedMessage;
import singularity.data.console.CosmicSender;
import singularity.events.player.CosmicDeathEvent;
import singularity.events.server.CosmicChatEvent;
import singularity.events.server.ServerStartEvent;
import singularity.interfaces.ISingularityExtension;
import net.streamline.api.SLAPI;
import host.plas.discord.data.events.DiscordEventMessages;
import host.plas.discord.messaging.BotMessages;
import singularity.events.server.LoginCompletedEvent;
import singularity.events.server.LogoutEvent;
import singularity.messages.events.ProxiedMessageEvent;
import singularity.messages.events.ProxyMessageInEvent;
import singularity.messages.proxied.ProxiedMessage;
import singularity.modules.ModuleUtils;
import singularity.objects.SingleSet;
import singularity.utils.UserUtils;

import host.plas.events.streamline.bot.command.DiscordCommandEvent;
import host.plas.events.streamline.bot.posting.DiscordMessageEvent;
import host.plas.events.streamline.proxy.SimpleDiscordPMessageReceivedEvent;
import host.plas.events.streamline.verification.off.UnVerificationSuccessEvent;
import host.plas.events.streamline.verification.on.VerificationSuccessEvent;

public class MainListener implements BaseEventListener {
    public MainListener() {
        ModuleUtils.listen(this, StreamlineDiscord.getInstance());
        StreamlineDiscord.getInstance().logInfo(getClass().getSimpleName() + " is now registered!");
    }

    /**
     * Runs at {@link BaseEventPriority#NORMAL}: after mutes and channels (which cancel what they
     * take), before chat formatters such as TacoEssentials' (at {@code HIGH}), which cancel the
     * event to broadcast their own line.
     */
    @BaseProcessor(priority = BaseEventPriority.NORMAL)
    public void onStreamlineMessage(CosmicChatEvent event) {
        if (event.isCanceled()) return;
        if (ModuleUtils.isCommand(event.getMessage())) return;
        RouteLoader.getLoadedRoutes().forEach(route -> {
            switch (route.getInput().getType()) {
                case GLOBAL_NATIVE:
                    route.bounceMessage(new RoutedUser(event.getSender()), event.getMessage());
                    break;
                case SPECIFIC_NATIVE:
                    if (event.getSender().getServerName().equals(route.getInput().getEndPointIdentifier())) {
                        route.bounceMessage(
                                new RoutedUser(event.getSender()), event.getMessage());
                    }
                    break;
                case PERMISSION:
                    if (ModuleUtils.hasPermission(event.getSender(), route.getInput().getEndPointIdentifier())) {
                        route.bounceMessage(
                                new RoutedUser(event.getSender()), event.getMessage());
                    }
                    break;
            }
        });
    }

    @BaseProcessor
    public void onMessage(DiscordMessageEvent event) {
        if (event instanceof DiscordCommandEvent) return;

        if (event.getMessage().getAuthor().isBot()) return;

        if (! event.getMessage().hasPrefix()/* || ! (StreamlineDiscord.getConfig().getBotLayout().isSlashCommandsEnabled() && event.getMessage().hasSlashPrefix())*/) {
            if (! StreamlineDiscord.getConfig().verificationOnlyCommand()) {
                if (DiscordHandler.hasVerification(event.getMessage().getTotalMessage())) {
                    SingleSet<MessageCreateData, BotMessageConfig> data =
                            DiscordHandler.tryVerificationForUser(event.getMessage(), event.getMessage().getTotalMessage(), false);

                    if (data != null) DiscordMessenger.message(event.getMessage().getChannel().getIdLong(), data.getKey(), data.getValue());
                }
            }
            RouteLoader.getLoadedRoutes().forEach(route -> {
                if (route.getInput().getType().equals(EndPointType.DISCORD_TEXT) && route.getInput().getEndPointIdentifier().equals(event.getMessage().getChannel().getId())) {
                    route.bounceMessage(new RoutedUser(event.getMessage().getAuthor().getIdLong()), event.getMessage().getTotalMessage());
                }
            });
        } else {
            DiscordCommand command = DiscordHandler.getCommandByAlias(event.getMessage().getBase());
            if (command == null) {
                StreamlineDiscord.getInstance().logDebug("Could not get DiscordCommand with alias of '" + event.getMessage().getBase() + "'.");
                return;
            }

            ModuleUtils.fireEvent(new DiscordCommandEvent(event.getMessage(), command));
        }
    }

    @BaseProcessor
    public void onCommand(DiscordCommandEvent event) {
        StreamlineDiscord.getInstance().logDebug("Executing command '" + event.getCommand().getCommandIdentifier() + "'...!");
        try {
            SingleSet<MessageCreateData, BotMessageConfig> reply = event.getCommand().execute(event.getMessage());
            if (reply == null) return;

            long channelId = event.getMessage().getChannel().getIdLong();
            // A private reply goes to a thread only its author is added to, like verification messages.
            if (event.getCommand().isReplyEphemeral()) {
                DiscordMessenger.message(channelId, reply.getKey(), reply.getValue(), event.getMessage().getAuthor().getIdLong());
            } else {
                DiscordMessenger.message(channelId, reply.getKey(), reply.getValue());
            }
        } catch (Throwable t) {
            StreamlineDiscord.getInstance().logWarning("Could not answer the Discord command '" + event.getCommand().getCommandIdentifier() + "': " + t);
        }
    }

    @BaseProcessor(priority = BaseEventPriority.HIGHEST)
    public void onProxiedMessage(ProxiedMessageEvent event) {
        ProxiedMessage message = event.getMessage();

        if (message == null) return;
        if (message.getSubChannel().equals(DiscordProxiedMessage.getSelfSubChannel())) {
            StreamlineDiscord.getInstance().logDebug("Got DiscordProxiedMessage...");
            DiscordProxiedMessage discordProxiedMessage = DiscordProxiedMessage.translate(message);
            SimpleDiscordPMessageReceivedEvent ev = new SimpleDiscordPMessageReceivedEvent(discordProxiedMessage).fire();
            if (ev.isCancelled()) return;
            EndPointType type = null;
            try {
                type = EndPointType.valueOf(ev.simplyGetInputType());
            } catch (Exception e) {
                StreamlineDiscord.getInstance().logSevere("Could not parse EndPointType from a received DiscordProxyMessage...");
                return;
            }

            EndPointType finalType = type;
            RouteLoader.getLoadedRoutes().forEach(route -> {
                if (route.getInput().getType().equals(finalType) && route.getInput().getEndPointIdentifier().equals(ev.simplyGetInputIdentifier())) {
                    route.bounceMessage(new RoutedUser(UserUtils.getConsole()), ev.simplyGetMessage(),
                            ev.simplyGetMessage().startsWith("{") && ev.simplyGetMessage().endsWith("}"));
                }
            });
        }
    }

    @BaseProcessor
    public void onSimpleDiscordPMReceived(SimpleDiscordPMessageReceivedEvent event) {

    }

    @BaseProcessor
    public void onProxiedMessageReceived(ProxyMessageInEvent event) {
        if (DiscordHandler.isBackEnd()) return;

        ProxiedMessage message = event.getMessage();
        if (message == null) return;
        if (message.getSubChannel() == null) return;

        // TODO: Check if this is needed.
//        if (message.getSubChannel().equals(DiscordEventMessageBuilder.getSubChannel())) {
//            DiscordEventMessageBuilder.handle(message);
//        }
    }

    @BaseProcessor
    public void onVerificationSuccess(VerificationSuccessEvent event) {
        CosmicSender user = event.getSender().orElse(null);
        if (user == null) {
            StreamlineDiscord.getInstance().logWarning("Verified Discord ID '" + event.getMessage().getAuthor().getIdLong() + "', but the associated StreamlineUser is 'null'! Skipping...");
            return;
        }

        ModuleUtils.sendMessage(user, StreamlineDiscord.getMessages().verifySuccessMinecraft());

        try {
            if (StreamlineDiscord.getConfig().verificationEventVerifiedDiscordEnabled()) {
                Guild guild = DiscordHandler.getServerById(StreamlineDiscord.getConfig().getBotLayout().getMainGuildId());
                for (long id : StreamlineDiscord.getConfig().verificationEventVerifiedDiscordRoles()) {
                    Role r = guild.getRoleById(id);
                    if (r != null) {
                        try {
                            guild.addRoleToMember(event.getMessage().getAuthor(), r).queue();
                        } catch (Exception e) {
                            StreamlineDiscord.getInstance().logSevere("Could not add role '" + r.getName() + "' to user '" + event.getMessage().getAuthor().getAsTag() + "'! Are they an admin? (Cannot add roles to admins and server owners...)");
                        }
                    }
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (StreamlineDiscord.getConfig().verificationEventVerifiedMinecraftEnabled()) {
                for (String command : StreamlineDiscord.getConfig().verificationEventVerifiedCommandsList()) {
                    ModuleUtils.queueRunAs(user, command);
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @BaseProcessor
    public void onUnVerificationSuccess(UnVerificationSuccessEvent event) {
        CosmicSender user = event.getSender().orElse(null);
        if (user == null) {
            StreamlineDiscord.getInstance().logWarning("UnVerified Discord ID '" + event.getDiscordId() + "', but the associated StreamlineUser is 'null'! Skipping...");
            return;
        }

        ModuleUtils.sendMessage(user, StreamlineDiscord.getMessages().unVerifySuccessMinecraft());

        User u = DiscordHandler.getUser(event.getDiscordId());
        if (u == null) {
            StreamlineDiscord.getInstance().logWarning("UnVerified Discord ID '" + event.getDiscordId() + "', but the associated Discord User is 'null'! Skipping...");
            return;
        }

        try {
            if (StreamlineDiscord.getConfig().verificationEventUnVerifiedDiscordEnabled()) {
                Guild guild = DiscordHandler.getServerById(StreamlineDiscord.getConfig().getBotLayout().getMainGuildId());
                for (long id : StreamlineDiscord.getConfig().verificationEventUnVerifiedDiscordRoles()) {
                    Role r = guild.getRoleById(id);
                    if (r != null) {
                        try {
                            guild.removeRoleFromMember(u, r).queue();
                        } catch (Exception e) {
                            StreamlineDiscord.getInstance().logSevere("Could not remove role '" + r.getName() + "' to user '" + u.getAsTag() + "'! Are they an admin? (Cannot add roles to admins and server owners...)");
                        }
                    }
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (StreamlineDiscord.getConfig().verificationEventUnVerifiedMinecraftEnabled()) {
                for (String command : StreamlineDiscord.getConfig().verificationEventUnVerifiedCommandsList()) {
                    ModuleUtils.queueRunAs(user, command);
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @BaseProcessor(priority = BaseEventPriority.MONITOR)
    public void onLogin(LoginCompletedEvent event) {
        try {
            if (! StreamlineDiscord.getConfig().serverEventStreamlineLogin()) return;

            CosmicSender sender = event.getSender();
            if (sender == null) return;

            // A proxy announces the join once the player has reached a backend, so the message can name it.
            long delay = DiscordEventMessages.isProxy() ? Math.max(0, StreamlineDiscord.getConfig().serverEventProxyJoinDelayTicks()) : 0L;
            DiscordEventMessages.sendPlayerEvent(EventClassifier.LOGIN, DiscordEventMessages.KEY_JOIN, sender, null, delay);
        } catch (Throwable t) {
            StreamlineDiscord.getInstance().logWarning("Could not send a join message to Discord: " + t);
        }
    }

    @BaseProcessor(priority = BaseEventPriority.MONITOR)
    public void onLogout(LogoutEvent event) {
        try {
            if (! StreamlineDiscord.getConfig().serverEventStreamlineLogout()) return;

            CosmicSender sender = event.getSender();
            if (sender == null) return;

            DiscordEventMessages.sendPlayerEvent(EventClassifier.LOGOUT, DiscordEventMessages.KEY_LEAVE, sender, null);
        } catch (Throwable t) {
            StreamlineDiscord.getInstance().logWarning("Could not send a quit message to Discord: " + t);
        }
    }

    /**
     * Deaths on Fabric, Forge and NeoForge servers. Streamline's death event carries no death message there,
     * so the message reads "{@code <player> died}". Spigot posts its own deaths, with the vanilla message,
     * from the Bukkit listener.
     */
    @BaseProcessor(priority = BaseEventPriority.MONITOR)
    public void onDeath(CosmicDeathEvent event) {
        try {
            if (! isModServer()) return;
            if (! StreamlineDiscord.getConfig().serverEventSpigotDeath()) return;

            CosmicSender sender = event.getSender();
            if (sender == null) return;

            String deathMessage = DiscordEventMessages.escape(sender.getCurrentName()) + " died";
            DiscordEventMessages.sendPlayerEvent(EventClassifier.DEATH, DiscordEventMessages.KEY_DEATH, sender,
                    BotMessages.placeholders(
                            "this_death_message", deathMessage,
                            "this_keep_inventory", "",
                            "this_keep_experience", ""));
        } catch (Throwable t) {
            StreamlineDiscord.getInstance().logWarning("Could not send a death message to Discord: " + t);
        }
    }

    private static boolean isModServer() {
        ISingularityExtension.PlatformType type = SLAPI.getInstance().getPlatform().getPlatformType();
        return type == ISingularityExtension.PlatformType.FABRIC
                || type == ISingularityExtension.PlatformType.FORGE
                || type == ISingularityExtension.PlatformType.NEOFORGE;
    }

    @BaseProcessor(priority = BaseEventPriority.MONITOR)
    public void onServerStart(ServerStartEvent event) {
        try {
            DiscordEventMessages.onServerStarted();
        } catch (Throwable t) {
            StreamlineDiscord.getInstance().logWarning("Could not send the server start message to Discord: " + t);
        }
    }
}
