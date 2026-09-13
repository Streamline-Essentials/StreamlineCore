package net.streamline.platform;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import net.streamline.api.SLAPI;
import net.streamline.base.StreamlineBungee;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.interfaces.IMessenger;
import singularity.modules.ModuleUtils;
import singularity.objects.CosmicTitle;
import singularity.text.HexPolicy;
import singularity.text.TextManager;
import singularity.utils.MessageUtils;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BungeeCord implementation of {@link IMessenger} that formats and delivers
 * messages to {@link CommandSender}s and {@link singularity.data.console.CosmicSender}s.
 *
 * <p>Handles colour codes (including hex), JSON component embedding via the
 * {@code !!json:} prefix, and optional PlaceholderAPI-style replacements when
 * the {@link net.streamline.api.SLAPI} is fully initialised.
 */
public class Messenger implements IMessenger {

    /** The singleton instance of this messenger, set during construction. */
    @Getter
    private static Messenger instance;

    /**
     * Constructs the {@code Messenger} and registers it as the singleton instance.
     */
    public Messenger() {
        instance = this;
    }

    /**
     * Sends a formatted message to the given BungeeCord {@link CommandSender}.
     *
     * <p>Applies colour codes and placeholder replacements (when SLAPI is ready)
     * before delivery. Does nothing if {@code to} is {@code null}.
     *
     * @param to      the recipient; {@code null} is silently ignored
     * @param message the raw message string, supporting {@code &}-colour codes
     */
    public void sendMessage(@Nullable CommandSender to, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(replaceAllPlayerBungee(to, message)));
        }
    }

    /**
     * Sends a formatted message to {@code to} with placeholders resolved for
     * the player identified by {@code otherUUID}.
     *
     * @param to        the recipient; {@code null} is silently ignored
     * @param otherUUID the UUID string of the player whose context is used for placeholder replacement
     * @param message   the raw message string
     */
    public void sendMessage(@Nullable CommandSender to, String otherUUID, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(otherUUID, message)));
        }
    }

    /**
     * Sends a formatted message to {@code to} with placeholders resolved in the
     * context of {@code other}.
     *
     * @param to      the recipient; {@code null} is silently ignored
     * @param other   the {@link CosmicSender} whose context is used for placeholder replacement
     * @param message the raw message string
     */
    public void sendMessage(@Nullable CommandSender to, CosmicSender other, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(other, message)));
        }
    }

    /**
     * Sends a formatted message to a {@link CosmicSender}, routing to the
     * corresponding BungeeCord player or console sender.
     *
     * @param to      the cross-platform recipient; {@code null} is silently ignored
     * @param message the raw message string
     */
    public void sendMessage(@Nullable CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(StreamlineBungee.getPlayer(to.getUuid()), message);
        else sendMessage(ProxyServer.getInstance().getConsole(), message);
    }

    /**
     * Sends a formatted message to a {@link CosmicSender} with placeholders
     * resolved for the player identified by {@code otherUUID}.
     *
     * @param to        the cross-platform recipient; {@code null} is silently ignored
     * @param otherUUID the UUID string of the context player for placeholder replacement
     * @param message   the raw message string
     */
    public void sendMessage(@Nullable CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(StreamlineBungee.getPlayer(to.getUuid()), otherUUID, message);
        else sendMessage(ProxyServer.getInstance().getConsole(), otherUUID, message);
    }

    /**
     * Sends a formatted message to a {@link CosmicSender} with placeholders
     * resolved in the context of {@code other}.
     *
     * @param to      the cross-platform recipient; {@code null} is silently ignored
     * @param other   the {@link CosmicSender} context for placeholder replacement; {@code null} is silently ignored
     * @param message the raw message string
     */
    public void sendMessage(@Nullable CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        if (to instanceof CosmicPlayer) sendMessage(StreamlineBungee.getPlayer(to.getUuid()), other, message);
        else sendMessage(ProxyServer.getInstance().getConsole(), other, message);
    }

    /**
     * Sends a message to a BungeeCord {@link CommandSender} as a raw
     * {@link net.md_5.bungee.api.chat.BaseComponent} array without additional
     * colour processing beyond placeholder replacement.
     *
     * @param to      the recipient; {@code null} is silently ignored
     * @param message the raw message string
     */
    public void sendMessageRaw(CommandSender to, String message) {
        if (to == null) return;

        BaseComponent[] component;
        if (! SLAPI.isReady()) {
            component = new ComponentBuilder(message).create();
        } else {
            component = new ComponentBuilder(replaceAllPlayerBungee(to, message)).create();
        }

        to.sendMessage(component);
    }

    /**
     * Sends a raw component message to a {@link CommandSender} with placeholders
     * resolved for the player identified by {@code otherUUID}.
     *
     * @param to        the recipient; {@code null} is silently ignored
     * @param otherUUID the UUID string of the context player for placeholder replacement
     * @param message   the raw message string
     */
    public void sendMessageRaw(CommandSender to, String otherUUID, String message) {
        if (to == null) return;

        BaseComponent[] component;
        if (! SLAPI.isReady()) {
            component = new ComponentBuilder(message).create();
        } else {
            component = new ComponentBuilder(MessageUtils.replaceAllPlayerBungee(otherUUID, message)).create();
        }

        to.sendMessage(component);
    }

    /**
     * Sends a raw component message to a {@link CommandSender} with placeholders
     * resolved in the context of {@code other}.
     *
     * @param to      the recipient; {@code null} is silently ignored
     * @param other   the {@link CosmicSender} context for placeholder replacement
     * @param message the raw message string
     */
    public void sendMessageRaw(CommandSender to, CosmicSender other, String message) {
        if (to == null) return;

        BaseComponent[] component;
        if (! SLAPI.isReady()) {
            component = new ComponentBuilder(message).create();
        } else {
            component = new ComponentBuilder(MessageUtils.replaceAllPlayerBungee(other, message)).create();
        }

        to.sendMessage(component);
    }

    /**
     * Sends a raw component message to a {@link CosmicSender}, routing to the
     * corresponding BungeeCord player or console sender.
     *
     * @param to      the cross-platform recipient; {@code null} is silently ignored
     * @param message the raw message string
     */
    public void sendMessageRaw(@Nullable CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(StreamlineBungee.getPlayer(to.getUuid()), message);
        else sendMessageRaw(ProxyServer.getInstance().getConsole(), message);
    }

    /**
     * Sends a raw component message to a {@link CosmicSender} with placeholders
     * resolved for the player identified by {@code otherUUID}.
     *
     * @param to        the cross-platform recipient; {@code null} is silently ignored
     * @param otherUUID the UUID string of the context player
     * @param message   the raw message string
     */
    public void sendMessageRaw(@Nullable CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(StreamlineBungee.getPlayer(to.getUuid()), otherUUID, message);
        else sendMessageRaw(ProxyServer.getInstance().getConsole(), otherUUID, message);
    }

    /**
     * Sends a raw component message to a {@link CosmicSender} with placeholders
     * resolved in the context of {@code other}.
     *
     * @param to      the cross-platform recipient; {@code null} is silently ignored
     * @param other   the context {@link CosmicSender}; {@code null} is silently ignored
     * @param message the raw message string
     */
    public void sendMessageRaw(@Nullable CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(StreamlineBungee.getPlayer(to.getUuid()), other, message);
        else sendMessageRaw(ProxyServer.getInstance().getConsole(), other, message);
    }

    /**
     * Sends a title and subtitle to the given player using the BungeeCord title API.
     *
     * <p>Logs an info message and returns without action if the underlying
     * {@link ProxiedPlayer} cannot be resolved.
     *
     * @param player the cross-platform player to receive the title
     * @param title  the {@link CosmicTitle} containing main text, subtitle,
     *               fade-in, stay, and fade-out durations (in ticks)
     */
    public void sendTitle(CosmicSender player, CosmicTitle title) {
        ProxiedPlayer p = StreamlineBungee.getPlayer(player.getUuid());
        if (p == null) {
            MessageUtils.logInfo("Could not send a title to a player because player is null!");
            return;
        }

        p.sendTitle(StreamlineBungee.getInstance().getProxy().createTitle()
                .title(codedText(title.getMain()))
                .subTitle(codedText(title.getSub()))
                .fadeIn((int) title.getFadeIn())
                .stay((int) title.getStay())
                .fadeOut((int) title.getFadeOut())
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Translates {@code &}-prefixed colour codes and normalises newline
     * characters via {@link singularity.modules.ModuleUtils#newLined}.
     */
    @Override
    public String codedString(String from) {
        return ChatColor.translateAlternateColorCodes('&', ModuleUtils.newLined(from));
    }

    /**
     * Strips all BungeeCord colour codes and hex colour tags from the given string.
     *
     * @param string the string to strip
     * @return the colour-stripped string
     */
    public String stripColor(String string){
        return ChatColor.stripColor(string).replaceAll("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", "");
    }

    /**
     * Converts a raw string (with {@code &}-colour codes, hex codes, and optional
     * {@code !!json:} segments) into a BungeeCord {@link net.md_5.bungee.api.chat.BaseComponent}
     * array suitable for sending to players.
     *
     * <p>Processing order: hex code expansion, {@link #codedString} colour translation,
     * legacy PlaceholderAPI-style processing, then JSON block extraction and parsing.
     *
     * @param from the raw input string
     * @return the resulting component array; never {@code null}
     */
    public BaseComponent[] codedText(String from) {
        String raw = from;

        List<BaseComponent> componentsList = new ArrayList<>();

        // Handle hex codes
        for (HexPolicy policy : TextManager.getHexPolicies()) {
            for (String hexCode : TextManager.extractHexCodes(raw, policy)) {
                String original = hexCode;
                if (! hexCode.startsWith("#")) hexCode = "#" + hexCode;
                String replacement = ChatColor.of(hexCode).toString();
                raw = raw.replace(policy.getResult(original), replacement);
            }
        }

        raw = codedString(raw);

        // Assuming codedString is another method you've implemented to replace color codes etc.
        String legacy = MessageUtils.codedString(raw);

        List<String> jsonStrings = TextManager.extractJsonStrings(legacy, "!!json:");

        int lastEnd = 0;

        for (String jsonStr : jsonStrings) {
            int index = legacy.indexOf("!!json:" + jsonStr);
            String before = legacy.substring(lastEnd, index);
            BaseComponent[] beforeComponent = TextComponent.fromLegacyText(before);
            Collections.addAll(componentsList, beforeComponent);

            try {
                BaseComponent[] jsonComponent = ComponentSerializer.parse(jsonStr);
                Collections.addAll(componentsList, jsonComponent);
            } catch (Exception e) {
                // Handle exception
                e.printStackTrace();
            }

            lastEnd = index + jsonStr.length() + 7; // 7 is the length of "!!json:"
        }

        // Append any remaining text after the last JSON block
        if (lastEnd < legacy.length()) {
            BaseComponent[] remainingComponent = TextComponent.fromLegacyText(legacy.substring(lastEnd));
            Collections.addAll(componentsList, remainingComponent);
        }

        return componentsList.toArray(new BaseComponent[0]);
    }

    /**
     * Resolves the {@link CosmicSender} for the given BungeeCord
     * {@link CommandSender} and applies all registered player placeholder
     * replacements to the string {@code of}.
     *
     * <p>Returns {@code of} unchanged if the sender cannot be resolved.
     *
     * @param sender the BungeeCord command sender
     * @param of     the string in which to apply replacements
     * @return the string with all applicable placeholders replaced
     */
    public String replaceAllPlayerBungee(CommandSender sender, String of) {
        CosmicSender s = UserManager.getInstance().getOrCreateSender(sender).orElse(null);
        if (s == null) {
            return of;
        }

        return MessageUtils.replaceAllPlayerBungee(s, of);
    }
}
