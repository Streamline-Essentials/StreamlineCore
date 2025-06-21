package net.streamline.platform;

import host.plas.bou.commands.Sender;
import host.plas.bou.utils.ColorUtils;
import host.plas.bou.utils.SenderUtils;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
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
import singularity.objects.CosmicTitle;
import singularity.utils.MessageUtils;

import java.util.*;

public class Messenger implements IMessenger {
    @Getter
    private static Messenger instance;

    public Messenger() {
        instance = this;
    }

    @Deprecated
    public static String colorAsString(String message) {
        return colorAsStringBOU(message);
    }

    @Deprecated
    public static String colorAsStringBOU(String message) {
        return host.plas.bou.utils.MessageUtils.codedString(message); // Already new-lined.
    }

    public void sendMessage(@Nullable CommandSender to, String message) {
        if (to == null) return;
        Sender s = SenderUtils.getSender(to);
        if (! SLAPI.isReady()) {
            s.sendMessage(message);
        } else {
            s.sendMessage(replaceAllPlayerBungee(to, message));
        }
    }

    public void sendMessage(@Nullable CommandSender to, String otherUUID, String message) {
        if (to == null) return;
        Sender s = SenderUtils.getSender(to);
        if (! SLAPI.isReady()) {
            s.sendMessage(message);
        } else {
            s.sendMessage(MessageUtils.replaceAllPlayerBungee(otherUUID, message));
        }
    }

    public void sendMessage(@Nullable CommandSender to, CosmicSender other, String message) {
        if (to == null) return;
        Sender s = SenderUtils.getSender(to);
        if (! SLAPI.isReady()) {
            s.sendMessage(message);
        } else {
            s.sendMessage(MessageUtils.replaceAllPlayerBungee(other, message));
        }
    }

    @Override
    public void sendMessage(@Nullable CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(Bukkit.getPlayer(UUID.fromString(to.getUuid())), message);
        else sendMessage(Bukkit.getConsoleSender(), message);
    }
    
    @Override
    public void sendMessage(@Nullable CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessage(Bukkit.getPlayer(UUID.fromString(to.getUuid())), otherUUID, message);
        else sendMessage(Bukkit.getConsoleSender(), otherUUID, message);
    }

    @Override
    public void sendMessage(@Nullable CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        if (to instanceof CosmicPlayer) sendMessage(Bukkit.getPlayer(UUID.fromString(to.getUuid())), other, message);
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

    @Override
    public void sendMessageRaw(@Nullable CosmicSender to, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(Bukkit.getPlayer(UUID.fromString(to.getUuid())), message);
        else sendMessageRaw(Bukkit.getConsoleSender(), message);
    }

    @Override
    public void sendMessageRaw(@Nullable CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(Bukkit.getPlayer(UUID.fromString(to.getUuid())), otherUUID, message);
        else sendMessageRaw(Bukkit.getConsoleSender(), otherUUID, message);
    }

    @Override
    public void sendMessageRaw(@Nullable CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        if (to instanceof CosmicPlayer) sendMessageRaw(Bukkit.getPlayer(UUID.fromString(to.getUuid())), other, message);
        else sendMessageRaw(Bukkit.getConsoleSender(), other, message);
    }

    @Override
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
        return codedStringBOU(from);
    }

    @Override
    public String stripColor(String string){
        return ChatColor.stripColor(string).replaceAll("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", "");
    }

    public String codedStringBOU(String value) {
        return host.plas.bou.utils.MessageUtils.codedString(value); // Already new-lined.
    }
    
    public BaseComponent[] colorizeBOU(String value) {
        return ColorUtils.color(value);
    }
    
    public BaseComponent[] codedText(String from) {
        return colorizeBOU(from);
    }

    public String colorizeHard(String value) {
        return ColorUtils.colorizeHard(value);
    }

    public String replaceAllPlayerBungee(CommandSender sender, String of) {
        CosmicSender s = UserManager.getInstance().getOrCreateSender(sender);

        return MessageUtils.replaceAllPlayerBungee(s, of);
    }
}
