package host.plas.data;

import host.plas.StreamersUnite;
import host.plas.managers.TimedEntry;
import lombok.Getter;
import singularity.data.console.CosmicSender;
import singularity.utils.UserUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class LiveManager {
    /**
     * UUIDs of the streamers who are live. Keyed by UUID rather than by sender object, since
     * the same player can be represented by different sender instances over a session.
     */
    @Getter
    private static final Set<String> currentlyLive = ConcurrentHashMap.newKeySet();

    /** The link each live streamer last announced with {@code /imlive}, by UUID. */
    private static final Map<String, String> liveLinks = new ConcurrentHashMap<>();

    /** When each streamer last announced with {@code /imlive}, by UUID. */
    private static final Map<String, Long> lastAnnounce = new ConcurrentHashMap<>();

    public static void goLive(CosmicSender player) {
        currentlyLive.add(player.getUuid());
    }

    public static void goLive(UUID player) {
        currentlyLive.add(player.toString());
    }

    public static void goOffline(CosmicSender player) {
        goOffline(player.getUuid());
    }

    public static void goOffline(UUID player) {
        goOffline(player.toString());
    }

    public static void goOffline(String uuid) {
        currentlyLive.remove(uuid);
        liveLinks.remove(uuid);
    }

    public static boolean isLive(CosmicSender player) {
        return player != null && currentlyLive.contains(player.getUuid());
    }

    public static boolean isLive(UUID player) {
        return currentlyLive.contains(player.toString());
    }

    /** The link a live streamer announced, falling back to the link in their setup. */
    public static String getLink(StreamerSetup setup) {
        String link = liveLinks.get(setup.getStreamerUuid().toString());
        return link == null || link.isEmpty() ? setup.getStreamLink() : link;
    }

    public static void setLiveLink(String uuid, String link) {
        if (link == null || link.isEmpty()) liveLinks.remove(uuid);
        else liveLinks.put(uuid, link);
    }

    /** Milliseconds until {@code uuid} may announce with {@code /imlive} again; 0 or less when now. */
    public static long announceCooldownLeft(String uuid) {
        long cooldown = StreamersUnite.getMainConfig().getAnnounceCooldownSeconds() * 1000L;
        return lastAnnounce.getOrDefault(uuid, 0L) + cooldown - System.currentTimeMillis();
    }

    public static void markAnnounced(String uuid) {
        lastAnnounce.put(uuid, System.currentTimeMillis());
    }

    public static void clear() {
        currentlyLive.clear();
        liveLinks.clear();
    }

    public static ConcurrentSkipListSet<StreamerSetup> getCurrentlyLiveSetups() {
        ConcurrentSkipListSet<StreamerSetup> r = new ConcurrentSkipListSet<>();

        for (String uuid : currentlyLive) {
            Optional<StreamerSetup> setup = StreamersUnite.getStreamerConfig().getSetup(uuid);

            setup.ifPresent(r::add);
        }

        return r;
    }

    public static boolean initiateLive(CosmicSender player) {
        if (TimedEntry.hasEntry("go-live", player.getUuid())) {
            return false;
        }

        AtomicBoolean r = new AtomicBoolean(false);

        StreamersUnite.getStreamerConfig().getSetup(player.getUuid()).ifPresent(setup -> {
            if (! player.isOnline()) {
                goLive(player);
                return;
            }

            if (StreamersUnite.getMainConfig().announceGoLive()) {
                if (! TimedEntry.hasEntry("broadcast", player.getUuid())) {
                    setup.tellStreamLinkGoingLive(UserUtils.getOnlineSenders().values().toArray(CosmicSender[]::new));

                    new TimedEntry<>(20 * 60 * 30, "broadcast", player.getUuid(), player);
                }
            }

            setup.getGoLiveCommands().forEach(command -> {
                if (command == null) return;

                String playerName = player.getUuid();
                try {
                    playerName = player.getDisplayName();
                } catch (Exception e) {
                    playerName = "";
                    // no error
                }

                String c = command
                        .replace("%player_name%", player.getCurrentName())
                        .replace("%player_uuid%", player.getUuid())
                        .replace("%player_display_name%", playerName)
                        ;

                if (c.startsWith("/")) c = c.substring(1);

                boolean asConsole = false;
                if (c.startsWith("!C!")) {
                    c = c.substring("!C!".length());
                    asConsole = true;
                }

                while (c.startsWith(" ")) c = c.substring(1);

                if (asConsole) {
                    UserUtils.getConsole().runCommand(c);
                } else {
                    player.runCommand(c);
                }
            });
            goLive(player);

            new TimedEntry<>(20 * 60 * 30, "go-live", player.getUuid(), player);

            r.set(true);
        });

        return r.get();
    }

    public static boolean initiateOffline(CosmicSender player) {
        if (TimedEntry.hasEntry("go-offline", player.getUuid())) {
            return false;
        }

        AtomicBoolean r = new AtomicBoolean(false);

        StreamersUnite.getStreamerConfig().getSetup(player.getUuid()).ifPresent(setup -> {
            if (! player.isOnline()) {
                goOffline(player);
                return;
            }

            if (StreamersUnite.getMainConfig().announceGoOffline()) {
                if (! TimedEntry.hasEntry("broadcast", player.getUuid())) {
                    setup.tellStreamLinkGoingOffline(UserUtils.getOnlineSenders().values().toArray(CosmicSender[]::new));

                    new TimedEntry<>(20 * 60 * 30, "broadcast", player.getUuid(), player);
                }
            }

            setup.getGoOfflineCommands().forEach(command -> {
                if (command == null) return;

                String playerName = player.getUuid();
                try {
                    playerName = player.getDisplayName();
                } catch (Exception e) {
                    playerName = "";
                    // no error
                }

                String c = command
                        .replace("%player_name%", player.getCurrentName())
                        .replace("%player_uuid%", player.getUuid())
                        .replace("%player_display_name%", playerName)
                        ;

                if (c.startsWith("/")) c = c.substring(1);

                boolean asConsole = false;
                if (c.startsWith("!C!")) {
                    c = c.substring("!C!".length());
                    asConsole = true;
                }

                while (c.startsWith(" ")) c = c.substring(1);

                if (asConsole) {
                    UserUtils.getConsole().runCommand(c);
                } else {
                    player.runCommand(c);
                }
            });
            goOffline(player);

            new TimedEntry<>(20 * 60 * 30, "go-offline", player.getUuid(), player);

            r.set(true);
        });

        return r.get();
    }

    public static boolean broadcastLive(CosmicSender player) {
        if (TimedEntry.hasEntry("broadcast", player.getUuid())) {
            return false;
        }

        AtomicBoolean r = new AtomicBoolean(false);

        StreamersUnite.getStreamerConfig().getSetup(player.getUuid()).ifPresent(setup -> {
            if (! player.isOnline()) {
                return;
            }

            setup.tellStreamLinkCurrentlyLive(UserUtils.getOnlineSenders().values().toArray(CosmicSender[]::new));

            new TimedEntry<>(20 * 60 * 30, "broadcast", player.getUuid(), player);

            r.set(true);
        });

        return r.get();
    }
}
