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
import singularity.events.server.CosmicChatEvent;
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

    @BaseProcessor
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

                    DiscordMessenger.message(event.getMessage().getChannel().getIdLong(), data.getKey(), data.getValue());
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
        event.getCommand().execute(event.getMessage());
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

    @BaseProcessor
    public void onLogin(LoginCompletedEvent event) {
        if (! StreamlineDiscord.getConfig().serverEventStreamlineLogin()) return;

        CosmicSender sender = event.getSender();
        if (sender == null) return;

        String message = StreamlineDiscord.getMessages().forwardedStreamlineLogin();

        RouteLoader.getLoadedRoutes().forEach(route -> {
            if (! route.hasEnabledEvent(EventClassifier.LOGIN)) return;

            if (route.getInput().getType().equals(EndPointType.GLOBAL_NATIVE)) {
                route.bounceEvent(new RoutedUser(sender), message);
            } else if (route.getInput().getType().equals(EndPointType.SPECIFIC_NATIVE) && route.getInput().getEndPointIdentifier().equals(sender.getServerName())) {
                route.bounceEvent(new RoutedUser(sender), message);
            } else if (route.getInput().getType().equals(EndPointType.PERMISSION) && sender.hasPermission(route.getInput().getEndPointIdentifier())) {
                route.bounceEvent(new RoutedUser(sender), message);
            }
        });
    }

    @BaseProcessor
    public void onLogout(LogoutEvent event) {
        if (! StreamlineDiscord.getConfig().serverEventStreamlineLogout()) return;

        CosmicSender sender = event.getSender();
        if (sender == null) return;

        String message = StreamlineDiscord.getMessages().forwardedStreamlineLogout();

        RouteLoader.getLoadedRoutes().forEach(route -> {
            if (! route.hasEnabledEvent(EventClassifier.LOGOUT)) return;

            if (route.getInput().getType().equals(EndPointType.GLOBAL_NATIVE)) {
                route.bounceEvent(new RoutedUser(sender), message);
            } else if (
                    ( route.getInput().getType().equals(EndPointType.SPECIFIC_NATIVE) || route.getInput().getType().equals(EndPointType.SPECIFIC_HANDLED) )
                            && route.getInput().getEndPointIdentifier().equals(sender.getServerName())) {
                route.bounceEvent(new RoutedUser(sender), message);
            } else if (route.getInput().getType().equals(EndPointType.PERMISSION) && sender.hasPermission(route.getInput().getEndPointIdentifier())) {
                route.bounceEvent(new RoutedUser(sender), message);
            }
        });
    }
}
