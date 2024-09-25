package net.streamline.platform.messaging;

import singularity.data.players.CosmicPlayer;
import net.streamline.base.Streamline;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import singularity.messages.ProxyMessenger;
import singularity.messages.events.ProxyMessageInEvent;
import singularity.messages.proxied.ProxiedMessage;
import singularity.messages.proxied.ProxiedMessageManager;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.UUID;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxiedMessage message) {
        if (Streamline.getInstance().getProxy().getOnlinePlayers().isEmpty()) {
            ProxiedMessageManager.pendMessage(message);
            return;
        }

        CosmicPlayer carrier = message.getCarrier();
        if (carrier == null) {
            Optional<CosmicPlayer> optional = UserUtils.getOnlinePlayers().values().stream().findFirst();
            if (optional.isEmpty()) {
                ProxiedMessageManager.pendMessage(message);
                return;
            }

            carrier = optional.get();
            return;
        }

        Player player = Bukkit.getPlayer(UUID.fromString(carrier.getUuid()));
        if (player == null) {
            ProxiedMessageManager.pendMessage(message);
            return;
        }

        player.sendPluginMessage(Streamline.getInstance(), message.getMainChannel(), message.read());
    }

    @Override
    public void receiveMessage(ProxyMessageInEvent event) {
        // implemented else where.
    }
}
