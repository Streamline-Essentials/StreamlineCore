package host.plas.linkparser;

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
import singularity.utils.Links;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Replaces chat lines holding links, from allowed players, with the same line in which every
 * link is clickable.
 *
 * <p>Runs at {@link BaseEventPriority#HIGHEST}, after chat modules that take messages over
 * (StreamlineMessaging's channels, for one): a message one of them already cancelled is left
 * alone, so every message is shown once.</p>
 */
public class ChatListener implements BaseEventListener {
    /** Legacy and hex colour codes, as players type them. */
    private static final Pattern COLOR_CODES = Pattern.compile("(?i)&#[0-9a-f]{6}|[&§][0-9a-fk-orx]");

    @BaseProcessor(priority = BaseEventPriority.HIGHEST)
    public void onChat(CosmicChatEvent event) {
        if (event.isCanceled()) return;
        LinkParserConfig config = LinkParser.getConfig();
        if (Singularity.isProxy() && ! config.isProxyEnabled()) return;

        if (! (event.getSender() instanceof CosmicPlayer)) return;
        CosmicPlayer sender = (CosmicPlayer) event.getSender();
        String message = event.getMessage();
        if (message == null || ! Links.contains(message, config.isBareDomains())) return;
        if (! isAllowed(sender, config.getPermission())) return;

        if (! isAllowed(sender, config.getColorPermission())) message = COLOR_CODES.matcher(message).replaceAll("");

        // The format is resolved first and the message put in afterwards, so text a player types
        // is never read as a placeholder.
        String format = ModuleUtils.replacePlaceholders(sender, config.getFormat());
        int at = format.indexOf("%message%");
        ClickableMessage line = new ClickableMessage();
        if (at < 0) {
            line.textWithLinks(format, config.isBareDomains(), config.getLinkHover());
        } else {
            String before = format.substring(0, at);
            String after = format.substring(at + "%message%".length());
            // The message starts with whatever colour the format leaves in effect.
            line.textWithLinks(before + message + after, config.isBareDomains(), config.getLinkHover());
        }

        event.setCanceled(true);
        for (CosmicSender recipient : recipients(sender, config)) line.send(recipient);
    }

    /**
     * Whether {@code sender} has {@code permission}. Without LuckPerms, server operators count
     * as having it, since the platform's own permissions may know nothing of this node.
     */
    static boolean isAllowed(CosmicPlayer sender, String permission) {
        if (permission == null || permission.isEmpty()) return true;
        if (sender.hasPermission(permission)) return true;
        if (Permissions.isHooked()) return false;
        return Singularity.gameplay().map(gameplay -> gameplay.isOperator(sender.getUuid())).orElse(false);
    }

    /** Everyone online here, plus the console; on a proxy, by the configured scope. */
    private static List<CosmicSender> recipients(CosmicPlayer sender, LinkParserConfig config) {
        List<CosmicSender> recipients = new ArrayList<>();
        boolean byServer = Singularity.isProxy() && ! config.isProxyNetworkScope();
        String server = sender.getServerName();
        for (CosmicPlayer player : UserUtils.getOnlinePlayers().values()) {
            if (byServer && ! Objects.equals(server, player.getServerName())) continue;
            recipients.add(player);
        }
        CosmicSender console = UserUtils.getConsole();
        if (console != null) recipients.add(console);
        return recipients;
    }
}
