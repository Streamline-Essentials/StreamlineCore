package net.streamline.platform;

import net.kyori.adventure.key.Key;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.text.ChatComponent;
import net.streamline.api.text.SLComponent;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.streamline.api.SLAPI;
import net.streamline.api.interfaces.IMessenger;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

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
    public void sendMessage(@Nullable CommandSource to, StreamlineUser other, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(other, message)));
        }
    }

    public void sendMessage(@Nullable StreamlineUser to, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsoleCommandSource(), message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), message);
    }

    public void sendMessage(@Nullable StreamlineUser to, String otherUUID, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsoleCommandSource(), otherUUID, message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), otherUUID, message);
    }

    public void sendMessage(@Nullable StreamlineUser to, StreamlineUser other, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsoleCommandSource(), other, message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), other, message);
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

    public void sendMessageRaw(CommandSource to, StreamlineUser other, String message) {
        if (to == null) return;

        Component component;
        if (! SLAPI.isReady()) {
            component = Component.text(message);
        } else {
            component = Component.text(MessageUtils.replaceAllPlayerBungee(other, message));
        }

        to.sendMessage(component);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, String message) {
        if (to instanceof StreamlineConsole) sendMessageRaw(Streamline.getInstance().getProxy().getConsoleCommandSource(), message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), message);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, String otherUUID, String message) {
        if (to instanceof StreamlineConsole) sendMessageRaw(Streamline.getInstance().getProxy().getConsoleCommandSource(), otherUUID, message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), otherUUID, message);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, StreamlineUser other, String message) {
        if (to instanceof StreamlineConsole) sendMessageRaw(Streamline.getInstance().getProxy().getConsoleCommandSource(), other, message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), other, message);
    }

    @Override
    public void sendTitle(StreamlinePlayer player, StreamlineTitle title) {
        Player p = Streamline.getPlayer(player.getUuid());
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
        return ModuleUtils.newLined(from.replace('&', '§'));
    }

    public String stripColor(String string){
        return PlainTextComponentSerializer.plainText().serialize(LegacyComponentSerializer.legacySection().deserialize(string))
                .replaceAll("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", "")
                .replaceAll("[&][1-9a-f]", "");
    }

    public Component codedText(String from) {
        String raw = codedString(from);

        ConcurrentSkipListMap<Integer, SLComponent> components = SLComponent.extract(raw);

        int sub = raw.length();
        if (! components.isEmpty()) {
            sub = components.firstKey();
        }

        Component builder = legacyCode(raw.substring(0, sub));

        for (int start : components.keySet()) {
            SLComponent component = components.get(start);
            if (component == null) {
                continue;
            }

            if (component instanceof ChatComponent) {
                ChatComponent chatComponent = (ChatComponent) component;

                String simpleText = chatComponent.getSimpleText();
                Component chatBuilder = legacyCode(simpleText);

                if (chatComponent.isCompleteHover()) {
                    String value = chatComponent.getHoverValue();
                    ChatComponent.HoverAction action = chatComponent.getHoverAction();

                    if (action == ChatComponent.HoverAction.SHOW_TEXT) {
                        chatBuilder = chatBuilder.hoverEvent(HoverEvent.showText(legacyCode(value)));
                    } else if (action == ChatComponent.HoverAction.SHOW_ITEM) {
                        chatBuilder = chatBuilder.hoverEvent(HoverEvent.showItem(HoverEvent.ShowItem.of(Key.key(value), 1)));
                    } else if (action == ChatComponent.HoverAction.SHOW_ENTITY) {
                        chatBuilder = chatBuilder.hoverEvent(HoverEvent.showEntity(HoverEvent.ShowEntity.of(Key.key(value), UUID.randomUUID())));
                    } else {
                        chatBuilder = chatBuilder.hoverEvent(HoverEvent.showText(legacyCode(value)));
                    }
                }
                if (chatComponent.isCompleteClick()) {
                    String value = chatComponent.getClickValue();
                    ChatComponent.ClickAction action = chatComponent.getClickAction();

                    if (action == ChatComponent.ClickAction.OPEN_URL) {
                        chatBuilder = chatBuilder.clickEvent(ClickEvent.openUrl(value));
                    } else if (action == ChatComponent.ClickAction.OPEN_FILE) {
                        chatBuilder = chatBuilder.clickEvent(ClickEvent.openFile(value));
                    } else if (action == ChatComponent.ClickAction.RUN_COMMAND) {
                        chatBuilder = chatBuilder.clickEvent(ClickEvent.runCommand(value));
                    } else if (action == ChatComponent.ClickAction.SUGGEST_COMMAND) {
                        chatBuilder = chatBuilder.clickEvent(ClickEvent.suggestCommand(value));
                    } else if (action == ChatComponent.ClickAction.CHANGE_PAGE) {
                        chatBuilder = chatBuilder.clickEvent(ClickEvent.changePage(Integer.parseInt(value)));
                    } else {
                        chatBuilder = chatBuilder.clickEvent(ClickEvent.runCommand(value));
                    }
                }

                builder = builder.append(chatBuilder);
            }

            Integer next = components.higherKey(start);
            if (next == null) {
                Component baseComponent = legacyCode(raw.substring(component.realEnd())).hoverEvent(null).clickEvent(null);
                builder = builder.append(baseComponent);
            } else {
                Component baseComponent = legacyCode(raw.substring(component.realEnd(), next)).hoverEvent(null).clickEvent(null);
                builder = builder.append(baseComponent);
            }
        }

        return builder;
    }

    public String asString(Component textComponent){
        return LegacyComponentSerializer.legacySection().serialize(textComponent);
    }

    public Component legacyCode(String from) {
        return LegacyComponentSerializer.builder().extractUrls().character('&').hexColors().build().deserialize(from);
    }

    public String replaceAllPlayerBungee(CommandSource sender, String of) {
        return MessageUtils.replaceAllPlayerBungee(UserManager.getInstance().getOrGetUser(sender), of);
    }
}
