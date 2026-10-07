package singularity.objects;

import singularity.Singularity;
import singularity.data.console.CosmicSender;
import singularity.utils.Links;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One chat line built from segments, each with its own colour-coded text and an optional
 * hover tooltip and click action. Platforms that can render chat components send it as
 * one; elsewhere (the console, proxies) it arrives as the segments' plain text.
 *
 * <p>Hover, click and suggestion setters apply to the most recently added segment:</p>
 * <pre>{@code
 * new ClickableMessage()
 *         .text("&c[X]").hover("&cDelete").run("/aliaseditor home remove")
 *         .text(" &6home")
 *         .send(player);
 * }</pre>
 */
public class ClickableMessage {

    public enum ClickAction {
        /** Runs the value as a command, as if the player typed it. */
        RUN_COMMAND,
        /** Puts the value in the player's chat box without sending it. */
        SUGGEST_COMMAND,
        /** Opens the value as a URL, after the client confirms. */
        OPEN_URL,
    }

    public static final class Segment {
        private final String text;
        private String hover;
        private ClickAction clickAction;
        private String clickValue;

        private Segment(String text) {
            this.text = text;
        }

        /** The text, with colour codes as {@code &} or {@code §}. */
        public String getText() {
            return text;
        }

        /** The tooltip, with colour codes, or {@code null}. */
        public String getHover() {
            return hover;
        }

        /** What clicking the segment does, or {@code null} when it does nothing. */
        public ClickAction getClickAction() {
            return clickAction;
        }

        public String getClickValue() {
            return clickValue;
        }
    }

    private final List<Segment> segments = new ArrayList<>();

    public ClickableMessage text(String text) {
        if (text != null && ! text.isEmpty()) segments.add(new Segment(text));
        return this;
    }

    public ClickableMessage hover(String hover) {
        Segment last = last();
        if (last != null && hover != null && ! hover.isEmpty()) last.hover = hover;
        return this;
    }

    public ClickableMessage hover(List<String> lines) {
        if (lines == null || lines.isEmpty()) return this;
        return hover(String.join("\n", lines));
    }

    /** Clicking runs {@code command}; a leading {@code /} is added when missing. */
    public ClickableMessage run(String command) {
        return click(ClickAction.RUN_COMMAND, command == null || command.startsWith("/") ? command : "/" + command);
    }

    /** Clicking puts {@code text} in the chat box as-is. */
    public ClickableMessage suggest(String text) {
        return click(ClickAction.SUGGEST_COMMAND, text);
    }

    public ClickableMessage url(String url) {
        return click(ClickAction.OPEN_URL, url == null ? null : url.trim());
    }

    /**
     * Adds {@code text} with every link in it made clickable, as {@link #linkified} does.
     *
     * @param text        the text, with colour codes
     * @param bareDomains whether links without {@code https://} or {@code www.} count
     * @param linkHover   the tooltip on each link, {@code %url%} replaced by its URL; {@code null} for none
     */
    public ClickableMessage textWithLinks(String text, boolean bareDomains, String linkHover) {
        if (text == null || text.isEmpty()) return this;
        List<Links.Found> links = Links.find(text, bareDomains);
        int at = 0;
        for (Links.Found link : links) {
            if (link.getStart() > at) text(activeCodes(text, at) + text.substring(at, link.getStart()));
            text(activeCodes(text, link.getStart()) + link.getText());
            url(link.getUrl());
            if (linkHover != null) hover(linkHover.replace("%url%", link.getUrl()));
            at = link.getEnd();
        }
        if (at < text.length()) text(activeCodes(text, at) + text.substring(at));
        return this;
    }

    /**
     * A message of {@code text} in which every link opens in the browser when clicked. Each
     * segment starts with the colour and formatting in effect where it begins, since segments
     * do not inherit them from the one before.
     *
     * @param text        the text, with colour codes
     * @param bareDomains whether links without {@code https://} or {@code www.} count
     * @param linkHover   the tooltip on each link, {@code %url%} replaced by its URL; {@code null} for none
     */
    public static ClickableMessage linkified(String text, boolean bareDomains, String linkHover) {
        return new ClickableMessage().textWithLinks(text, bareDomains, linkHover);
    }

    /** Links with {@code https://} or {@code www.}, with no tooltip. */
    public static ClickableMessage linkified(String text) {
        return linkified(text, false, null);
    }

    /** Legacy and hex colour codes, and the formatting codes k-o and r. */
    private static final Pattern CODE = Pattern.compile(
            "(?i)&#[0-9a-f]{6}|\\{#[0-9a-f]{6}}|<#[0-9a-f]{6}>|[&§]x(?:[&§][0-9a-f]){6}|[&§][0-9a-fk-or]|#[0-9a-f]{6}");

    /**
     * The colour code and formatting codes in effect at {@code index} of {@code text}: the last
     * colour, then every formatting code after it. A colour clears formatting, as in vanilla,
     * and {@code r} clears both.
     */
    static String activeCodes(String text, int index) {
        Matcher matcher = CODE.matcher(text.substring(0, Math.min(index, text.length())));
        String colour = "";
        StringBuilder formats = new StringBuilder();
        while (matcher.find()) {
            String code = matcher.group();
            char last = Character.toLowerCase(code.charAt(code.length() - 1));
            boolean legacy = code.length() == 2;
            if (legacy && last == 'r') {
                colour = "";
                formats.setLength(0);
            } else if (legacy && last >= 'k' && last <= 'o') {
                formats.append(code);
            } else {
                colour = code;
                formats.setLength(0);
            }
        }
        return colour + formats;
    }

    private ClickableMessage click(ClickAction action, String value) {
        Segment last = last();
        if (last != null && value != null && ! value.isEmpty()) {
            last.clickAction = action;
            last.clickValue = value;
        }
        return this;
    }

    private Segment last() {
        return segments.isEmpty() ? null : segments.get(segments.size() - 1);
    }

    public List<Segment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    /** Every segment's text joined, colour codes included. */
    public String joinedText() {
        StringBuilder builder = new StringBuilder();
        for (Segment segment : segments) builder.append(segment.text);
        return builder.toString();
    }

    public void send(CosmicSender to) {
        if (to == null || isEmpty()) return;
        Singularity.getInstance().getMessenger().sendClickable(to, this);
    }
}
