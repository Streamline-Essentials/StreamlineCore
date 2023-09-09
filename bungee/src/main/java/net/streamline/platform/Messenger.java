package net.streamline.platform;

import net.md_5.bungee.chat.ComponentSerializer;
import net.streamline.api.modules.ModuleUtils;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.SLAPI;
import net.streamline.api.interfaces.IMessenger;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.text.HexPolicy;
import net.streamline.api.text.TextManager;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import org.jetbrains.annotations.Nullable;
import tv.quaint.thebase.lib.re2j.Matcher;
import tv.quaint.thebase.lib.re2j.Pattern;

import java.util.*;

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
    public void sendMessage(@Nullable CommandSender to, StreamlineUser other, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(other, message)));
        }
    }

    public void sendMessage(@Nullable StreamlineUser to, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsole(), message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), message);
    }

    public void sendMessage(@Nullable StreamlineUser to, String otherUUID, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsole(), otherUUID, message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), otherUUID, message);
    }

    public void sendMessage(@Nullable StreamlineUser to, StreamlineUser other, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsole(), other, message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), other, message);
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

    public void sendMessageRaw(CommandSender to, StreamlineUser other, String message) {
        if (to == null) return;

        BaseComponent[] component;
        if (! SLAPI.isReady()) {
            component = new ComponentBuilder(message).create();
        } else {
            component = new ComponentBuilder(MessageUtils.replaceAllPlayerBungee(other, message)).create();
        }

        to.sendMessage(component);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, String message) {
        if (to instanceof StreamlineConsole) sendMessageRaw(Streamline.getInstance().getProxy().getConsole(), message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), message);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, String otherUUID, String message) {
        if (to instanceof StreamlineConsole) sendMessageRaw(Streamline.getInstance().getProxy().getConsole(), otherUUID, message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), otherUUID, message);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, StreamlineUser other, String message) {
        if (to instanceof StreamlineConsole) sendMessageRaw(Streamline.getInstance().getProxy().getConsole(), other, message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), other, message);
    }

    public void sendTitle(StreamlinePlayer player, StreamlineTitle title) {
        ProxiedPlayer p = Streamline.getPlayer(player.getUuid());
        if (p == null) {
            MessageUtils.logInfo("Could not send a title to a player because player is null!");
            return;
        }

        p.sendTitle(Streamline.getInstance().getProxy().createTitle()
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
        String raw = codedString(from);

        // Assuming codedString is another method you've implemented to replace color codes etc.
        String legacy = MessageUtils.codedString(raw);

        List<BaseComponent> componentsList = new ArrayList<>();

        // Handle hex codes
        for (HexPolicy policy : TextManager.getHexPolicies()) {
            for (String hexCode : TextManager.extractHexCodes(legacy, policy)) {
                String original = hexCode;
                if (! hexCode.startsWith("#")) hexCode = "#" + hexCode;
                String replacement = ChatColor.of(hexCode).toString();
                legacy = legacy.replace(policy.getResult(original), replacement);
            }
        }

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
        return MessageUtils.replaceAllPlayerBungee(UserUtils.getOrGetUserByName(sender.getName()), of);
    }
}
