package net.streamline.platform;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.streamline.api.SLAPI;
import net.streamline.base.StreamlineNeoForge;
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
        if (to instanceof CosmicPlayer cp) {
            ServerPlayer player = getServerPlayer(cp.getUuid());
            if (player != null) player.sendSystemMessage(Component.literal(codedString(message)));
        } else {
            StreamlineNeoForge.getInstance().getSlf4jLogger().info(stripColor(message));
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
        if (to instanceof CosmicPlayer cp) {
            ServerPlayer player = getServerPlayer(cp.getUuid());
            if (player != null) player.sendSystemMessage(Component.literal(message));
        } else {
            StreamlineNeoForge.getInstance().getSlf4jLogger().info(message);
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
        if (!(player instanceof CosmicPlayer cp)) return;
        ServerPlayer p = getServerPlayer(cp.getUuid());
        if (p == null) return;
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                Component.literal(title.getMain())));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                Component.literal(title.getSub())));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(
                (int) title.getFadeIn(), (int) title.getStay(), (int) title.getFadeOut()));
    }

    @Override
    public String codedString(String from) {
        return from.replace("&", "\u00A7");
    }

    @Override
    public String stripColor(String string) {
        return Pattern.compile("(?i)\u00A7[0-9A-FK-ORX]|&[0-9A-FK-ORX]|<[^>]+>").matcher(string).replaceAll("");
    }

    private ServerPlayer getServerPlayer(String uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        try {
            return server.getPlayerList().getPlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
