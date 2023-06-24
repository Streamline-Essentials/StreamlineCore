package net.streamline.platform;

import net.md_5.bungee.api.chat.hover.content.Entity;
import net.md_5.bungee.api.chat.hover.content.Item;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.text.ChatComponent;
import net.streamline.api.text.SLComponent;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.SLAPI;
import net.streamline.api.interfaces.IMessenger;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

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

        ConcurrentSkipListMap<Integer, SLComponent> components = SLComponent.extract(raw);

        int sub = raw.length();
        if (! components.isEmpty()) {
            sub = components.firstKey();
        }

        ComponentBuilder builder = new ComponentBuilder(raw.substring(0, sub));

        for (int start : components.keySet()) {
            SLComponent component = components.get(start);
            if (component == null) {
                continue;
            }

            if (component instanceof ChatComponent) {
                ChatComponent chatComponent = (ChatComponent) component;

                String simpleText = chatComponent.getSimpleText();
                ComponentBuilder chatBuilder = new ComponentBuilder(simpleText);

                if (chatComponent.isCompleteHover()) {
                    String value = chatComponent.getHoverValue();
                    ChatComponent.HoverAction action = chatComponent.getHoverAction();

                    if (action == ChatComponent.HoverAction.SHOW_TEXT) {
                        chatBuilder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new Text(value)));
                    } else if (action == ChatComponent.HoverAction.SHOW_ITEM) {
                        chatBuilder.event(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                                new Item(value, 1, null)));
                    } else if (action == ChatComponent.HoverAction.SHOW_ENTITY) {
                        chatBuilder.event(new HoverEvent(HoverEvent.Action.SHOW_ENTITY,
                                new Entity(value, UUID.randomUUID().toString(), new ComponentBuilder().create()[0])));
                    } else {
                        chatBuilder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new Text(value)));
                    }
                }
                if (chatComponent.isCompleteClick()) {
                    String value = chatComponent.getClickValue();
                    ChatComponent.ClickAction action = chatComponent.getClickAction();

                    if (action == ChatComponent.ClickAction.OPEN_URL) {
                        chatBuilder.event(new ClickEvent(ClickEvent.Action.OPEN_URL, value));
                    } else if (action == ChatComponent.ClickAction.OPEN_FILE) {
                        chatBuilder.event(new ClickEvent(ClickEvent.Action.OPEN_FILE, value));
                    } else if (action == ChatComponent.ClickAction.RUN_COMMAND) {
                        chatBuilder.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, value));
                    } else if (action == ChatComponent.ClickAction.SUGGEST_COMMAND) {
                        chatBuilder.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, value));
                    } else if (action == ChatComponent.ClickAction.CHANGE_PAGE) {
                        chatBuilder.event(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, value));
                    } else {
                        chatBuilder.event(new ClickEvent(ClickEvent.Action.OPEN_URL, value));
                    }
                }

                builder.append(chatBuilder.create());
            }

            Integer next = components.higherKey(start);
            if (next == null) {
                BaseComponent[] baseComponent = new ComponentBuilder(raw.substring(component.realEnd()))
                        .event((HoverEvent) null).event((ClickEvent) null).create();
                builder.append(baseComponent);
            } else {
                BaseComponent[] baseComponent = new ComponentBuilder(raw.substring(component.realEnd(), next))
                        .event((HoverEvent) null).event((ClickEvent) null).create();
                builder.append(baseComponent);
            }
        }

        return builder.create();
    }

    public String replaceAllPlayerBungee(CommandSender sender, String of) {
        return MessageUtils.replaceAllPlayerBungee(UserUtils.getOrGetUserByName(sender.getName()), of);
    }
}
