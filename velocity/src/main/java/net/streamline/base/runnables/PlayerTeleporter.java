package net.streamline.base.runnables;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.Setter;
import net.streamline.base.StreamlineVelocity;
import singularity.configs.given.GivenConfigs;
import singularity.data.teleportation.TPTicket;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerTeleporter extends Thread {
    public static final long TICKING_FREQUENCY = 50L;

    @Getter @Setter
    private static PlayerTeleporter instance;

    public static void init() {
        instance = new PlayerTeleporter();
        instance.start();
    }

    public static void stopInstance() {
        try {
            if (instance != null) {
                instance.interrupt();
                instance = null;
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Getter @Setter
    private static AtomicReference<TeleportStage> stage = new AtomicReference<>(TeleportStage.READY);
    @Getter @Setter
    private static AtomicLong lastRun = new AtomicLong(0);

    public enum TeleportStage {
        COLLECTION,
        TELEPORTATION,
        READY,
        ;
    }

    public static boolean isAbleToRunAgain() {
        return lastRun.get() + TICKING_FREQUENCY < System.currentTimeMillis();
    }

    public static void setLastRun() {
        lastRun.set(System.currentTimeMillis());
    }

    public PlayerTeleporter() {
        super("SL - Player Teleporter");
    }

    @Override
    public void run() {
        if (! isAbleToRunAgain()) return;

        setLastRun();

        stage.set(TeleportStage.COLLECTION);
        ConcurrentSkipListSet<TPTicket> tickets = GivenConfigs.getMainDatabase().pullAllTPTickets().join();

        stage.set(TeleportStage.TELEPORTATION);
        tickets.forEach(ticket -> {
            if (ticket.getCreateDate().before(new Date(System.currentTimeMillis() - 7 * 1000))) {
                ticket.clear();
                return;
            }

            Player player = StreamlineVelocity.getInstance().getProxy().getPlayer(UUID.fromString(ticket.getIdentifier())).orElse(null);
            if (player == null) {
                ticket.clear();
                return;
            }

            teleportPlayerAsync(player, ticket.getTargetServer().getIdentifier());
        });

        stage.set(TeleportStage.READY);
    }

    private static void teleportPlayerAsync(Player player, String server) {
        StreamlineVelocity.getInstance().getProxy().getServer(server).ifPresent(serverInfo -> {
            ConnectionRequestBuilder builder = player.createConnectionRequest(serverInfo);
            builder.connect().join();
        });
    }
}
