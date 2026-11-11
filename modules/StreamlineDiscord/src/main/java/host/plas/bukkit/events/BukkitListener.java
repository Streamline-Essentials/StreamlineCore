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

import java.util.HashMap;
import java.util.Map;

public class BukkitListener implements Listener {
    @EventHandler
    public void onEvent(PlayerAdvancementDoneEvent event) {
        EventingEvent<Event> eventingEvent = new EventingEvent<>(event.getEventName(), event);
        eventingEvent.fire();

        if (! StreamlineDiscord.getConfig().serverEventSpigotAdvancement()) return;

        CosmicSender sender = UserUtils.getOrGetSender(event.getPlayer().getUniqueId().toString()).orElse(null);
        if (sender == null) return;

        Map<String, String> extra = advancementPlaceholders(event);

        RouteLoader.getLoadedRoutes().forEach(route -> {
            if (! route.hasEnabledEvent(EventClassifier.ADVANCEMENT)) return;

            if (route.getInput().getType().equals(EndPointType.GLOBAL_NATIVE)) {
                route.bounceEvent(new RoutedUser(sender), "events.advancement", extra);
            } else if (
                    ( route.getInput().getType().equals(EndPointType.SPECIFIC_NATIVE) || route.getInput().getType().equals(EndPointType.SPECIFIC_HANDLED) )
                            && route.getInput().getEndPointIdentifier().equals(sender.getServerName())) {
                route.bounceEvent(new RoutedUser(sender), "events.advancement", extra);
            } else if (route.getInput().getType().equals(EndPointType.PERMISSION) && sender.hasPermission(route.getInput().getEndPointIdentifier())) {
                route.bounceEvent(new RoutedUser(sender), "events.advancement", extra);
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

        Map<String, String> extra = deathPlaceholders(event);

        RouteLoader.getLoadedRoutes().forEach(route -> {
            if (! route.hasEnabledEvent(EventClassifier.DEATH)) return;

            if (route.getInput().getType().equals(EndPointType.GLOBAL_NATIVE)) {
                route.bounceEvent(new RoutedUser(sender), "events.death", extra);
            } else if (
                    ( route.getInput().getType().equals(EndPointType.SPECIFIC_NATIVE) || route.getInput().getType().equals(EndPointType.SPECIFIC_HANDLED) )
                            && route.getInput().getEndPointIdentifier().equals(sender.getServerName())) {
                route.bounceEvent(new RoutedUser(sender), "events.death", extra);
            } else if (route.getInput().getType().equals(EndPointType.PERMISSION) && sender.hasPermission(route.getInput().getEndPointIdentifier())) {
                route.bounceEvent(new RoutedUser(sender), "events.death", extra);
            }
        });
    }

    private static Map<String, String> advancementPlaceholders(PlayerAdvancementDoneEvent event) {
        Map<String, String> map = new HashMap<>();
        String key = event.getAdvancement().getKey().toString();
        map.put("this_advancement_key", key);
        map.put("this_advancement_title", key);
        map.put("this_advancement_description", "");
        map.put("this_advancement_criteria", String.join(", ", event.getAdvancement().getCriteria()));
        try {
            // AdvancementDisplay exists from Spigot 1.19.4.
            org.bukkit.advancement.AdvancementDisplay display = event.getAdvancement().getDisplay();
            if (display != null) {
                map.put("this_advancement_title", display.getTitle());
                map.put("this_advancement_description", display.getDescription());
            }
        } catch (Throwable ignored) {
            // Older servers keep the key as the title.
        }
        return map;
    }

    private static Map<String, String> deathPlaceholders(PlayerDeathEvent event) {
        Map<String, String> map = new HashMap<>();
        String deathMessage = event.getDeathMessage();
        map.put("this_death_message", deathMessage == null ? "" : deathMessage);
        map.put("this_keep_inventory", String.valueOf(event.getKeepInventory()));
        map.put("this_keep_experience", String.valueOf(event.getKeepLevel()));
        return map;
    }
}
