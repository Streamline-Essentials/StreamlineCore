package net.streamline.platform;

import lombok.Getter;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.streamline.api.SLAPI;
import net.streamline.base.StreamlineFabric;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.interfaces.IMessenger;
import singularity.objects.CosmicTitle;
import singularity.utils.MessageUtils;

import java.util.UUID;
import java.util.regex.Pattern;

public class Messenger implements IMessenger {

    @Getter
    private static Messenger instance;

    public Messenger() {
        instance = this;
    }

    @Override
    public void sendMessage(CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) {
            CosmicPlayer cp = (CosmicPlayer) to;
            ServerPlayerEntity player = getServerPlayer(cp.getUuid());
            if (player != null) player.sendMessage(new LiteralText(codedString(message)), false);
        } else {
            if (StreamlineFabric.getInstance().getServer() != null) {
                StreamlineFabric.getInstance().getSlf4jLogger().info(stripColor(message));
            }
        }
    }

    @Override
    public void sendMessage(CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        String processed = SLAPI.isReady() ? MessageUtils.replaceAllPlayerBungee(otherUUID, message) : message;
        sendMessageRaw(to, processed);
    }

    @Override
    public void sendMessage(CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        String processed = SLAPI.isReady() ? MessageUtils.replaceAllPlayerBungee(other, message) : message;
        sendMessageRaw(to, processed);
    }

    @Override
    public void sendMessageRaw(CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) {
            CosmicPlayer cp = (CosmicPlayer) to;
            ServerPlayerEntity player = getServerPlayer(cp.getUuid());
            if (player != null) player.sendMessage(new LiteralText(message), false);
        } else {
            if (StreamlineFabric.getInstance().getServer() != null) {
                StreamlineFabric.getInstance().getSlf4jLogger().info(message);
            }
        }
    }

    @Override
    public void sendMessageRaw(CosmicSender to, String otherUUID, String message) {
        sendMessageRaw(to, message);
    }

    @Override
    public void sendMessageRaw(CosmicSender to, CosmicSender other, String message) {
        sendMessageRaw(to, message);
    }

    @Override
    public void sendTitle(CosmicSender player, CosmicTitle title) {
        // Title packets for 1.16.5 Yarn — stub
        if (!(player instanceof CosmicPlayer)) return;
        CosmicPlayer cp = (CosmicPlayer) player;
        ServerPlayerEntity p = getServerPlayer(cp.getUuid());
        if (p == null) return;
        // Title sending via network packets is complex in 1.16.5; leaving as stub
        MessageUtils.logWarning("sendTitle not fully implemented for Fabric 1.16.5.");
    }

    @Override
    public String codedString(String from) {
        return from.replace("&", "\u00A7");
    }

    @Override
    public String stripColor(String string) {
        return Pattern.compile("(?i)\u00A7[0-9A-FK-ORX]|&[0-9A-FK-ORX]|<[^>]+>").matcher(string).replaceAll("");
    }

    private ServerPlayerEntity getServerPlayer(String uuid) {
        if (StreamlineFabric.getInstance().getServer() == null) return null;
        try {
            return StreamlineFabric.getInstance().getServer()
                    .getPlayerManager().getPlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
