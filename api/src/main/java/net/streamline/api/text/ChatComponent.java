package net.streamline.api.text;

import lombok.Getter;

public class ChatComponent extends DataComponent {
    public interface ChatAction {}
    public enum HoverAction implements ChatComponent.ChatAction {
        SHOW_TEXT,
        SHOW_ITEM,
        SHOW_ENTITY
    }
    public enum ClickAction implements ChatComponent.ChatAction {
        OPEN_URL,
        OPEN_FILE,
        RUN_COMMAND,
        SUGGEST_COMMAND,
        CHANGE_PAGE
    }
    public enum ChatComponentType {
        TEXT,
        HOVER,
        CLICK
    }

    public static final String CHAT_TYPE_KEY = "CHAT";

    public static final String HOVER_ACTION_KEY = "HOVER_ACTION";
    public static final String HOVER_VALUE_KEY = "HOVER_VALUE";

    public static final String CLICK_ACTION_KEY = "CLICK_ACTION";
    public static final String CLICK_VALUE_KEY = "CLICK_VALUE";

    public static final String SIMPLE_TEXT_KEY = "SIMPLE";

    public ChatComponent(String raw, int substringStart, String main) {
        super(raw, substringStart, main);
    }

    public ChatComponent(String raw, String main) {
        super(raw, main);
    }

    public <A extends ChatAction> A getAction(ChatComponentType type) {
        if (type == ChatComponentType.HOVER) {
            if (! hasPartAnyCase(HOVER_ACTION_KEY)) return null;
            return (A) HoverAction.valueOf(getPartAnyCase(HOVER_ACTION_KEY).getValue());
        }
        if (type == ChatComponentType.CLICK) {
            if (! hasPartAnyCase(CLICK_ACTION_KEY)) return null;
            return (A) ClickAction.valueOf(getPartAnyCase(CLICK_ACTION_KEY).getValue());
        }

        return null;
    }

    public HoverAction getHoverAction() {
        return getAction(ChatComponentType.HOVER);
    }

    public ClickAction getClickAction() {
        return getAction(ChatComponentType.CLICK);
    }

    public boolean hasAction(ChatComponentType type) {
        return getAction(type) != null;
    }

    public boolean hasHoverAction() {
        return hasAction(ChatComponentType.HOVER);
    }

    public boolean hasClickAction() {
        return hasAction(ChatComponentType.CLICK);
    }

    public String getValue(ChatComponentType type) {
        if (type == ChatComponentType.HOVER) {
            if (! hasPartAnyCase(HOVER_VALUE_KEY)) return null;
            return getPartAnyCase(HOVER_VALUE_KEY).getValue();
        }
        if (type == ChatComponentType.CLICK) {
            if (! hasPartAnyCase(CLICK_VALUE_KEY)) return null;
            return getPartAnyCase(CLICK_VALUE_KEY).getValue();
        }

        return null;
    }

    public String getHoverValue() {
        return getValue(ChatComponentType.HOVER);
    }

    public String getClickValue() {
        return getValue(ChatComponentType.CLICK);
    }

    public boolean hasValue(ChatComponentType type) {
        return getValue(type) != null;
    }

    public boolean hasHoverValue() {
        return hasValue(ChatComponentType.HOVER);
    }

    public boolean hasClickValue() {
        return hasValue(ChatComponentType.CLICK);
    }

    public String getSimpleText() {
        if (! hasPartAnyCase(SIMPLE_TEXT_KEY)) return null;

        return getPartAnyCase(SIMPLE_TEXT_KEY).getValue();
    }

    public boolean isCompleteHover() {
        return hasAction(ChatComponentType.HOVER) && hasValue(ChatComponentType.HOVER);
    }

    public boolean isCompleteClick() {
        return hasAction(ChatComponentType.CLICK) && hasValue(ChatComponentType.CLICK);
    }

    public static ChatComponent transpose(DataComponent component) {
        if (! component.hasPartAnyCase(DataComponent.DATA_TYPE_KEY)) {
            component.addPart(DATA_TYPE_KEY, CHAT_TYPE_KEY);
        }

        return new ChatComponent(component.getRaw(), component.getSubstringStart(), component.getMain());
    }
}
