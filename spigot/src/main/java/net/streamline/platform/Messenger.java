package net.streamline.platform;

import host.plas.bou.commands.Sender;
import host.plas.bou.utils.ColorUtils;
import host.plas.bou.utils.SenderUtils;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.streamline.api.SLAPI;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import net.streamline.base.StreamlineSpigot;
import net.streamline.platform.savables.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import singularity.interfaces.IMessenger;
import singularity.objects.CosmicTitle;
import singularity.utils.MessageUtils;

import java.util.*;

/**
 * Spigot implementation of {@link singularity.interfaces.IMessenger} that
 * sends formatted messages and titles to Bukkit players and the console.
 *
 * <p>Colour codes are processed by the BukkitOfUtils (BOU) message utilities,
 * supporting legacy {@code &}-codes, hex colours, and MiniMessage-style tags.
 * When the SLAPI layer is ready, PlaceholderAPI-style replacements are applied
 * before sending.
 */
public class Messenger implements IMessenger {
    /**
     * The singleton instance of this {@code Messenger}, set during construction.
     */
    @Getter
    private static Messenger instance;

    /**
     * Constructs a new {@code Messenger} and registers it as the singleton
     * instance accessible via {@link #getInstance()}.
     */
    public Messenger() {
        instance = this;
    }

    /**
     * Translates colour codes in a message string using the BOU utilities.
     *
     * @param message the raw message with colour codes
     * @return the coloured message string
     * @deprecated use {@link #codedStringBOU(String)} instead
     */
    @Deprecated
    public static String colorAsString(String message) {
        return colorAsStringBOU(message);
    }

    /**
     * Translates colour codes in a message string using the BOU message utilities.
     *
     * @param message the raw message with colour codes
     * @return the coloured message string
     * @deprecated use {@link #codedStringBOU(String)} or {@link #codedString(String)} instead
     */
    @Deprecated
    public static String colorAsStringBOU(String message) {
        return host.plas.bou.utils.MessageUtils.codedString(message); // Already new-lined.
    }

    /**
     * Sends a formatted message to a Bukkit {@link org.bukkit.command.CommandSender}.
     *
     * <p>When the SLAPI layer is ready, placeholder replacements relative to the
     * sender are applied before delivery.
     *
     * @param to      the recipient; does nothing if {@code null}
     * @param message the message to send, may contain colour codes and placeholders
     */
    public void sendMessage(@Nullable CommandSender to, String message) {
        if (to == null) return;
        Sender s = SenderUtils.getSender(to);
        if (! SLAPI.isReady()) {
            s.sendMessage(message);
        } else {
            s.sendMessage(replaceAllPlayerBungee(to, message));
        }
    }

    /**
     * Sends a formatted message to a Bukkit {@link org.bukkit.command.CommandSender},
     * resolving placeholders relative to the player identified by {@code otherUUID}.
     *
     * @param to        the recipient; does nothing if {@code null}
     * @param otherUUID the UUID of the player used for placeholder resolution
     * @param message   the message to send, may contain colour codes and placeholders
     */
    public void sendMessage(@Nullable CommandSender to, String otherUUID, String message) {
        if (to == null) return;
        Sender s = SenderUtils.getSender(to);
        if (! SLAPI.isReady()) {
            s.sendMessage(message);
        } else {
            s.sendMessage(MessageUtils.replaceAllPlayerBungee(otherUUID, message));
        }
    }

