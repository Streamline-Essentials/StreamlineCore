package host.plas.commands;

import host.plas.StreamersUnite;
import host.plas.data.LiveManager;
import host.plas.data.StreamerSetup;
import host.plas.managers.StreamerUtils;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;
import singularity.utils.Links;
import singularity.utils.UserUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@code /imlive (link) (streamer)}: broadcasts a registered streamer's clickable stream link
 * to everyone online and marks them live. The link defaults to the one in the streamer's setup.
 * Streamers announcing themselves wait {@code imlive.announce-cooldown-seconds} between
 * announcements; announcing for someone else needs {@code streamersunite.command.imlive.others}
 * and skips the cooldown.
 */
public class ImLiveCMD extends ModuleCommand {
    public static final String OTHERS = "streamersunite.command.imlive.others";

    public ImLiveCMD() {
        super(StreamersUnite.getInstance(),
                "imlive",
                "streamersunite.command.imlive",
                "iamlive", "streamlink");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        CosmicSender sender = context.getSender();
        String rawLink = context.isArgUsable(0) ? context.getStringArg(0) : "";

        CosmicSender streamer;
        boolean self;
        if (context.isArgUsable(1)) {
            if (! sender.hasPermission(OTHERS)) {
                context.sendMessage(message("no-permission-others"));
                return;
            }
            Optional<CosmicSender> target = StreamerUtils.getOrGetSenderByName(context.getStringArg(1));
            if (target.isEmpty()) {
                context.sendMessage(message("unknown-player").replace("%player%", context.getStringArg(1)));
                return;
            }
            streamer = target.get();
            self = false;
        } else {
            if (context.isConsole()) {
                context.sendMessage(message("players-only"));
                return;
            }
            streamer = sender;
            self = true;
        }

        Optional<StreamerSetup> setup = StreamersUnite.getStreamerConfig().getSetup(streamer.getUuid());
        if (setup.isEmpty()) {
            context.sendMessage(message("not-registered").replace("%player%", streamer.getCurrentName()));
            return;
        }

        String link = rawLink.isEmpty() ? setup.get().getStreamLink() : rawLink;
        String url = Links.toClickUrl(link);
        if (url == null) {
            context.sendMessage(rawLink.isEmpty() ? message("usage") : message("invalid-link"));
            return;
        }

        if (self) {
            long wait = LiveManager.announceCooldownLeft(streamer.getUuid());
            if (wait > 0) {
                context.sendMessage(message("cooldown").replace("%time%", formatDuration(wait)));
                return;
            }
        }

        LiveManager.markAnnounced(streamer.getUuid());
        LiveManager.setLiveLink(streamer.getUuid(), url);
        LiveManager.goLive(streamer);

        // Keyed by UUID, so the console is told once whether or not it counts as online.
        Map<String, CosmicSender> recipients = new LinkedHashMap<>(UserUtils.getOnlineSenders());
        CosmicSender console = UserUtils.getConsole();
        if (console != null) recipients.put(console.getUuid(), console);
        setup.get().announce(StreamersUnite.getMainConfig().getImLiveAnnouncement(), url,
                recipients.values().toArray(new CosmicSender[0]));

        if (! self) context.sendMessage(message("announced").replace("%player%", streamer.getCurrentName()));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        ConcurrentSkipListSet<String> options = new ConcurrentSkipListSet<>();
        if (context.getArgCount() <= 1) {
            StreamersUnite.getStreamerConfig().getSetup(context.getSender().getUuid())
                    .map(StreamerSetup::getStreamLink)
                    .filter(link -> ! link.isEmpty())
                    .ifPresent(options::add);
            options.add("https://");
        } else if (context.getArgCount() == 2 && context.getSender().hasPermission(OTHERS)) {
            options.addAll(StreamerCMD.streamerNames());
        }
        return options;
    }

    private static String message(String key) {
        return StreamersUnite.getMainConfig().getMessage(key);
    }

    /** {@code 1h 2m 3s}, leaving out leading zero units. */
    static String formatDuration(long millis) {
        long seconds = Math.max(1L, (millis + 999L) / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        StringBuilder builder = new StringBuilder();
        if (hours > 0) builder.append(hours).append("h ");
        if (hours > 0 || minutes > 0) builder.append(minutes).append("m ");
        builder.append(secs).append('s');
        return builder.toString();
    }
}
