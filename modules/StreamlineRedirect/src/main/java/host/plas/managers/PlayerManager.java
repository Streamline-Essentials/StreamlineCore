package host.plas.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import host.plas.StreamlineRedirect;
import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

public class PlayerManager {
    @Getter @Setter
    private static Cache<String, ConcurrentSkipListMap<String, Integer>> tickedPlayers = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .build();

    public static void addPlayer(String uuid) {
        if (hasPlayer(uuid)) return;

        tickedPlayers.put(uuid, new ConcurrentSkipListMap<>());
    }

    public static boolean hasPlayer(String uuid) {
        return tickedPlayers.asMap().containsKey(uuid);
    }

    public static void removePlayer(String uuid) {
        tickedPlayers.invalidate(uuid);
    }

    public static int setPlayerAt(String uuid, String redirectIdentifier, int ticks) {
        ConcurrentSkipListMap<String, Integer> playerTicks = tickedPlayers.getIfPresent(uuid);
        if (playerTicks == null) {
            playerTicks = new ConcurrentSkipListMap<>();
        }

        playerTicks.put(redirectIdentifier, ticks);

        tickedPlayers.put(uuid, playerTicks);

        return ticks;
    }

    public static int tickPlayerOrGet(String uuid, String redirectIdentifier) {
        if (hasPlayer(uuid)) {
            return tickPlayer(uuid, redirectIdentifier);
        } else {
            return initPlayer(uuid, redirectIdentifier);
        }
    }

    public static int initPlayer(String uuid, String redirectIdentifier) {
        addPlayer(uuid);

        ConcurrentSkipListMap<String, Integer> playerTicks = new ConcurrentSkipListMap<>();
        playerTicks.put(redirectIdentifier, 0);

        tickedPlayers.put(uuid, playerTicks);

        return 0;
    }

    public static int tickPlayer(String uuid, String redirectIdentifier) {
        ConcurrentSkipListMap<String, Integer> playerTicks = tickedPlayers.getIfPresent(uuid);
        if (playerTicks == null) {
            playerTicks = new ConcurrentSkipListMap<>();
        }

        int ticks = playerTicks.getOrDefault(redirectIdentifier, 0);
        ticks ++;

        playerTicks.put(redirectIdentifier, ticks);

        tickedPlayers.put(uuid, playerTicks);

        return ticks;
    }

    public static int getAt(String uuid, String redirectIdentifier) {
        if (! hasPlayer(uuid)) return -1;

        return Optional.ofNullable(tickedPlayers.getIfPresent(uuid)).orElse(new ConcurrentSkipListMap<>()).getOrDefault(redirectIdentifier, -1);
    }
}
