package net.streamline.platform.savables;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.platform.Messenger;
import singularity.interfaces.audiences.IPlayerInterface;
import singularity.interfaces.audiences.getters.PlayerGetter;
import singularity.interfaces.audiences.real.RealPlayer;

import java.util.UUID;

@Getter @Setter
public class PlayerInterface implements IPlayerInterface<ProxiedPlayer> {
    @Override
    public PlayerGetter<ProxiedPlayer> getPlayerGetter(UUID uuid) {
        return () -> ProxyServer.getInstance().getPlayer(uuid);
    }

    @Override
    public PlayerGetter<ProxiedPlayer> getPlayerGetter(String playerName) {
        return () -> ProxyServer.getInstance().getPlayer(playerName);
    }

    @Override
    public RealPlayer<ProxiedPlayer> getPlayer(PlayerGetter<ProxiedPlayer> playerGetter) {
        return new RealPlayer<>(playerGetter) {
            @Override
            public void chatAs(String command) {
                getPlayer().chat(command);
            }

            @Override
            public void runCommand(String command) {
                ProxyServer.getInstance().getPluginManager().dispatchCommand(getPlayer(), command);
            }

            @Override
            public void sendMessage(String message) {
                getPlayer().sendMessage(Messenger.getInstance().codedText(message));
            }

            @Override
            public void sendMessageRaw(String message) {
                getPlayer().sendMessage(new TextComponent(message));
            }

            @Override
            public boolean hasPermission(String permission) {
                return getPlayer().hasPermission(permission);
            }

            @Override
            public void addPermission(String permission) {
                LuckPermsHandler.addPermission(getPlayer().getUniqueId().toString(), permission);
            }

            @Override
            public void removePermission(String permission) {
                LuckPermsHandler.removePermission(getPlayer().getUniqueId().toString(), permission);
            }
        };
    }
}
