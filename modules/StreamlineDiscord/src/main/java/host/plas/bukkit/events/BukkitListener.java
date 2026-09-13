package host.plas.bukkit.events;

import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.StreamlineDiscord;
import host.plas.discord.data.channeling.EndPointType;
import host.plas.discord.data.channeling.RouteLoader;
import host.plas.discord.data.channeling.RoutedUser;
import host.plas.discord.data.events.EventClassifier;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import host.plas.bukkit.events.streamline.EventingEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import singularity.data.console.CosmicSender;
import singularity.events.server.LogoutEvent;
import singularity.utils.UserUtils;

public class BukkitListener implements Listener {
    @EventHandler
    public void onEvent(PlayerAdvancementDoneEvent event) {
        EventingEvent<Event> eventingEvent = new EventingEvent<>(event.getEventName(), event);
        eventingEvent.fire();

        if (! StreamlineDiscord.getConfig().serverEventSpigotAdvancement()) return;

        CosmicSender sender = UserUtils.getOrGetSender(event.getPlayer().getUniqueId().toString()).orElse(null);
        if (sender == null) return;

        String message = StreamlineDiscord.getMessages().forwardedSpigotAdvancement();

        RouteLoader.getLoadedRoutes().forEach(route -> {
            if (! route.hasEnabledEvent(EventClassifier.ADVANCEMENT)) return;

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

    @EventHandler
    public void onEvent(PlayerDeathEvent event) {
        EventingEvent<Event> eventingEvent = new EventingEvent<>(event.getEventName(), event);
        eventingEvent.fire();

        if (! StreamlineDiscord.getConfig().serverEventSpigotDeath()) return;

        CosmicSender sender = UserUtils.getOrGetSender(event.getEntity().getUniqueId().toString()).orElse(null);
        if (sender == null) return;

        String message = StreamlineDiscord.getMessages().forwardedSpigotDeath();

        RouteLoader.getLoadedRoutes().forEach(route -> {
            if (! route.hasEnabledEvent(EventClassifier.DEATH)) return;

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
