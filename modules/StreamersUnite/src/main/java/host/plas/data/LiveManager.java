package host.plas.data;

import host.plas.StreamersUnite;
import host.plas.managers.StreamerUtils;
import host.plas.managers.TimedEntry;
import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class LiveManager {
    @Getter @Setter
    private static List<CosmicSender> currentlyLive = new ArrayList<>();

    public static void goLive(CosmicSender player) {
        currentlyLive.add(player);
    }

    public static void goLive(UUID player) {
        Optional<CosmicSender> optional = StreamerUtils.getOrGetSenderByName(player.toString());

        optional.ifPresent(LiveManager::goLive);
    }

    public static void goOffline(CosmicSender player) {
        currentlyLive.remove(player);
    }

    public static void goOffline(UUID player) {
        Optional<CosmicSender> optional = StreamerUtils.getOrGetSenderByName(player.toString());

        optional.ifPresent(LiveManager::goOffline);
    }

    public static boolean isLive(CosmicSender player) {
        return currentlyLive.contains(player);
    }

    public static boolean isLive(UUID player) {
        Optional<CosmicSender> optional = StreamerUtils.getOrGetSenderByName(player.toString());

        return optional.filter(LiveManager::isLive).isPresent();

    }

    public static ConcurrentSkipListSet<StreamerSetup> getCurrentlyLiveSetups() {
        ConcurrentSkipListSet<StreamerSetup> r = new ConcurrentSkipListSet<>();

        for (CosmicSender player : currentlyLive) {
            Optional<StreamerSetup> setup = StreamersUnite.getStreamerConfig().getSetup(player.getUuid());

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
