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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        if (from == null || from.isEmpty()) return Component.empty();

        String processed = codedString(from);

        // First pass: convert custom hex policies / placeholders → <#rrggbb> format
        String withHex = processed;
        for (HexPolicy policy : TextManager.getHexPolicies()) {
            Pattern pattern = policy.getPattern(); // assuming HexPolicy has getPattern() → Pattern
            Matcher matcher = pattern.matcher(withHex);

            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String hex = matcher.group("hex"); // assume group named "hex" or adjust
                if (hex == null) hex = matcher.group(1);

                String adventureHex = "<#" + hex.toLowerCase() + ">";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(adventureHex));
            }
            matcher.appendTail(sb);
            withHex = sb.toString();
        }

        // Now parse with Adventure's modern legacy serializer (supports &#rrggbb and <#rrggbb>)
        LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
                .character('&')
                .hexColors()
                .extractUrls()
                .build();

        Component mainComponent = serializer.deserialize(withHex);

        // Handle !!json: parts (your original JSON logic preserved)
        List<Component> parts = new ArrayList<>();
        String text = withHex; // use the pre-processed string with <#> tags

        Pattern jsonPattern = Pattern.compile("!!json:(\\{.*?\\})");
        Matcher jsonMatcher = jsonPattern.matcher(text);

        int lastEnd = 0;

        while (jsonMatcher.find()) {
            int start = jsonMatcher.start();
            String before = text.substring(lastEnd, start);

            // Parse text before JSON with legacy serializer (now supports hex)
            if (!before.isEmpty()) {
                parts.add(serializer.deserialize(before));
            }

            String jsonContent = jsonMatcher.group(1);
            try {
                Component jsonComp = JSONComponentSerializer.json().deserialize(jsonContent);
                parts.add(jsonComp);
            } catch (Exception e) {
                e.printStackTrace();
                parts.add(Component.text("[JSON ERROR]"));
            }

            lastEnd = jsonMatcher.end();
        }

        // Remaining text after last JSON block
        if (lastEnd < text.length()) {
            String tail = text.substring(lastEnd);
            if (!tail.isEmpty()) {
                parts.add(serializer.deserialize(tail));
            }
        }

        if (parts.isEmpty()) {
            return mainComponent;
        }

        return Component.empty().children(parts);
    }

    public String replaceAllPlayerBungee(CommandSource sender, String of) {
        CosmicSender s = UserManager.getInstance().getOrCreateSender(sender).orElse(null);
        if (s == null) {
            return of; // If sender is null, return the original string
        }

        return MessageUtils.replaceAllPlayerBungee(s, of);
    }
}