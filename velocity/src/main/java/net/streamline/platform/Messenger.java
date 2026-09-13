package net.streamline.platform;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.streamline.api.SLAPI;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import net.streamline.base.StreamlineVelocity;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.Nullable;
import singularity.interfaces.IMessenger;
import singularity.modules.ModuleUtils;
import singularity.objects.CosmicTitle;
import singularity.text.HexPolicy;
import singularity.text.TextManager;
import singularity.utils.MessageUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Velocity-specific implementation of {@link IMessenger} that converts colour-coded and
 * hex-colour strings into Adventure {@link Component} objects and delivers them to
 * {@link CommandSource}/{@link CosmicSender} targets.
 *
 * <p>Supports legacy {@code &}-codes, MiniMessage-style hex colours, and embedded JSON
 * component blocks (prefixed with {@code !!json:}). Placeholder replacement is applied
 * when the SLAPI layer is ready.
 */
public class Messenger implements IMessenger {
    /**
     * The singleton {@code Messenger} instance, set during construction.
     */
    @Getter
    private static Messenger instance;

    /**
     * Constructs a new {@code Messenger} and registers it as the singleton instance.
     */
    public Messenger() {
        instance = this;
    }

    /**
     * Sends a colour-coded, placeholder-replaced message to a Velocity {@link CommandSource}.
     *
     * @param to      the recipient; if {@code null} this method is a no-op
     * @param message the raw message string supporting {@code &}-colour codes
     */
    public void sendMessage(@Nullable CommandSource to, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(replaceAllPlayerBungee(to, message)));
        }
    }

    /**
     * Sends a message to a {@link CommandSource} with placeholders replaced relative to
     * another player identified by UUID.
     *
     * @param to        the recipient; if {@code null} this method is a no-op
     * @param otherUUID the UUID string of the player whose context is used for replacement
     * @param message   the raw message string
     */
    public void sendMessage(@Nullable CommandSource to, String otherUUID, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(otherUUID, message)));
        }
    }

    /**
     * Sends a message to a {@link CommandSource} with placeholders replaced relative to
     * another {@link CosmicSender}.
     *
     * @param to      the recipient; if {@code null} this method is a no-op
     * @param other   the sender whose context is used for placeholder replacement
     * @param message the raw message string
     */
    public void sendMessage(@Nullable CommandSource to, CosmicSender other, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(other, message)));
        }
    }

    /**
     * Sends a message to a {@link CosmicSender}, routing to the Velocity player or console
     * as appropriate.
     *
     * @param to      the recipient; if {@code null} this method is a no-op
     * @param message the raw message string
     */
    public void sendMessage(@Nullable CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(StreamlineVelocity.getPlayer(to.getUuid()), message);
        else sendMessage(StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource(), message);
    }

    /**
     * Sends a message to a {@link CosmicSender} with placeholders replaced relative to
     * another player identified by UUID.
     *
     * @param to        the recipient; if {@code null} this method is a no-op
     * @param otherUUID the UUID string of the player whose context is used for replacement
     * @param message   the raw message string
     */
    public void sendMessage(@Nullable CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(StreamlineVelocity.getPlayer(to.getUuid()), otherUUID, message);
        else sendMessage(StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource(), otherUUID, message);
    }

    /**
     * Sends a message to a {@link CosmicSender} with placeholders replaced relative to
     * another {@link CosmicSender}.
     *
     * @param to      the recipient; if {@code null} this method is a no-op
     * @param other   the sender whose context is used for placeholder replacement
     * @param message the raw message string
     */
    public void sendMessage(@Nullable CosmicSender to, CosmicSender other, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(StreamlineVelocity.getPlayer(to.getUuid()), other, message);
        else sendMessage(StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource(), other, message);
    }

    /**
     * Sends a plain (non-colour-processed) message to a Velocity {@link CommandSource}.
     *
     * @param to      the recipient; if {@code null} this method is a no-op
     * @param message the raw message string
     */
    public void sendMessageRaw(CommandSource to, String message) {
        if (to == null) return;

        Component component;
        if (! SLAPI.isReady()) {
            component = Component.text(message);
        } else {
            component = Component.text(replaceAllPlayerBungee(to, message));
        }

        to.sendMessage(component);
    }

    /**
     * Sends a plain (non-colour-processed) message to a {@link CommandSource}, replacing
     * placeholders relative to another player identified by UUID.
     *
     * @param to        the recipient; if {@code null} this method is a no-op
     * @param otherUUID the UUID string of the player whose context is used for replacement
     * @param message   the raw message string
     */
    public void sendMessageRaw(CommandSource to, String otherUUID, String message) {
        if (to == null) return;

        Component component;
        if (! SLAPI.isReady()) {
            component = Component.text(message);
        } else {
            component = Component.text(MessageUtils.replaceAllPlayerBungee(otherUUID, message));
        }

        to.sendMessage(component);
    }

    /**
     * Sends a plain (non-colour-processed) message to a {@link CommandSource}, replacing
     * placeholders relative to another {@link CosmicSender}.
     *
     * @param to      the recipient; if {@code null} this method is a no-op
     * @param other   the sender whose context is used for placeholder replacement
     * @param message the raw message string
     */
    public void sendMessageRaw(CommandSource to, CosmicSender other, String message) {
        if (to == null) return;

        Component component;
        if (! SLAPI.isReady()) {
            component = Component.text(message);
        } else {
            component = Component.text(MessageUtils.replaceAllPlayerBungee(other, message));
        }

        to.sendMessage(component);
    }

    /**
     * Sends a plain (non-colour-processed) message to a {@link CosmicSender}, routing to
     * the Velocity player or console as appropriate.
     *
     * @param to      the recipient; if {@code null} this method is a no-op
     * @param message the raw message string
     */
    public void sendMessageRaw(@Nullable CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(StreamlineVelocity.getPlayer(to.getUuid()), message);
        else sendMessageRaw(StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource(), message);
    }

    /**
     * Sends a plain (non-colour-processed) message to a {@link CosmicSender} with placeholders
     * replaced relative to another player identified by UUID.
     *
     * @param to        the recipient; if {@code null} this method is a no-op
     * @param otherUUID the UUID string of the player whose context is used for replacement
     * @param message   the raw message string
     */
    public void sendMessageRaw(@Nullable CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(StreamlineVelocity.getPlayer(to.getUuid()), otherUUID, message);
        else sendMessageRaw(StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource(), otherUUID, message);
    }

    /**
     * Sends a plain (non-colour-processed) message to a {@link CosmicSender} with placeholders
     * replaced relative to another {@link CosmicSender}.
     *
     * @param to      the recipient; if {@code null} this method is a no-op
     * @param other   the sender whose context is used for placeholder replacement
     * @param message the raw message string
     */
    public void sendMessageRaw(@Nullable CosmicSender to, CosmicSender other, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(StreamlineVelocity.getPlayer(to.getUuid()), other, message);
        else sendMessageRaw(StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource(), other, message);
    }

    @Override
    public void sendTitle(CosmicSender player, CosmicTitle title) {
        Player p = StreamlineVelocity.getPlayer(player.getUuid());
        if (p == null) {
            MessageUtils.logInfo("Could not send a title to a player because player is null!");
            return;
        }

        Title t = Title.title(
                codedText(
                        MessageUtils.replaceAllPlayerBungee(player, title.getMain())
                ),
                codedText(
                        MessageUtils.replaceAllPlayerBungee(player, title.getSub())
                ), Title.Times.times(
                        Duration.of(title.getFadeIn() * 50L, ChronoUnit.MILLIS),
                        Duration.of(title.getStay() * 50L, ChronoUnit.MILLIS),
                        Duration.of(title.getFadeOut() * 50L, ChronoUnit.MILLIS)
                )
        );

        p.showTitle(t);
    }

    @Override
    public String codedString(String from) {
//        return ModuleUtils.newLined(from.replace("&", "§"));

        return ModuleUtils.newLined(from); // issues. ^^^
    }

    /**
     * Strips all colour and formatting codes from the given string, returning plain text.
     *
     * <p>Removes legacy section-symbol codes, inline hex colour tags, and
     * ampersand colour codes.
     *
     * @param string the input string possibly containing colour codes
     * @return a plain-text version of the string with all colour markup removed
     */
    public String stripColor(String string){
        return PlainTextComponentSerializer.plainText().serialize(LegacyComponentSerializer.legacySection().deserialize(string))
                .replaceAll("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", "")
                .replaceAll("[&][1-9a-f]", "");
    }

    /**
     * Serialises an Adventure {@link Component} back to a legacy section-symbol colour string.
     *
     * @param textComponent the component to serialise
     * @return the legacy-coded string representation of the component
     */
    public String asString(Component textComponent){
        return LegacyComponentSerializer.legacySection().serialize(textComponent);
    }

    /**
     * Converts a string with {@code &}-colour codes and hex colours into an Adventure
     * {@link Component} using the legacy {@code &} character with URL extraction enabled.
     *
     * @param from the input string with {@code &}-colour codes
     * @return the deserialized Adventure {@link Component}
     */
    public static Component legacyCode(String from) {
        return LegacyComponentSerializer.builder().extractUrls().character('&').hexColors().build().deserialize(from);
    }

    /**
     * Converts a colour-coded string into an Adventure {@link Component}, processing:
     * <ul>
     *   <li>Hex colour policies registered in {@link singularity.text.TextManager}</li>
     *   <li>Embedded JSON component blocks prefixed with {@code !!json:}</li>
     *   <li>Legacy {@code &}-colour codes via the Adventure serializer</li>
     * </ul>
     *
     * @param from the raw string to convert
     * @return the fully processed Adventure {@link Component}
     */
    public Component codedText(String from) {
        String raw = codedString(from); // Assuming codedString is another method you've implemented

        String legacy = MessageUtils.newLined(MessageUtils.formatted(raw)); // Replace this with your actual legacy converter

        List<Component> componentsList = new ArrayList<>();

        LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
                .character('&')
                .hexColors()
                .build();

        // Handle hex codes
        for (HexPolicy policy : TextManager.getHexPolicies()) {
            for (String hexCode : TextManager.extractHexCodes(legacy, policy)) {
                String original = hexCode;
                if (! hexCode.startsWith("#")) hexCode = "#" + hexCode;
                legacy = legacy.replace(policy.getResult(original),
                        "&#" + hexCode.substring(1));
            }
        }

        List<String> jsonStrings = TextManager.extractJsonStrings(legacy, "!!json:");

        int lastEnd = 0;

        for (String jsonStr : jsonStrings) {
            int index = legacy.indexOf("!!json:" + jsonStr);
            String before = legacy.substring(lastEnd, index);
            Component beforeComponent = serializer.deserialize(before);
            componentsList.add(beforeComponent);

            try {
                Component jsonComponent = JSONComponentSerializer.json().deserialize(jsonStr);
                componentsList.add(jsonComponent);
            } catch (Exception e) {
                // Handle exception
                e.printStackTrace();
            }

            lastEnd = index + jsonStr.length() + 7; // 7 is the length of "!!json:"
        }

        // Append any remaining text after the last JSON block
        if (lastEnd < legacy.length()) {
            Component remainingComponent = serializer.deserialize(legacy.substring(lastEnd));
            componentsList.add(remainingComponent);
        }

        return Component.empty().children(componentsList);
    }

    /**
     * Replaces all player-specific placeholders in the given string using the
     * {@link CosmicSender} resolved from the supplied Velocity {@link CommandSource}.
     *
     * <p>If the sender cannot be resolved the original string is returned unchanged.
     *
     * @param sender the Velocity command source whose data is used for placeholder replacement
     * @param of     the string containing placeholders to replace
     * @return the string with all applicable placeholders substituted
     */
    public String replaceAllPlayerBungee(CommandSource sender, String of) {
        CosmicSender s = UserManager.getInstance().getOrCreateSender(sender).orElse(null);
        if (s == null) {
            return of; // If sender is null, return the original string
        }

        return MessageUtils.replaceAllPlayerBungee(s, of);
    }
}