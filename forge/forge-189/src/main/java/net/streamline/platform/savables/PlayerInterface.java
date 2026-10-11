package net.streamline.platform.savables;

import singularity.interfaces.audiences.IPlayerInterface;
import singularity.interfaces.audiences.getters.PlayerGetter;
import singularity.interfaces.audiences.real.RealPlayer;

import java.util.UUID;

/**
 * Players of a 1.8.9 client, of which there are none: it never hosts a server.
 */
public class PlayerInterface implements IPlayerInterface<Object> {

    @Override
    public PlayerGetter<Object> getPlayerGetter(UUID uuid) {
        return () -> null;
    }

    @Override
    public PlayerGetter<Object> getPlayerGetter(String playerName) {
        return () -> null;
    }

    @Override
    public RealPlayer<Object> getPlayer(PlayerGetter<Object> playerGetter) {
        return new RealPlayer<Object>(playerGetter) {
            @Override
            public void chatAs(String message) {
            }

            @Override
            public void runCommand(String command) {
            }

            @Override
            public void sendMessage(String message) {
            }

            @Override
            public void sendMessageRaw(String message) {
            }

            @Override
            public boolean hasPermission(String permission) {
                return false;
            }

            @Override
            public void addPermission(String permission) {
            }

            @Override
            public void removePermission(String permission) {
            }
        };
    }
}
