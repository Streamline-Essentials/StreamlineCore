package net.streamline.platform.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Turns Streamline's legacy colour syntax into a {@link Component}: {@code &} and {@code §} codes,
 * plus the hex forms the Spigot side (BukkitOfUtils' {@code ColorUtils}) accepts —
 * {@code &#RRGGBB}, {@code {#RRGGBB}}, {@code <#RRGGBB>}, {@code #RRGGBB} — and Bukkit's
 * {@code §x§R§R§G§G§B§B}.
 *
 * <p>As with legacy chat, a colour resets bold, italic and the other decorations, and {@code &r}
 * returns to the base style.</p>
 */
public final class LegacyText {
    private LegacyText() {
    }

    /** Item names and lore are italic unless their style says otherwise. */
    public static final Style ITEM_NAME = Style.EMPTY.withItalic(false);
    public static final Style ITEM_LORE = Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY);

    public static MutableComponent parse(String text) {
        return parse(text, Style.EMPTY);
    }

    public static MutableComponent parse(String text, Style base) {
        MutableComponent root = Component.empty().withStyle(base);
        if (text == null || text.isEmpty()) return root;

        StringBuilder segment = new StringBuilder();
        Style style = base;
        int i = 0;
        int length = text.length();

        while (i < length) {
            char c = text.charAt(i);

            int hexEnd = hexEnd(text, i);
            if (hexEnd > 0) {
                flush(root, segment, style);
                style = base.withColor(TextColor.fromRgb(Integer.parseInt(hexDigits(text, i, hexEnd), 16)));
                i = hexEnd;
                continue;
            }

            if ((c == '&' || c == '§') && i + 1 < length) {
                char code = Character.toLowerCase(text.charAt(i + 1));

                // Bukkit's §x§R§R§G§G§B§B hex form.
                if (code == 'x' && i + 14 <= length) {
                    String rgb = bukkitHex(text, i + 2);
                    if (rgb != null) {
                        flush(root, segment, style);
                        style = base.withColor(TextColor.fromRgb(Integer.parseInt(rgb, 16)));
                        i += 14;
                        continue;
                    }
                }

                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    flush(root, segment, style);
                    style = apply(base, style, formatting);
                    i += 2;
                    continue;
                }
            }

            if (c == '\n' || text.startsWith("%newline%", i)) {
                segment.append('\n');
                i += c == '\n' ? 1 : "%newline%".length();
                continue;
            }

            segment.append(c);
            i++;
        }

        flush(root, segment, style);
        return root;
    }

    private static Style apply(Style base, Style style, ChatFormatting formatting) {
        if (formatting == ChatFormatting.RESET) return base;
        // The sixteen colours are ChatFormatting's first constants, BLACK through WHITE.
        if (formatting.ordinal() <= ChatFormatting.WHITE.ordinal()) return base.withColor(formatting);
        switch (formatting) {
            case BOLD:
                return style.withBold(true);
            case ITALIC:
                return style.withItalic(true);
            case UNDERLINE:
                return style.withUnderlined(true);
            case STRIKETHROUGH:
                return style.withStrikethrough(true);
            case OBFUSCATED:
                return style.withObfuscated(true);
            default:
                return style;
        }
    }

    private static void flush(MutableComponent root, StringBuilder segment, Style style) {
        if (segment.length() == 0) return;
        root.append(Component.literal(segment.toString()).withStyle(style));
        segment.setLength(0);
    }

    /** The end index of a hex colour starting at {@code i}, or {@code -1} if none starts there. */
    private static int hexEnd(String text, int i) {
        if (text.startsWith("&#", i) && isHex(text, i + 2)) return i + 8;
        if (text.startsWith("{#", i) && isHex(text, i + 2) && charAt(text, i + 8) == '}') return i + 9;
        if (text.startsWith("<#", i) && isHex(text, i + 2) && charAt(text, i + 8) == '>') return i + 9;
        if (charAt(text, i) == '#' && isHex(text, i + 1)) return i + 7;
        return -1;
    }

    private static String hexDigits(String text, int start, int end) {
        int hash = text.indexOf('#', start);
        return text.substring(hash + 1, hash + 7);
    }

    private static boolean isHex(String text, int from) {
        if (from + 6 > text.length()) return false;
        for (int j = from; j < from + 6; j++) {
            if (Character.digit(text.charAt(j), 16) < 0) return false;
        }
        return true;
    }

    private static String bukkitHex(String text, int from) {
        StringBuilder rgb = new StringBuilder();
        for (int j = 0; j < 6; j++) {
            int at = from + j * 2;
            char marker = text.charAt(at);
            char digit = text.charAt(at + 1);
            if ((marker != '§' && marker != '&') || Character.digit(digit, 16) < 0) return null;
            rgb.append(digit);
        }
        return rgb.toString();
    }

    private static char charAt(String text, int i) {
        return i < text.length() ? text.charAt(i) : '\0';
    }
}
