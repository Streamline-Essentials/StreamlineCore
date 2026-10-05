package net.streamline.platform;

import lombok.Getter;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.api.SLAPI;
import net.streamline.platform.compat.McCompat;
import net.streamline.platform.text.LegacyText;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.interfaces.IMessenger;
import singularity.objects.ClickableMessage;
import singularity.objects.CosmicTitle;
import singularity.utils.MessageUtils;

import java.util.regex.Pattern;

public class Messenger implements IMessenger {

    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)&#[0-9A-F]{6}|\\{#[0-9A-F]{6}}|#[0-9A-F]{6}|§[0-9A-FK-ORX]|&[0-9A-FK-ORX]|<[^>]+>");

    @Getter
    private static Messenger instance;

    public Messenger() {
        instance = this;
    }

    /**
     * Resolves placeholders in {@code message} against {@code context}, the way every
     * other platform's messenger does before sending.
     */
    private static String resolve(CosmicSender context, String message) {
        return SLAPI.isReady() ? MessageUtils.replaceAllPlayerBungee(context, message) : message;
    }

    private void deliver(CosmicSender to, String message) {
        if (to instanceof CosmicPlayer) {
            ServerPlayer player = BasePlugin.getPlayer(to.getUuid());
            if (player != null) player.sendSystemMessage(LegacyText.parse(message));
        } else {
            BasePlugin.getInstance().getSlf4jLogger().info(stripColor(message));
        }
    }

    private void deliverRaw(CosmicSender to, String message) {
        if (to instanceof CosmicPlayer) {
            ServerPlayer player = BasePlugin.getPlayer(to.getUuid());
            if (player != null) player.sendSystemMessage(Component.literal(message));
        } else {
            BasePlugin.getInstance().getSlf4jLogger().info(message);
        }
    }

    @Override
    public void sendMessage(CosmicSender to, String message) {
        if (to == null) return;
        deliver(to, resolve(to, message));
    }

    @Override
    public void sendMessage(CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        deliver(to, SLAPI.isReady() ? MessageUtils.replaceAllPlayerBungee(otherUUID, message) : message);
    }

    @Override
    public void sendMessage(CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        deliver(to, resolve(other, message));
    }

    @Override
    public void sendMessageRaw(CosmicSender to, String message) {
        if (to == null) return;
        deliverRaw(to, resolve(to, message));
    }

    @Override
    public void sendMessageRaw(CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        deliverRaw(to, SLAPI.isReady() ? MessageUtils.replaceAllPlayerBungee(otherUUID, message) : message);
    }

    @Override
    public void sendMessageRaw(CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        deliverRaw(to, resolve(other, message));
    }

    @Override
    public void sendClickable(CosmicSender to, ClickableMessage message) {
        if (to == null || message == null || message.isEmpty()) return;
        ServerPlayer player = to instanceof CosmicPlayer ? BasePlugin.getPlayer(to.getUuid()) : null;
        if (player == null) {
            sendMessage(to, message.joinedText());
            return;
        }

        MutableComponent line = Component.empty();
        for (ClickableMessage.Segment segment : message.getSegments()) {
            Style style = Style.EMPTY;
            if (segment.getHover() != null) {
                style = style.withHoverEvent(McCompat.showText(LegacyText.parse(segment.getHover())));
            }
            ClickEvent click = McCompat.clickEvent(segment.getClickAction(), segment.getClickValue());
            if (click != null) style = style.withClickEvent(click);
            line.append(LegacyText.parse(segment.getText()).withStyle(style));
        }
        player.sendSystemMessage(line);
    }

    @Override
    public void sendTitle(CosmicSender player, CosmicTitle title) {
        if (! (player instanceof CosmicPlayer)) return;
        ServerPlayer p = BasePlugin.getPlayer(player.getUuid());
        if (p == null) return;
        p.connection.send(new ClientboundSetTitlesAnimationPacket(
                (int) title.getFadeIn(), (int) title.getStay(), (int) title.getFadeOut()));
        p.connection.send(new ClientboundSetSubtitleTextPacket(LegacyText.parse(resolve(player, title.getSub()))));
        p.connection.send(new ClientboundSetTitleTextPacket(LegacyText.parse(resolve(player, title.getMain()))));
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
