package net.streamline.platform.listeners;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import host.plas.bou.utils.ClassHelper;
import host.plas.bou.utils.ColorUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.CachedServerIcon;
import singularity.events.server.ping.PingReceivedEvent;
import singularity.objects.CosmicFavicon;
import singularity.objects.PingedResponse;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PaperListener implements Listener {
    public PaperListener() {
        if (! ClassHelper.isPaper()) return;

        Bukkit.getPluginManager().registerEvents(this, Streamline.getInstance());
        Streamline.getInstance().logInfo("PaperListener registered.");
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        String hostName;
        try {
            hostName = event.getAddress().getHostName();
        } catch (Throwable e) {
            hostName = "";
        }

        PingedResponse.Protocol protocol = new PingedResponse.Protocol("latest", 1);

        List<PingedResponse.PlayerInfo> playerInfos = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerInfos.add(new PingedResponse.PlayerInfo(player.getName(), player.getUniqueId().toString()));
        }

        PingedResponse.Players players = new PingedResponse.Players(event.getMaxPlayers(), event.getNumPlayers(),
                playerInfos.toArray(new PingedResponse.PlayerInfo[0]));

        PingedResponse response;
        try {
            response = new PingedResponse(protocol, players, event.getMotd());
        } catch (Throwable e) {
            Streamline.getInstance().logWarning("Failed to create PingedResponse: " + e.getMessage());
            Streamline.getInstance().logWarning(e.getStackTrace());
            return;
        }

        PingReceivedEvent pingReceivedEvent = new PingReceivedEvent(response, hostName).fire();

        if (pingReceivedEvent.isCancelled()) {
            return;
        }

        event.setMotd(Messenger.getInstance().codedString(pingReceivedEvent.getResponse().getDescription()));

        // Set the sample of the server (the players displayed when hovering over the player count)
        try {
            event.getPlayerSample().clear();

            List<PlayerProfile> playerSample = new ArrayList<>();
            for (PingedResponse.PlayerInfo playerInfo : pingReceivedEvent.getResponse().getPlayers().getSample()) {
                try {
                    PlayerProfile profile = Bukkit.getServer().createProfile(playerInfo.getUniqueId());
                    profile.setName(Messenger.getInstance().codedString(playerInfo.getName()));
                    playerSample.add(profile);
                } catch (Throwable e) {
                    // do nothing.
                }
            }

            event.getPlayerSample().addAll(playerSample);
        } catch (Throwable e) {
            Streamline.getInstance().logWarning("Failed to set player sample: " + e.getMessage());
            Streamline.getInstance().logWarning(e.getStackTrace());
        }

        event.setMaxPlayers(pingReceivedEvent.getResponse().getPlayers().getMax());
        event.setNumPlayers(pingReceivedEvent.getResponse().getPlayers().getOnline());

        CosmicFavicon favicon = pingReceivedEvent.getResponse().getFavicon();
        if (favicon != null) {
            try {
                CachedServerIcon icon = Bukkit.loadServerIcon(favicon.getImage());
                event.setServerIcon(icon);
            } catch (Throwable e) {
                Streamline.getInstance().logWarning("Failed to set server icon: " + e.getMessage());
                Streamline.getInstance().logWarning(e.getStackTrace());
            }
        }

        try {
            CachedServerIcon icon = Bukkit.loadServerIcon(Paths.get(pingReceivedEvent.getResponse().getFaviconString()).toFile());
            event.setServerIcon(icon);
        } catch (Exception e) {
            // do nothing.
        }
    }
}