    /**
     * Sends a formatted message to a Bukkit {@link org.bukkit.command.CommandSender},
     * resolving placeholders relative to the provided {@link CosmicSender}.
     *
     * @param to      the recipient; does nothing if {@code null}
     * @param other   the sender used as the context for placeholder resolution
     * @param message the message to send, may contain colour codes and placeholders
     */
    public void sendMessage(@Nullable CommandSender to, CosmicSender other, String message) {
        if (to == null) return;
        Sender s = SenderUtils.getSender(to);
        if (! SLAPI.isReady()) {
            s.sendMessage(message);
        } else {
            s.sendMessage(MessageUtils.replaceAllPlayerBungee(other, message));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void sendMessage(@Nullable CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(Bukkit.getPlayer(UUID.fromString(to.getUuid())), message);
        else sendMessage(Bukkit.getConsoleSender(), message);
    }
    
    /** {@inheritDoc} */
    @Override
    public void sendMessage(@Nullable CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(Bukkit.getPlayer(UUID.fromString(to.getUuid())), otherUUID, message);
        else sendMessage(Bukkit.getConsoleSender(), otherUUID, message);
    }

    /** {@inheritDoc} */
    @Override
    public void sendMessage(@Nullable CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        if (to instanceof CosmicPlayer) sendMessage(Bukkit.getPlayer(UUID.fromString(to.getUuid())), other, message);
        else sendMessage(Bukkit.getConsoleSender(), other, message);
    }

    /**
     * Sends a message to a Bukkit {@link org.bukkit.command.CommandSender} with
     * placeholder replacements applied relative to the sender, but without any
     * additional formatting or wrapping performed by the BOU Sender layer.
     *
     * @param to      the recipient; does nothing if {@code null}
     * @param message the message to send
     */
    public void sendMessageRaw(CommandSender to, String message) {
        if (to == null) return;

        String r = message;
        if (SLAPI.isReady()) {
            r = replaceAllPlayerBungee(to, message);
        }

        to.sendMessage(r);
    }

    /**
     * Sends a raw message to a Bukkit {@link org.bukkit.command.CommandSender},
     * resolving placeholders relative to the player identified by
     * {@code otherUUID}.
     *
     * @param to        the recipient; does nothing if {@code null}
     * @param otherUUID the UUID of the player used for placeholder resolution
     * @param message   the message to send
     */
    public void sendMessageRaw(CommandSender to, String otherUUID, String message) {
        if (to == null) return;

        String r = message;
        if (SLAPI.isReady()) {
            r = MessageUtils.replaceAllPlayerBungee(otherUUID, message);
        }

        to.sendMessage(r);
    }

    /**
     * Sends a raw message to a Bukkit {@link org.bukkit.command.CommandSender},
     * resolving placeholders relative to the provided {@link CosmicSender}.
     *
     * @param to      the recipient; does nothing if {@code null}
     * @param other   the sender used as the context for placeholder resolution
     * @param message the message to send
     */
    public void sendMessageRaw(CommandSender to, CosmicSender other, String message) {
        if (to == null) return;

        String r = message;
        if (SLAPI.isReady()) {
            r = MessageUtils.replaceAllPlayerBungee(other, message);
        }

        to.sendMessage(r);
    }

    /** {@inheritDoc} */
    @Override
    public void sendMessageRaw(@Nullable CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(Bukkit.getPlayer(UUID.fromString(to.getUuid())), message);
        else sendMessageRaw(Bukkit.getConsoleSender(), message);
    }

    /** {@inheritDoc} */
    @Override
    public void sendMessageRaw(@Nullable CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(Bukkit.getPlayer(UUID.fromString(to.getUuid())), otherUUID, message);
        else sendMessageRaw(Bukkit.getConsoleSender(), otherUUID, message);
    }

    /** {@inheritDoc} */
    @Override
    public void sendMessageRaw(@Nullable CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(Bukkit.getPlayer(UUID.fromString(to.getUuid())), other, message);
        else sendMessageRaw(Bukkit.getConsoleSender(), other, message);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Looks up the Bukkit {@link Player} for the given {@link CosmicSender}
     * and calls {@link Player#sendTitle} with the main and sub-title strings and
     * the fade-in, stay, and fade-out tick values from {@code title}.
     */
    @Override
    public void sendTitle(CosmicSender player, CosmicTitle title) {
        Player p = StreamlineSpigot.getPlayer(player.getUuid());
        if (p == null) {
            MessageUtils.logInfo("Could not send a title to a player because player is null!");
            return;
        }


        p.sendTitle(title.getMain(),
                title.getSub(),
                (int) title.getFadeIn(),
                (int) title.getStay(),
                (int) title.getFadeOut()
        );
    }

    /** {@inheritDoc} */
    @Override
    public String codedString(String from) {
        return codedStringBOU(from);
    }

    /** {@inheritDoc} */
    @Override
    public String stripColor(String string){
        return ChatColor.stripColor(string).replaceAll("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", "");
    }

    /**
     * Translates colour codes and formatting tags in {@code value} using the BOU
     * message utilities and returns the resulting coloured string.
     *
     * @param value the raw string with colour codes
     * @return the formatted string ready for display
     */
    public String codedStringBOU(String value) {
        return host.plas.bou.utils.MessageUtils.codedString(value); // Already new-lined.
    }
    
    /**
     * Converts {@code value} (which may contain colour codes and formatting tags)
     * into an array of BungeeCord {@link net.md_5.bungee.api.chat.BaseComponent}s
     * using the BOU {@link host.plas.bou.utils.ColorUtils} utilities.
     *
     * @param value the raw string with colour codes
     * @return the array of base components representing the formatted text
     */
    public BaseComponent[] colorizeBOU(String value) {
        return ColorUtils.color(value);
    }
    
    /**
     * Converts {@code from} into an array of
     * {@link net.md_5.bungee.api.chat.BaseComponent}s.
     *
     * <p>Convenience alias for {@link #colorizeBOU(String)}.
     *
     * @param from the raw string with colour codes
     * @return the array of base components representing the formatted text
     */
    public BaseComponent[] codedText(String from) {
        return colorizeBOU(from);
    }

    /**
     * Applies a "hard" colour translation on {@code value} using
     * {@link host.plas.bou.utils.ColorUtils#colorizeHard(String)}, which
     * performs a more aggressive conversion of colour codes than standard
     * {@link #codedString(String)}.
     *
     * @param value the raw string
     * @return the fully coloured string
     */
    public String colorizeHard(String value) {
        return ColorUtils.colorizeHard(value);
    }

    /**
     * Applies all PlaceholderAPI and Streamline placeholder replacements to
     * {@code of}, using the given Bukkit {@link org.bukkit.command.CommandSender}
     * as the context for resolution.
     *
     * <p>If the sender cannot be resolved to a {@link CosmicSender}, the
     * original string is returned unchanged.
     *
     * @param sender the Bukkit sender providing placeholder context
     * @param of     the string containing placeholders
     * @return the string with all applicable placeholders replaced
     */
    public String replaceAllPlayerBungee(CommandSender sender, String of) {
        CosmicSender s = UserManager.getInstance().getOrCreateSender(sender).orElse(null);
        if (s == null) {
            return of; // If we can't get the sender, just return the original string.
        }

        return MessageUtils.replaceAllPlayerBungee(s, of);
    }
}
