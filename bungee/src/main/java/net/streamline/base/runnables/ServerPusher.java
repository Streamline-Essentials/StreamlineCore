package net.streamline.base.runnables;

import net.md_5.bungee.api.config.ServerInfo;
import net.streamline.api.SLAPI;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.messages.builders.ServerInfoMessageBuilder;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.base.Streamline;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerPusher extends BaseRunnable {
    public ServerPusher() {
        super(200, 3600);
    }

    @Override
    public void run() {
        ConcurrentSkipListMap<String, ServerInfo> r = new ConcurrentSkipListMap<>(Streamline.getInstance().getProxy().getServers());

        ConcurrentSkipListSet<StreamPlayer> players = new ConcurrentSkipListSet<>();

        ModuleUtils.getOnlinePlayers().forEach((s, streamPlayer) -> {
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            players.forEach(p -> {
                if (p.getServer().getIdentifier().equals(streamPlayer.getServer().getIdentifier())) atomicBoolean.set(true);
            });
            if (atomicBoolean.get()) return;
            players.add(streamPlayer);
        });

        r.forEach((s, serverInfo) -> {
            ConcurrentSkipListSet<String> uuids = new ConcurrentSkipListSet<>();

            serverInfo.getPlayers().forEach(proxiedPlayer -> {
                uuids.add(proxiedPlayer.getUniqueId().toString());
            });

            StreamlineServerInfo ssi = new StreamlineServerInfo(
                    serverInfo.getName(), serverInfo.getName(), serverInfo.getMotd(), serverInfo.getSocketAddress().toString(), uuids);

            players.forEach(p -> {
                ProxiedMessage toSend = ServerInfoMessageBuilder.build(p, ssi);

                SLAPI.getInstance().getProxyMessenger().sendMessage(toSend);
            });
        });
    }
}
