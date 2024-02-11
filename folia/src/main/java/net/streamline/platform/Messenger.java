package net.streamline.platform;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.streamline.api.SLAPI;
import net.streamline.api.interfaces.IMessenger;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.text.HexPolicy;
import net.streamline.api.text.TextManager;
import net.streamline.api.utils.MessageUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
            try {
                to.sendMessage(codedText(message));
            } catch (NoSuchMethodError e) {
                to.sendMessage(codedString(message));
            }
        } else {
            try {
                to.sendMessage(codedText(replaceAllPlayerBungee(to, message)));
            } catch (NoSuchMethodError e) {
                to.sendMessage(codedString(replaceAllPlayerBungee(to, message)));
            }
        }
    }

    public void sendMessage(@Nullable CommandSender to, String otherUUID, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            try {
                to.sendMessage(codedText(message));
            } catch (NoSuchMethodError e) {
                to.sendMessage(codedString(message));
            }
        } else {
            try {
                to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(otherUUID, message)));
            } catch (NoSuchMethodError e) {
                to.sendMessage(codedString(MessageUtils.replaceAllPlayerBungee(otherUUID, message)));
            }
        }
    }
    public void sendMessage(@Nullable CommandSender to, StreamlineUser other, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            try {
                to.sendMessage(codedText(message));
            } catch (NoSuchMethodError e) {
                to.sendMessage(codedString(message));
            }
        } else {
            try {
                to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(other, message)));
            } catch (NoSuchMethodError e) {
                to.sendMessage(codedString(MessageUtils.replaceAllPlayerBungee(other, message)));
            }
        }
    }

    public void sendMessage(@Nullable StreamlineUser to, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsoleSender(), message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), message);
    }

    public void sendMessage(@Nullable StreamlineUser to, String otherUUID, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsoleSender(), otherUUID, message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), otherUUID, message);
    }

    public void sendMessage(@Nullable StreamlineUser to, StreamlineUser other, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsoleSender(), other, message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), other, message);
    }

    public void sendMessageRaw(CommandSender to, String message) {
        if (to == null) return;

        String r = message;
        if (SLAPI.isReady()) {
            r = replaceAllPlayerBungee(to, message);
        }

        to.sendMessage(r);
    }

    public void sendMessageRaw(CommandSender to, String otherUUID, String message) {
        if (to == null) return;

        String r = message;
        if (SLAPI.isReady()) {
            r = MessageUtils.replaceAllPlayerBungee(otherUUID, message);
        }

        to.sendMessage(r);
    }

    public void sendMessageRaw(CommandSender to, StreamlineUser other, String message) {
        if (to == null) return;

        String r = message;
        if (SLAPI.isReady()) {
            r = MessageUtils.replaceAllPlayerBungee(other, message);
        }

        to.sendMessage(r);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Bukkit.getConsoleSender(), message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), message);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, String otherUUID, String message) {
        if (to instanceof StreamlineConsole) sendMessageRaw(Bukkit.getConsoleSender(), otherUUID, message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), otherUUID, message);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, StreamlineUser other, String message) {
        if (to instanceof StreamlineConsole) sendMessageRaw(Bukkit.getConsoleSender(), other, message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), other, message);
    }

    public void sendTitle(StreamlinePlayer player, StreamlineTitle title) {
        Player p = Streamline.getPlayer(player.getUuid());
        if (p == null) {
            MessageUtils.logInfo("Could not send a title to a player because player is null!");
            return;
        }


        p.sendTitle(title.getMain(),
                title.getSub(),
                (int) title.getFadeIn(),
                (int) title.getStay(),
                (int) title.getFadeOut()
        );
    }

    @Override
    public String codedString(String from) {
        return ChatColor.translateAlternateColorCodes('&', ModuleUtils.newLined(from));
    }

    public String stripColor(String string){
        return ChatColor.stripColor(string).replaceAll("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", "");
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

    public String replaceAllPlayerBungee(CommandSender sender, String of) {
        return MessageUtils.replaceAllPlayerBungee(UserManager.getInstance().getOrGetUser(sender), of);
    }
}
