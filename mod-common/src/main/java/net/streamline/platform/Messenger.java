package net.streamline.platform;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.api.SLAPI;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.interfaces.IMessenger;
import singularity.objects.CosmicTitle;
import singularity.utils.MessageUtils;

import java.util.regex.Pattern;

public class Messenger implements IMessenger {

    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-ORX]|&[0-9A-FK-ORX]|<[^>]+>");

    @Getter
    private static Messenger instance;

    public Messenger() {
        instance = this;
    }

    @Override
    public void sendMessage(CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) {
            ServerPlayer player = BasePlugin.getPlayer(to.getUuid());
            if (player != null) player.sendSystemMessage(Component.literal(codedString(message)));
        } else {
            BasePlugin.getInstance().getSlf4jLogger().info(stripColor(message));
        }
    }

    @Override
    public void sendMessage(CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        String processed = SLAPI.isReady() ? MessageUtils.replaceAllPlayerBungee(otherUUID, message) : message;
        sendMessage(to, processed);
    }

    @Override
    public void sendMessage(CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        String processed = SLAPI.isReady() ? MessageUtils.replaceAllPlayerBungee(other, message) : message;
        sendMessage(to, processed);
    }

    @Override
    public void sendMessageRaw(CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) {
            ServerPlayer player = BasePlugin.getPlayer(to.getUuid());
            if (player != null) player.sendSystemMessage(Component.literal(message));
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
        if (! (player instanceof CosmicPlayer)) return;
        ServerPlayer p = BasePlugin.getPlayer(player.getUuid());
        if (p == null) return;
        p.connection.send(new ClientboundSetTitlesAnimationPacket(
                (int) title.getFadeIn(), (int) title.getStay(), (int) title.getFadeOut()));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(codedString(title.getSub()))));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(codedString(title.getMain()))));
    }

    @Override
    public String codedString(String from) {
        return from.replace("&", "§");
    }

    @Override
    public String stripColor(String string) {
        return COLOR_PATTERN.matcher(string).replaceAll("");
    }
}
