package net.streamline.platform;

import host.plas.bou.utils.ColorUtils;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.streamline.api.SLAPI;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import singularity.interfaces.IMessenger;
import singularity.modules.ModuleUtils;
import singularity.objects.CosmicTitle;
import singularity.text.HexPolicy;
import singularity.text.TextManager;
import singularity.utils.MessageUtils;

import java.util.*;

public class Messenger implements IMessenger {
    @Getter
    private static Messenger instance;

    public Messenger() {
        instance = this;
    }

    public static String colorAsString(String message) {
        StringBuilder builder = new StringBuilder();

        for (BaseComponent component : ColorUtils.color(message)) {
            builder.append(component.toLegacyText());
        }

        return builder.toString();
    }

    public void sendMessage(@Nullable CommandSender to, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(simplify(codedText(message)));
        } else {
            to.sendMessage(simplify(codedText(replaceAllPlayerBungee(to, message))));
        }
    }

    public void sendMessage(@Nullable CommandSender to, String otherUUID, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(simplify(codedText(message)));
        } else {
            to.sendMessage(simplify(codedText(MessageUtils.replaceAllPlayerBungee(otherUUID, message))));
        }
    }

    public void sendMessage(@Nullable CommandSender to, CosmicSender other, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(simplify(codedText(message)));
        } else {
            to.sendMessage(simplify(codedText(MessageUtils.replaceAllPlayerBungee(other, message))));
        }
    }

    public void sendMessage(@Nullable CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(Bukkit.getPlayer(to.getUuid()), message);
        else sendMessage(Bukkit.getConsoleSender(), message);
    }

    public void sendMessage(@Nullable CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(Bukkit.getPlayer(to.getUuid()), otherUUID, message);
        else sendMessage(Bukkit.getConsoleSender(), otherUUID, message);
    }

    public void sendMessage(@Nullable CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        if (to instanceof CosmicPlayer) sendMessage(Bukkit.getPlayer(to.getUuid()), other, message);
        else sendMessage(Bukkit.getConsoleSender(), other, message);
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

    public void sendMessageRaw(CommandSender to, CosmicSender other, String message) {
        if (to == null) return;

        String r = message;
        if (SLAPI.isReady()) {
            r = MessageUtils.replaceAllPlayerBungee(other, message);
        }

        to.sendMessage(r);
    }

    public void sendMessageRaw(@Nullable CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(Bukkit.getPlayer(to.getUuid()), message);
        else sendMessageRaw(Bukkit.getConsoleSender(), message);
    }

    public void sendMessageRaw(@Nullable CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(Bukkit.getPlayer(to.getUuid()), otherUUID, message);
        else sendMessageRaw(Bukkit.getConsoleSender(), otherUUID, message);
    }

    public void sendMessageRaw(@Nullable CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(Bukkit.getPlayer(to.getUuid()), other, message);
        else sendMessageRaw(Bukkit.getConsoleSender(), other, message);
    }

    public String simplify(BaseComponent... components) {
        StringBuilder builder = new StringBuilder();

        for (BaseComponent component : components) {
            builder.append(component.toLegacyText());
        }

        return builder.toString();
    }

    public void sendTitle(CosmicSender player, CosmicTitle title) {
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
        CosmicSender s = UserManager.getInstance().getOrCreateSender(sender);

        return MessageUtils.replaceAllPlayerBungee(s, of);
    }
}
