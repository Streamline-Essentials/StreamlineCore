package host.plas.hud;

import singularity.client.ClientInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Element text with its placeholders looked up once, when it is set: drawing a frame only
 * joins literal text and the values the placeholders read, with no searching or regexes.
 */
final class Template {

    private final String source;
    /** One entry per line; each part is a literal {@link String} or a {@link Placeholders.Value}. */
    private final List<Object[]> lines;

    private Template(String source, List<Object[]> lines) {
        this.source = source;
        this.lines = lines;
    }

    String source() {
        return source;
    }

    static Template compile(String source) {
        List<Object[]> lines = new ArrayList<>();
        List<Object> parts = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int length = source.length();
        int i = 0;
        while (i < length) {
            if (source.startsWith(Placeholders.NEWLINE, i)) {
                flush(parts, literal);
                lines.add(parts.toArray());
                parts.clear();
                i += Placeholders.NEWLINE.length();
                continue;
            }
            if (source.startsWith(Placeholders.PREFIX, i)) {
                int keyStart = i + Placeholders.PREFIX.length();
                int end = source.indexOf('%', keyStart);
                Placeholders.Value value = end > keyStart ? Placeholders.get(source.substring(keyStart, end)) : null;
                if (value != null) {
                    flush(parts, literal);
                    parts.add(value);
                    i = end + 1;
                    continue;
                }
            }
            literal.append(source.charAt(i));
            i++;
        }
        flush(parts, literal);
        lines.add(parts.toArray());
        return new Template(source, lines);
    }

    private static void flush(List<Object> parts, StringBuilder literal) {
        if (literal.length() == 0) return;
        parts.add(literal.toString());
        literal.setLength(0);
    }

    /** @return the lines for this frame, leaving out lines with nothing to show */
    List<String> render(ClientInfo info) {
        if (lines.size() == 1) {
            String line = render(lines.get(0), info);
            return line.isEmpty() ? Collections.emptyList() : Collections.singletonList(line);
        }
        List<String> out = new ArrayList<>(lines.size());
        for (Object[] parts : lines) {
            String line = render(parts, info);
            if (! line.isEmpty()) out.add(line);
        }
        return out;
    }

    private static String render(Object[] parts, ClientInfo info) {
        if (parts.length == 1 && parts[0] instanceof String) return (String) parts[0];
        StringBuilder out = new StringBuilder(64);
        for (Object part : parts) {
            if (part instanceof String) {
                out.append((String) part);
            } else {
                String value = ((Placeholders.Value) part).get(info);
                if (value != null) out.append(value);
            }
        }
        return out.toString();
    }
}
