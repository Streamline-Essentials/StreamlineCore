package net.streamline.base.runnables;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.streamline.api.SLAPI;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.messages.builders.ServerInfoMessageBuilder;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.base.StreamlineVelocity;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerPusher extends BaseRunnable {
    public ServerPusher() {
        super(200, 3600);
    }

    @Override
    public void run() {
        ConcurrentSkipListMap<String, RegisteredServer> r = new ConcurrentSkipListMap<>();
        StreamlineVelocity.getInstance().getProxy().getAllServers().forEach(registeredServer -> {
            r.put(registeredServer.getServerInfo().getName(), registeredServer);
        });

        ConcurrentSkipListSet<StreamPlayer> players = new ConcurrentSkipListSet<>();

        ModuleUtils.getOnlinePlayers().forEach((s, streamPlayer) -> {
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            players.forEach(p -> {
                if (p.getServer().getIdentifier().equals(streamPlayer.getServer().getIdentifier())) atomicBoolean.set(true);
            });
            if (atomicBoolean.get()) return;
            players.add(streamPlayer);
        });

        r.forEach((s, registeredServer) -> {
            ConcurrentSkipListSet<String> uuids = new ConcurrentSkipListSet<>();

            registeredServer.getPlayersConnected().forEach(proxiedPlayer -> {
                uuids.add(proxiedPlayer.getUniqueId().toString());
            });

            StreamlineServerInfo ssi = new StreamlineServerInfo(
                    registeredServer.getServerInfo().getName(), registeredServer.getServerInfo().getName(),
                    registeredServer.getServerInfo().getName(), registeredServer.getServerInfo().getAddress().toString(), uuids);

            players.forEach(p -> {
                ProxiedMessage toSend = ServerInfoMessageBuilder.build(p, ssi);

                SLAPI.getInstance().getProxyMessenger().sendMessage(toSend);
            });
        });
    }
}