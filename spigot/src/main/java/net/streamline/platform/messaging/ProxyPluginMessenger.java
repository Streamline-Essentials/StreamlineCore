package net.streamline.platform.messaging;

import net.streamline.api.SLAPI;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.messages.ProxyMessenger;
import net.streamline.api.messages.builders.ResourcePackMessageBuilder;
import net.streamline.api.messages.builders.ServerInfoMessageBuilder;
import net.streamline.api.messages.builders.StreamPlayerMessageBuilder;
import net.streamline.api.messages.builders.TeleportMessageBuilder;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.messages.proxied.ProxiedMessageManager;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxiedMessage message) {
        if (Streamline.getInstance().getProxy().getOnlinePlayers().isEmpty()) {
            ProxiedMessageManager.pendMessage(message);
            return;
        }

        StreamPlayer carrier = message.getCarrier();
        if (carrier == null) {
            Optional<StreamPlayer> optional = UserUtils.getOnlinePlayers().values().stream().findFirst();
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
