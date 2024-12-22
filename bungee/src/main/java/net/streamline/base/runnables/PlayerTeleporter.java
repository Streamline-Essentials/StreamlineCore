package net.streamline.base.runnables;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.streamline.base.Streamline;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.teleportation.TPTicket;
import singularity.scheduler.BaseRunnable;
import tv.quaint.objects.AtomicString;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
                ProxiedPlayer player = Streamline.getInstance().getProxy().getPlayer(ticket.getIdentifier());
                if (player == null) {
                    ticket.clear();
                    return;
                }

                teleportPlayerAsync(player, ticket.getTargetServer().getIdentifier());
            });

            checking.set(false);
        });
    }

    private static void teleportPlayerAsync(ProxiedPlayer player, String server) {
        CompletableFuture.runAsync(() -> {
            ServerInfo targetServer = Streamline.getInstance().getProxy().getServerInfo(server);
            if (targetServer != null) {
                ServerConnectRequest request = ServerConnectRequest.builder()
                        .target(targetServer)
                        .reason(ServerConnectEvent.Reason.PLUGIN)
                        .build();
                player.connect(request);
            }

            AtomicString playerServer = new AtomicString("");
            Optional.ofNullable(player.getServer()).ifPresent(s -> playerServer.set(s.getInfo().getName()));

            long startTime = System.currentTimeMillis();
            while (! playerServer.get().equals(server) && System.currentTimeMillis() - startTime < 5000) {
                Thread.onSpinWait();
                Optional.ofNullable(player.getServer()).ifPresent(s -> playerServer.set(s.getInfo().getName()));
            }
        });
    }
}
