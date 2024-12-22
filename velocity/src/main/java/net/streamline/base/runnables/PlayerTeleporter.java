package net.streamline.base.runnables;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.Setter;
import net.streamline.base.StreamlineVelocity;
import singularity.configs.given.GivenConfigs;
import singularity.data.teleportation.TPTicket;
import singularity.scheduler.BaseRunnable;
import tv.quaint.objects.AtomicString;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerTeleporter extends BaseRunnable {
    @Getter @Setter
    private static AtomicBoolean checking = new AtomicBoolean(false);

    public PlayerTeleporter() {
        super(0, 10);
    }

    @Override
    public void run() {
        if (checking.get()) return;

        checking.set(true);
        GivenConfigs.getMainDatabase().pullAllTPTickets().whenComplete((tickets, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                checking.set(false);
                return;
            }

            tickets.forEach(ticket -> {
                Player player = StreamlineVelocity.getInstance().getProxy().getPlayer(ticket.getIdentifier()).orElse(null);
                if (player == null) {
                    ticket.clear();
                    return;
                }

                teleportPlayerAsync(player, ticket.getTargetServer().getIdentifier());
            });

            checking.set(false);
        });
    }

    private static void teleportPlayerAsync(Player player, String server) {
        CompletableFuture.runAsync(() -> {
            StreamlineVelocity.getInstance().getProxy().getServer(server).ifPresent(serverInfo -> {
                ConnectionRequestBuilder builder = player.createConnectionRequest(serverInfo);
                builder.connect().join();
            });

            AtomicString playerServer = new AtomicString("");
            player.getCurrentServer().ifPresent(serverInfo -> playerServer.set(serverInfo.getServerInfo().getName()));
            long startTime = System.currentTimeMillis();


            // If success is true OR 5 seconds have passed, stop waiting
            while (! playerServer.get().equals(server) && System.currentTimeMillis() - startTime < 5000) {
                Thread.onSpinWait();
                player.getCurrentServer().ifPresent(serverInfo -> playerServer.set(serverInfo.getServerInfo().getName()));
            }
        });
    }
}
