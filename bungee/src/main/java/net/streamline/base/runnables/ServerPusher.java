package net.streamline.base.runnables;

import net.md_5.bungee.api.config.ServerInfo;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.messages.builders.ServerInfoMessageBuilder;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.savables.users.StreamlinePlayer;
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

        ConcurrentSkipListSet<StreamlinePlayer> players = new ConcurrentSkipListSet<>();

        ModuleUtils.getOnlinePlayers().forEach((s, streamlinePlayer) -> {
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            players.forEach(p -> {
                if (p.getLatestServer().equals(streamlinePlayer.getLatestServer())) atomicBoolean.set(true);
            });
            if (atomicBoolean.get()) return;
            players.add(streamlinePlayer);
        });

        r.forEach((s, serverInfo) -> {
            ConcurrentSkipListSet<String> uuids = new ConcurrentSkipListSet<>();

            serverInfo.getPlayers().forEach(proxiedPlayer -> {
                uuids.add(proxiedPlayer.getUniqueId().toString());
            });

            StreamlineServerInfo ssi = new StreamlineServerInfo(
                    serverInfo.getName(), serverInfo.getName(), serverInfo.getMotd(), serverInfo.getSocketAddress().toString(), uuids);

            GivenConfigs.getProfileConfig().updateServerInfo(ssi);

            players.forEach(p -> {
                ProxiedMessage toSend = ServerInfoMessageBuilder.build(p, ssi);

                SLAPI.getInstance().getProxyMessenger().sendMessage(toSend);
            });
        });
    }
}
