package net.streamline.platform;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import net.streamline.api.SLAPI;
import net.streamline.base.StreamlineBungee;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.interfaces.IMessenger;
import singularity.modules.ModuleUtils;
import singularity.objects.CosmicTitle;
import singularity.text.HexPolicy;
import singularity.text.TextManager;
import singularity.utils.MessageUtils;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Messenger implements IMessenger {
    @Getter
    private static Messenger instance;

    public Messenger() {
        instance = this;
    }

    public void sendMessage(@Nullable CommandSender to, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(replaceAllPlayerBungee(to, message)));
        }
    }

    public void sendMessage(@Nullable CommandSender to, String otherUUID, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(otherUUID, message)));
        }
    }

    public void sendMessage(@Nullable CommandSender to, CosmicSender other, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(other, message)));
        }
    }

    public void sendMessage(@Nullable CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(StreamlineBungee.getPlayer(to.getUuid()), message);
        else sendMessage(ProxyServer.getInstance().getConsole(), message);
    }

    public void sendMessage(@Nullable CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(StreamlineBungee.getPlayer(to.getUuid()), otherUUID, message);
        else sendMessage(ProxyServer.getInstance().getConsole(), otherUUID, message);
    }

    public void sendMessage(@Nullable CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        if (to instanceof CosmicPlayer) sendMessage(StreamlineBungee.getPlayer(to.getUuid()), other, message);
        else sendMessage(ProxyServer.getInstance().getConsole(), other, message);
    }

    public void sendMessageRaw(CommandSender to, String message) {
        if (to == null) return;

        BaseComponent[] component;
        if (! SLAPI.isReady()) {
            component = new ComponentBuilder(message).create();
        } else {
            component = new ComponentBuilder(replaceAllPlayerBungee(to, message)).create();
        }

        to.sendMessage(component);
    }

    public void sendMessageRaw(CommandSender to, String otherUUID, String message) {
        if (to == null) return;

        BaseComponent[] component;
        if (! SLAPI.isReady()) {
            component = new ComponentBuilder(message).create();
        } else {
            component = new ComponentBuilder(MessageUtils.replaceAllPlayerBungee(otherUUID, message)).create();
        }

        to.sendMessage(component);
    }

    public void sendMessageRaw(CommandSender to, CosmicSender other, String message) {
        if (to == null) return;

        BaseComponent[] component;
        if (! SLAPI.isReady()) {
            component = new ComponentBuilder(message).create();
        } else {
            component = new ComponentBuilder(MessageUtils.replaceAllPlayerBungee(other, message)).create();
        }

        to.sendMessage(component);
    }

    public void sendMessageRaw(@Nullable CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(StreamlineBungee.getPlayer(to.getUuid()), message);
        else sendMessageRaw(ProxyServer.getInstance().getConsole(), message);
    }

    public void sendMessageRaw(@Nullable CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(StreamlineBungee.getPlayer(to.getUuid()), otherUUID, message);
        else sendMessageRaw(ProxyServer.getInstance().getConsole(), otherUUID, message);
    }

    public void sendMessageRaw(@Nullable CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(StreamlineBungee.getPlayer(to.getUuid()), other, message);
        else sendMessageRaw(ProxyServer.getInstance().getConsole(), other, message);
    }

    public void sendTitle(CosmicSender player, CosmicTitle title) {
        ProxiedPlayer p = StreamlineBungee.getPlayer(player.getUuid());
        if (p == null) {
            MessageUtils.logInfo("Could not send a title to a player because player is null!");
            return;
        }

        p.sendTitle(StreamlineBungee.getInstance().getProxy().createTitle()
                .title(codedText(title.getMain()))
                .subTitle(codedText(title.getSub()))
                .fadeIn((int) title.getFadeIn())
                .stay((int) title.getStay())
                .fadeOut((int) title.getFadeOut())
        );
    }

    @Override
    public String codedString(String from) {
        return ChatColor.translateAlternateColorCodes('&', ModuleUtils.newLined(from));
    }

    public String stripColor(String string){
        return ChatColor.stripColor(string).replaceAll("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", "");
    }

    public BaseComponent[] codedText(String from) {
        String raw = from;

        List<BaseComponent> componentsList = new ArrayList<>();

        // Handle hex codes
        for (HexPolicy policy : TextManager.getHexPolicies()) {
            for (String hexCode : TextManager.extractHexCodes(raw, policy)) {
                String original = hexCode;
                if (! hexCode.startsWith("#")) hexCode = "#" + hexCode;
                String replacement = ChatColor.of(hexCode).toString();
                raw = raw.replace(policy.getResult(original), replacement);
            }
        }

        raw = codedString(raw);

        // Assuming codedString is another method you've implemented to replace color codes etc.
        String legacy = MessageUtils.codedString(raw);

        List<String> jsonStrings = TextManager.extractJsonStrings(legacy, "!!json:");

        int lastEnd = 0;

        for (String jsonStr : jsonStrings) {
            int index = legacy.indexOf("!!json:" + jsonStr);
            String before = legacy.substring(lastEnd, index);
            BaseComponent[] beforeComponent = TextComponent.fromLegacyText(before);
            Collections.addAll(componentsList, beforeComponent);

            try {
                BaseComponent[] jsonComponent = ComponentSerializer.parse(jsonStr);
                Collections.addAll(componentsList, jsonComponent);
            } catch (Exception e) {
                // Handle exception
                e.printStackTrace();
            }

            lastEnd = index + jsonStr.length() + 7; // 7 is the length of "!!json:"
        }

        // Append any remaining text after the last JSON block
        if (lastEnd < legacy.length()) {
            BaseComponent[] remainingComponent = TextComponent.fromLegacyText(legacy.substring(lastEnd));
            Collections.addAll(componentsList, remainingComponent);
        }

        return componentsList.toArray(new BaseComponent[0]);
    }

    public String replaceAllPlayerBungee(CommandSender sender, String of) {
        CosmicSender s = UserManager.getInstance().getOrCreateSender(sender).orElse(null);
        if (s == null) {
            return of;
        }

        return MessageUtils.replaceAllPlayerBungee(s, of);
    }
}
