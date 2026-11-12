package host.plas.bukkit.events;

import host.plas.StreamlineDiscord;
import host.plas.bukkit.events.streamline.EventingEvent;
import host.plas.discord.data.events.DiscordEventMessages;
import host.plas.discord.data.events.EventClassifier;
import org.bukkit.GameRule;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import singularity.data.console.CosmicSender;
import singularity.utils.UserUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Posts Spigot / Paper advancements and deaths to Discord, the way the game announces them in chat.
 *
 * <p>Modules are not remapped like plugins, so a Spigot method Paper replaced (such as
 * {@code Advancement.getDisplay()}, whose return type differs on Paper) cannot be linked directly: the
 * advancement's display is read reflectively, by the method names both APIs share.
 */
public class BukkitListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEvent(PlayerAdvancementDoneEvent event) {
        try {
            new EventingEvent<Event>(event.getEventName(), event).fire();
        } catch (Throwable t) {
            StreamlineDiscord.getInstance().logDebug("Could not forward " + event.getEventName() + ": " + t);
        }

        try {
            if (! StreamlineDiscord.getConfig().serverEventSpigotAdvancement()) return;

            Map<String, String> extra = advancementPlaceholders(event);
            if (extra == null) return;

            CosmicSender sender = UserUtils.getOrGetSender(event.getPlayer().getUniqueId().toString()).orElse(null);
            if (sender == null) return;

            DiscordEventMessages.sendPlayerEvent(EventClassifier.ADVANCEMENT, DiscordEventMessages.KEY_ADVANCEMENT, sender, extra);
        } catch (Throwable t) {
            StreamlineDiscord.getInstance().logWarning("Could not send an advancement to Discord: " + t);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEvent(PlayerDeathEvent event) {
        try {
            new EventingEvent<Event>(event.getEventName(), event).fire();
        } catch (Throwable t) {
            StreamlineDiscord.getInstance().logDebug("Could not forward " + event.getEventName() + ": " + t);
        }

        try {
            if (! StreamlineDiscord.getConfig().serverEventSpigotDeath()) return;

            // A death message removed by the server or a plugin means the death is not announced.
            String deathMessage = DiscordEventMessages.clean(event.getDeathMessage()).trim();
            if (deathMessage.isEmpty()) return;
            // The message is still filled in when the showDeathMessages game rule hides it from chat.
            try {
                Boolean show = event.getEntity().getWorld().getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES);
                if (Boolean.FALSE.equals(show)) return;
            } catch (Throwable ignored) {
                // No game rules on this server version.
            }

            CosmicSender sender = UserUtils.getOrGetSender(event.getEntity().getUniqueId().toString()).orElse(null);
            if (sender == null) return;

            Map<String, String> extra = new HashMap<>();
            extra.put("this_death_message", DiscordEventMessages.escape(deathMessage));
            extra.put("this_keep_inventory", String.valueOf(event.getKeepInventory()));
            extra.put("this_keep_experience", String.valueOf(event.getKeepLevel()));

            DiscordEventMessages.sendPlayerEvent(EventClassifier.DEATH, DiscordEventMessages.KEY_DEATH, sender, extra);
        } catch (Throwable t) {
            StreamlineDiscord.getInstance().logWarning("Could not send a death to Discord: " + t);
        }
    }

    /**
     * The advancement's placeholders, or null when the game does not announce it: advancements without a
     * display (recipe unlocks, hidden roots), ones not shown in chat, Paper events whose message was removed,
     * and worlds with the {@code announceAdvancements} game rule off.
     */
    private static Map<String, String> advancementPlaceholders(PlayerAdvancementDoneEvent event) {
        Advancement advancement = event.getAdvancement();
        String key = advancement.getKey().toString();
        if (advancement.getKey().getKey().startsWith("recipes/")) return null;

        // Paper: message() is the chat line, null once removed.
        Method message = method(event, "message");
        if (message != null && invoke(message, event) == null) return null;

        try {
            Boolean announce = event.getPlayer().getWorld().getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS);
            if (Boolean.FALSE.equals(announce)) return null;
        } catch (Throwable ignored) {
            // No game rules on this server version.
        }

        String title = key;
        String description = "";
        Method getDisplay = method(advancement, "getDisplay");
        if (getDisplay != null) {
            Object display = invoke(getDisplay, advancement);
            if (display == null) return null;

            // Spigot: shouldAnnounceChat(), Paper: doesAnnounceToChat().
            Object announces = call(display, "shouldAnnounceChat");
            if (announces == null) announces = call(display, "doesAnnounceToChat");
            if (Boolean.FALSE.equals(announces)) return null;

            // Spigot: getTitle() / getDescription() strings, Paper: title() / description() components.
            String displayTitle = text(call(display, "getTitle"));
            if (displayTitle == null) displayTitle = text(call(display, "title"));
            String displayDescription = text(call(display, "getDescription"));
            if (displayDescription == null) displayDescription = text(call(display, "description"));

            if (displayTitle != null && ! displayTitle.isBlank()) title = displayTitle;
            if (displayDescription != null) description = displayDescription;
        }

        Map<String, String> map = new HashMap<>();
        map.put("this_advancement_key", key);
        map.put("this_advancement_title", DiscordEventMessages.escape(title));
        map.put("this_advancement_description", DiscordEventMessages.escape(description));
        map.put("this_advancement_criteria", String.join(", ", advancement.getCriteria()));
        // Short names for the shipped embed.
        map.put("advancement", map.get("this_advancement_title"));
        map.put("description", map.get("this_advancement_description"));
        return map;
    }

    private static Method method(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            method.setAccessible(true);
            return method;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object invoke(Method method, Object target) {
        try {
            return method.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object call(Object target, String name) {
        Method method = method(target, name);
        return method == null ? null : invoke(method, target);
    }

    /** Plain text of a string or an Adventure component, colour codes removed; null for anything else. */
    private static String text(Object value) {
        if (value == null) return null;
        if (value instanceof String) return DiscordEventMessages.clean((String) value);

        try {
            Class<?> serializer = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer",
                    true, value.getClass().getClassLoader());
            Object plain = serializer.getMethod("plainText").invoke(null);
            Class<?> component = Class.forName("net.kyori.adventure.text.Component", true, value.getClass().getClassLoader());
            Object serialized = serializer.getMethod("serialize", component).invoke(plain, value);
            return serialized == null ? null : DiscordEventMessages.clean(serialized.toString());
        } catch (Throwable t) {
            return null;
        }
    }
}
