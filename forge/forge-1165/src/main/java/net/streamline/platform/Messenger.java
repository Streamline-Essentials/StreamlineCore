package net.streamline.platform;

import lombok.Getter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.streamline.api.SLAPI;
import net.streamline.platform.BasePlugin;
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
            if (player != null) player.sendMessage(new StringTextComponent(codedString(message)), player.getUUID());
        } else {
            BasePlugin.getInstance().getSlf4jLogger().info(stripColor(message));
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
            if (player != null) player.sendMessage(new StringTextComponent(message), player.getUUID());
        } else {
            BasePlugin.getInstance().getSlf4jLogger().info(message);
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
        if (!(player instanceof CosmicPlayer)) return;
        CosmicPlayer cp = (CosmicPlayer) player;
        ServerPlayerEntity p = getServerPlayer(cp.getUuid());
        if (p == null) return;
        p.connection.send(new STitlePacket(STitlePacket.Type.TIMES,
                null, (int) title.getFadeIn(), (int) title.getStay(), (int) title.getFadeOut()));
        p.connection.send(new STitlePacket(STitlePacket.Type.SUBTITLE,
                new StringTextComponent(title.getSub())));
        p.connection.send(new STitlePacket(STitlePacket.Type.TITLE,
                new StringTextComponent(title.getMain())));
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
        if (ServerLifecycleHooks.getCurrentServer() == null) return null;
        try {
            return ServerLifecycleHooks.getCurrentServer()
                    .getPlayerList().getPlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
