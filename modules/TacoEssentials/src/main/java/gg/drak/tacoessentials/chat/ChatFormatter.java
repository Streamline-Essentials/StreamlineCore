package gg.drak.tacoessentials.chat;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.commands.Perms;
import gg.drak.tacoessentials.data.TacoConfig;
import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseEventPriority;
import gg.drak.thebase.events.processing.BaseProcessor;
import net.streamline.api.permissions.Permissions;
import singularity.Singularity;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.events.server.CosmicChatEvent;
import singularity.modules.ModuleUtils;
import singularity.objects.ClickableMessage;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Public chat in {@code chat.format}: the sender's part carries a hover card ({@code chat.hover})
 * and a click that starts a {@code /msg}, and links in the message are clickable.
 *
 * <p>Runs at {@link BaseEventPriority#HIGH}: after StreamlineMessaging's channels and this module's
 * mute and alias-prompt handling, which cancel what they take, and before LinkParser, which leaves
 * cancelled lines alone. The format is resolved before the message is put in, so text a player
 * types is never read as a placeholder; colour codes in it only count with
 * {@code tacoessentials.chat.color}.</p>
 */
public class ChatFormatter implements BaseEventListener {
    /** Legacy and hex colour codes, as players type them. */
    private static final Pattern COLOR_CODES = Pattern.compile("(?i)&#[0-9a-f]{6}|[&§][0-9a-fk-orx]");
    private static final Pattern ACTIVE_CODE = Pattern.compile(
            "(?i)&#[0-9a-f]{6}|\\{#[0-9a-f]{6}}|<#[0-9a-f]{6}>|[&§]x(?:[&§][0-9a-f]){6}|[&§][0-9a-fk-or]|#[0-9a-f]{6}");
    private static final String MESSAGE = "%message%";

    /** Short tokens from the documented format, and the module placeholders they stand for. */
    private static final Map<String, String> ALIASES = new LinkedHashMap<>();

    static {
        ALIASES.put("%stars%", "%streamlinecollections_stars%");
        ALIASES.put("%tag-left%", "%streamlinetags_tag_left%");
        ALIASES.put("%tag-right%", "%streamlinetags_tag_right%");
        ALIASES.put("%tag%", "%streamlinetags_tag%");
    }

    @BaseProcessor(priority = BaseEventPriority.HIGH)
    public void onChat(CosmicChatEvent event) {
        if (event.isCanceled()) return;
        TacoConfig config = TacoEssentials.getConfig();
        if (config == null || ! config.chatEnabled()) return;
        if (! (event.getSender() instanceof CosmicPlayer)) return;
        CosmicPlayer sender = (CosmicPlayer) event.getSender();
        String message = event.getMessage();
        if (message == null || message.isEmpty()) return;

        if (! allowed(sender, Perms.CHAT_COLOR)) message = COLOR_CODES.matcher(message).replaceAll("");

        String format = resolve(sender, config.chatFormat());
        int at = format.indexOf(MESSAGE);
        String before = at < 0 ? format : format.substring(0, at);
        String after = at < 0 ? "" : format.substring(at + MESSAGE.length());

        ClickableMessage line = new ClickableMessage().text(before);
        if (config.chatHoverEnabled() && ! before.isEmpty()) {
            List<String> hover = new ArrayList<>();
            for (String hoverLine : config.chatHover()) hover.add(resolve(sender, hoverLine));
            line.hover(hover).suggest("/msg " + sender.getCurrentName() + " ");
        }
        if (at >= 0) {
            String body = activeCodes(before) + message;
            if (config.chatClickableLinks()) line.textWithLinks(body, false, "&7Open &f%url%");
            else line.text(body);
            line.text(after.isEmpty() ? after : activeCodes(before + message) + after);
        }

        event.setCanceled(true);
        for (CosmicPlayer recipient : UserUtils.getOnlinePlayers().values()) line.send(recipient);
        CosmicSender console = UserUtils.getConsole();
        if (console != null) line.send(console);
    }

    /**
     * Fills in a format line for {@code sender}: the short tokens, Streamline and PlaceholderAPI
     * placeholders, then {@code %prefix%}, {@code %suffix%}, {@code %player%} and
     * {@code %displayname%}. Those last values go in after the placeholder pass, so a nickname that
     * looks like a placeholder stays as typed. {@code %message%} is left for the caller.
     */
    public static String resolve(CosmicPlayer sender, String format) {
        String text = format == null ? "" : format;
        for (Map.Entry<String, String> alias : ALIASES.entrySet()) text = text.replace(alias.getKey(), alias.getValue());

        Map<String, String> own = new LinkedHashMap<>();
        own.put("%message%", MESSAGE);
        own.put("%displayname%", displayName(sender));
        own.put("%prefix%", UserUtils.getPrefix(sender));
        own.put("%suffix%", UserUtils.getSuffix(sender));
        own.put("%player%", sender.getCurrentName());

        // Each own token becomes a marker the placeholder passes cannot match.
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, String> token : own.entrySet()) {
            if (! text.contains(token.getKey())) continue;
            text = text.replace(token.getKey(), "\u0000" + values.size() + "\u0000");
            values.add(token.getValue() == null ? "" : token.getValue());
        }

        text = ModuleUtils.replacePlaceholders(sender, text);
        text = PapiBridge.apply(sender.getUuid(), text);

        for (int i = 0; i < values.size(); i++) text = text.replace("\u0000" + i + "\u0000", values.get(i));
        return text;
    }

    /** {@code display-name.format} for {@code sender}. */
    public static String displayName(CosmicPlayer sender) {
        String nickname = sender.getMeta().getNickname();
        if (nickname == null || nickname.trim().isEmpty()) nickname = sender.getCurrentName();
        String prefix = UserUtils.getPrefix(sender);
        String suffix = UserUtils.getSuffix(sender);
        return TacoEssentials.getConfig().displayNameFormat()
                .replace("%prefix%", prefix == null ? "" : prefix)
                .replace("%suffix%", suffix == null ? "" : suffix)
                .replace("%nickname%", nickname)
                .replace("%player%", sender.getCurrentName());
    }

    /**
     * Whether {@code sender} has {@code permission}. Without LuckPerms, server operators count as
     * having it, since the platform's own permissions may know nothing of this node.
     */
    private static boolean allowed(CosmicPlayer sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        if (Permissions.isHooked()) return false;
        return Singularity.gameplay().map(gameplay -> gameplay.isOperator(sender.getUuid())).orElse(false);
    }

    /** The colour and formatting codes in effect at the end of {@code text}. */
    private static String activeCodes(String text) {
        Matcher matcher = ACTIVE_CODE.matcher(text);
        String colour = "";
        StringBuilder formats = new StringBuilder();
        while (matcher.find()) {
            String code = matcher.group();
            char last = Character.toLowerCase(code.charAt(code.length() - 1));
            boolean legacy = code.length() == 2;
            if (legacy && last == 'r') {
                colour = "";
                formats.setLength(0);
            } else if (legacy && last >= 'k' && last <= 'o') {
                formats.append(code);
            } else {
                colour = code;
                formats.setLength(0);
            }
        }
        return colour + formats;
    }
}
