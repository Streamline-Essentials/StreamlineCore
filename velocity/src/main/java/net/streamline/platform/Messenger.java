package net.streamline.platform;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.streamline.api.SLAPI;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import net.streamline.base.StreamlineVelocity;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.Nullable;
import singularity.interfaces.IMessenger;
import singularity.modules.ModuleUtils;
import singularity.objects.CosmicTitle;
import singularity.text.HexPolicy;
import singularity.text.TextManager;
import singularity.utils.MessageUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Messenger implements IMessenger {
    @Getter
    private static Messenger instance;

    public Messenger() {
        instance = this;
    }

    public void sendMessage(@Nullable CommandSource to, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(replaceAllPlayerBungee(to, message)));
        }
    }

    public void sendMessage(@Nullable CommandSource to, String otherUUID, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(otherUUID, message)));
        }
    }
    public void sendMessage(@Nullable CommandSource to, CosmicSender other, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(other, message)));
        }
    }

    public void sendMessage(@Nullable CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(StreamlineVelocity.getPlayer(to.getUuid()), message);
        else sendMessage(StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource(), message);
    }

    public void sendMessage(@Nullable CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(StreamlineVelocity.getPlayer(to.getUuid()), otherUUID, message);
        else sendMessage(StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource(), otherUUID, message);
    }

    public void sendMessage(@Nullable CosmicSender to, CosmicSender other, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(StreamlineVelocity.getPlayer(to.getUuid()), other, message);
        else sendMessage(StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource(), other, message);
    }

    public void sendMessageRaw(CommandSource to, String message) {
        if (to == null) return;

        Component component;
        if (! SLAPI.isReady()) {
            component = Component.text(message);
        } else {
            component = Component.text(replaceAllPlayerBungee(to, message));
        }

        to.sendMessage(component);
    }

    public void sendMessageRaw(CommandSource to, String otherUUID, String message) {
        if (to == null) return;

        Component component;
        if (! SLAPI.isReady()) {
            component = Component.text(message);
        } else {
            component = Component.text(MessageUtils.replaceAllPlayerBungee(otherUUID, message));
        }

        to.sendMessage(component);
    }

    public void sendMessageRaw(CommandSource to, CosmicSender other, String message) {
        if (to == null) return;

        Component component;
        if (! SLAPI.isReady()) {
            component = Component.text(message);
        } else {
            component = Component.text(MessageUtils.replaceAllPlayerBungee(other, message));
        }

        to.sendMessage(component);
    }

    public void sendMessageRaw(@Nullable CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(StreamlineVelocity.getPlayer(to.getUuid()), message);
        else sendMessageRaw(StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource(), message);
    }

    public void sendMessageRaw(@Nullable CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(StreamlineVelocity.getPlayer(to.getUuid()), otherUUID, message);
        else sendMessageRaw(StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource(), otherUUID, message);
    }

    public void sendMessageRaw(@Nullable CosmicSender to, CosmicSender other, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(StreamlineVelocity.getPlayer(to.getUuid()), other, message);
        else sendMessageRaw(StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource(), other, message);
    }

    @Override
    public void sendTitle(CosmicSender player, CosmicTitle title) {
        Player p = StreamlineVelocity.getPlayer(player.getUuid());
        if (p == null) {
            MessageUtils.logInfo("Could not send a title to a player because player is null!");
            return;
        }

        Title t = Title.title(
                codedText(
                        MessageUtils.replaceAllPlayerBungee(player, title.getMain())
                ),
                codedText(
                        MessageUtils.replaceAllPlayerBungee(player, title.getSub())
                ), Title.Times.times(
                        Duration.of(title.getFadeIn() * 50L, ChronoUnit.MILLIS),
                        Duration.of(title.getStay() * 50L, ChronoUnit.MILLIS),
                        Duration.of(title.getFadeOut() * 50L, ChronoUnit.MILLIS)
                )
        );

        p.showTitle(t);
    }

    @Override
    public String codedString(String from) {
//        return ModuleUtils.newLined(from.replace("&", "§"));

        return ModuleUtils.newLined(from); // issues. ^^^
    }

    public String stripColor(String string){
        return PlainTextComponentSerializer.plainText().serialize(LegacyComponentSerializer.legacySection().deserialize(string))
                .replaceAll("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", "")
                .replaceAll("[&][1-9a-f]", "");
    }

    public String asString(Component textComponent){
        return LegacyComponentSerializer.legacySection().serialize(textComponent);
    }

    public static Component legacyCode(String from) {
        return LegacyComponentSerializer.builder().extractUrls().character('&').hexColors().build().deserialize(from);
    }

    public Component codedText(String from) {
        String raw = codedString(from); // Assuming codedString is another method you've implemented

        String legacy = MessageUtils.codedString(raw); // Replace this with your actual legacy converter

        List<Component> componentsList = new ArrayList<>();

        // Handle hex codes
        for (HexPolicy policy : TextManager.getHexPolicies()) {
            for (String hexCode : TextManager.extractHexCodes(legacy, policy)) {
                String original = hexCode;
                if (! hexCode.startsWith("#")) hexCode = "#" + hexCode;
                legacy = legacy.replace(policy.getResult(original),
                        LegacyComponentSerializer.legacyAmpersand().serialize(Component.text("", TextColor.fromCSSHexString(hexCode))));
            }
        }

        List<String> jsonStrings = TextManager.extractJsonStrings(legacy, "!!json:");

        int lastEnd = 0;

        for (String jsonStr : jsonStrings) {
            int index = legacy.indexOf("!!json:" + jsonStr);
            String before = legacy.substring(lastEnd, index);
            Component beforeComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(before);
            componentsList.add(beforeComponent);

            try {
                Component jsonComponent = JSONComponentSerializer.json().deserialize(jsonStr);
                componentsList.add(jsonComponent);
            } catch (Exception e) {
                // Handle exception
                e.printStackTrace();
            }

            lastEnd = index + jsonStr.length() + 7; // 7 is the length of "!!json:"
        }

        // Append any remaining text after the last JSON block
        if (lastEnd < legacy.length()) {
            Component remainingComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(legacy.substring(lastEnd));
            componentsList.add(remainingComponent);
        }

        return Component.empty().children(componentsList);
    }

    public String replaceAllPlayerBungee(CommandSource sender, String of) {
        CosmicSender s = UserManager.getInstance().getOrCreateSender(sender);

        return MessageUtils.replaceAllPlayerBungee(s, of);
    }
}
