package singularity.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleLike;
import singularity.messages.answered.ReturnableMessage;
import singularity.messages.builders.ProxyParseMessageBuilder;
import singularity.modules.ModuleUtils;
import singularity.objects.SingleSet;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Static utility class providing console logging, text manipulation, and message
 * dispatch helpers used throughout the Singularity framework.
 *
 * <p>All {@code log*} methods respect the corresponding enable/disable flag in the
 * main config so that individual log levels can be suppressed at runtime.  The
 * {@code %newline%} token in any message string is expanded to a real newline
 * before output.
 */
public class MessageUtils {

    /**
     * Reserved initialisation hook, called during framework startup.
     * Currently a no-op but present for future use.
     */
    public static void init() {
    }

    /**
     * Sends an informational message to the console, respecting the configured
     * info-level prefix and the disable flag.
     *
     * @param message the message to log; {@code %newline%} is expanded to {@code \n}
     */
    public static void logInfo(String message) {
        if (GivenConfigs.getMainConfig().debugConsoleInfoDisabled()) return;
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            Singularity.sendConsoleMessage(GivenConfigs.getMainConfig().debugConsoleInfoPrefix() + line);
        }
    }

    /**
     * Sends a warning message to the console, respecting the configured
     * warning-level prefix and the disable flag.
     *
     * @param message the message to log; {@code %newline%} is expanded to {@code \n}
     */
    public static void logWarning(String message) {
        if (GivenConfigs.getMainConfig().debugConsoleWarningsDisabled()) return;
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            Singularity.sendConsoleMessage(GivenConfigs.getMainConfig().debugConsoleWarningsPrefix() + line);
        }
    }

    /**
     * Sends an error-level message to the console, respecting the configured
     * error prefix and the disable flag.
     *
     * @param message the message to log; {@code %newline%} is expanded to {@code \n}
     */
    public static void logSevere(String message) {
        if (GivenConfigs.getMainConfig().debugConsoleErrorsDisabled()) return;
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            Singularity.sendConsoleMessage(GivenConfigs.getMainConfig().debugConsoleErrorsPrefix() + line);
        }
    }

    /**
     * Sends a debug message to the console, respecting the configured
     * debug-level prefix and the disable flag.
     *
     * @param message the message to log; {@code %newline%} is expanded to {@code \n}
     */
    public static void logDebug(String message) {
        if (GivenConfigs.getMainConfig().debugConsoleDebugDisabled()) return;
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            Singularity.sendConsoleMessage(GivenConfigs.getMainConfig().debugConsoleDebugPrefix() + line);
        }
    }

    /**
     * Logs each element of a stack trace at the INFO level.
     *
     * @param stackTraceElements the stack trace to log
     */
    public static void logInfo(StackTraceElement[] stackTraceElements) {
        Arrays.stream(stackTraceElements).forEach(stackTraceElement -> {
            logInfo(stackTraceElement.toString());
        });
    }

    /**
     * Logs each element of a stack trace at the WARNING level.
     *
     * @param stackTraceElements the stack trace to log
     */
    public static void logWarning(StackTraceElement[] stackTraceElements) {
        Arrays.stream(stackTraceElements).forEach(stackTraceElement -> {
            logWarning(stackTraceElement.toString());
        });
    }

    /**
     * Logs each element of a stack trace at the SEVERE/ERROR level.
     *
     * @param stackTraceElements the stack trace to log
     */
    public static void logSevere(StackTraceElement[] stackTraceElements) {
        Arrays.stream(stackTraceElements).forEach(stackTraceElement -> {
            logSevere(stackTraceElement.toString());
        });
    }

    /**
     * Logs each element of a stack trace at the DEBUG level.
     *
     * @param stackTraceElements the stack trace to log
     */
    public static void logDebug(StackTraceElement[] stackTraceElements) {
        Arrays.stream(stackTraceElements).forEach(stackTraceElement -> {
            logDebug(stackTraceElement.toString());
        });
    }

    /**
     * Logs an exception's message and full stack trace at the INFO level.
     *
     * @param e the exception to log
     */
    public static void logInfo(Exception e) {
        logInfo(e.getMessage());
        logInfo(e.getStackTrace());
    }

    /**
     * Logs an exception's message and full stack trace at the WARNING level.
     *
     * @param e the exception to log
     */
    public static void logWarning(Exception e) {
        logWarning(e.getMessage());
        logWarning(e.getStackTrace());
    }

    /**
     * Logs an exception's message and full stack trace at the SEVERE/ERROR level.
     *
     * @param e the exception to log
     */
    public static void logSevere(Exception e) {
        logSevere(e.getMessage());
        logSevere(e.getStackTrace());
    }

    /**
     * Logs an exception's message and full stack trace at the DEBUG level.
     *
     * @param e the exception to log
     */
    public static void logDebug(Exception e) {
        logDebug(e.getMessage());
        logDebug(e.getStackTrace());
    }

    /**
     * Logs a message together with a throwable's message and stack trace at INFO level.
     *
     * @param message   the contextual message to prepend
     * @param throwable the throwable whose details are appended
     */
    public static void logInfo(String message, Throwable throwable) {
        logInfo(message);
        logInfo(throwable.getMessage());
        logInfo(throwable.getStackTrace());
    }

    /**
     * Logs a message together with a throwable's message and stack trace at WARNING level.
     *
     * @param message   the contextual message to prepend
     * @param throwable the throwable whose details are appended
     */
    public static void logWarning(String message, Throwable throwable) {
        logWarning(message);
        logWarning(throwable.getMessage());
        logWarning(throwable.getStackTrace());
    }

    /**
     * Logs a message together with a throwable's message and stack trace at SEVERE/ERROR level.
     *
     * @param message   the contextual message to prepend
     * @param throwable the throwable whose details are appended
     */
    public static void logSevere(String message, Throwable throwable) {
        logSevere(message);
        logSevere(throwable.getMessage());
        logSevere(throwable.getStackTrace());
    }

    /**
     * Logs a message together with a throwable's message and stack trace at DEBUG level.
     *
     * @param message   the contextual message to prepend
     * @param throwable the throwable whose details are appended
     */
    public static void logDebug(String message, Throwable throwable) {
        logDebug(message);
        logDebug(throwable.getMessage());
        logDebug(throwable.getStackTrace());
    }

    /**
     * Convenience overload that appends the throwable's message to {@code message}
     * (adding a space if needed) before delegating to {@link #logInfo(String, Throwable)}.
     *
     * @param message   the leading context message
     * @param throwable the throwable whose message is appended and whose stack is logged
     */
    public static void logInfoWithInfo(String message, Throwable throwable) {
        logInfo(message + (message.endsWith(" ") ? "" : " ") + throwable.getMessage(), throwable);
    }

    /**
     * Convenience overload that appends the throwable's message to {@code message}
     * before delegating to {@link #logWarning(String, Throwable)}.
     *
     * @param message   the leading context message
     * @param throwable the throwable whose message is appended and whose stack is logged
     */
    public static void logWarningWithInfo(String message, Throwable throwable) {
        logWarning(message + (message.endsWith(" ") ? "" : " ") + throwable.getMessage(), throwable);
    }

    /**
     * Convenience overload that appends the throwable's message to {@code message}
     * before delegating to {@link #logSevere(String, Throwable)}.
     *
     * @param message   the leading context message
     * @param throwable the throwable whose message is appended and whose stack is logged
     */
    public static void logSevereWithInfo(String message, Throwable throwable) {
        logSevere(message + (message.endsWith(" ") ? "" : " ") + throwable.getMessage(), throwable);
    }

    /**
     * Convenience overload that appends the throwable's message to {@code message}
     * before delegating to {@link #logDebug(String, Throwable)}.
     *
     * @param message   the leading context message
     * @param throwable the throwable whose message is appended and whose stack is logged
     */
    public static void logDebugWithInfo(String message, Throwable throwable) {
        logDebug(message + (message.endsWith(" ") ? "" : " ") + throwable.getMessage(), throwable);
    }

    /**
     * Returns the log prefix string for a module, formatted as {@code "[<identifier>] "}.
     *
     * @param module the module whose identifier is used as the prefix
     * @return the formatted prefix string
     */
    public static String loggedModulePrefix(ModuleLike module) {
        return "[" + module.getIdentifier() + "] ";
    }

    /**
     * Logs a message at INFO level, prefixing it with the module's identifier.
     *
     * @param module  the owning module
     * @param message the message to log; {@code %newline%} is expanded to {@code \n}
     */
    public static void logInfo(ModuleLike module, String message) {
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            logInfo(loggedModulePrefix(module) + line);
        }
    }

    /**
     * Logs a message at WARNING level, prefixing it with the module's identifier.
     *
     * @param module  the owning module
     * @param message the message to log; {@code %newline%} is expanded to {@code \n}
     */
    public static void logWarning(ModuleLike module, String message) {
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            logWarning(loggedModulePrefix(module) + line);
        }
    }

    /**
     * Logs a message at SEVERE/ERROR level, prefixing it with the module's identifier.
     *
     * @param module  the owning module
     * @param message the message to log; {@code %newline%} is expanded to {@code \n}
     */
    public static void logSevere(ModuleLike module, String message) {
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            logSevere(loggedModulePrefix(module) + line);
        }
    }

    /**
     * Logs a message at DEBUG level, prefixing it with the module's identifier.
     *
     * @param module  the owning module
     * @param message the message to log; {@code %newline%} is expanded to {@code \n}
     */
    public static void logDebug(ModuleLike module, String message) {
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            logDebug(loggedModulePrefix(module) + line);
        }
    }

    /**
     * Logs each stack-trace element at INFO level, prefixing each line with the
     * module's identifier.
     *
     * @param module   the owning module
     * @param elements the stack trace to log
     */
    public static void logInfo(ModuleLike module, StackTraceElement[] elements) {
        Arrays.stream(elements).forEach(stackTraceElement -> {
            logInfo(loggedModulePrefix(module) + stackTraceElement);
        });
    }

    /**
     * Logs each stack-trace element at WARNING level, prefixing each line with the
     * module's identifier.
     *
     * @param module   the owning module
     * @param elements the stack trace to log
     */
    public static void logWarning(ModuleLike module, StackTraceElement[] elements) {
        Arrays.stream(elements).forEach(stackTraceElement -> {
            logWarning(loggedModulePrefix(module) + stackTraceElement);
        });
    }

    /**
     * Logs each stack-trace element at SEVERE/ERROR level, prefixing each line with
     * the module's identifier.
     *
     * @param module   the owning module
     * @param elements the stack trace to log
     */
    public static void logSevere(ModuleLike module, StackTraceElement[] elements) {
        Arrays.stream(elements).forEach(stackTraceElement -> {
            logSevere(loggedModulePrefix(module) + stackTraceElement);
        });
    }

    /**
     * Logs each stack-trace element at DEBUG level, prefixing each line with the
     * module's identifier.
     *
     * @param module   the owning module
     * @param elements the stack trace to log
     */
    public static void logDebug(ModuleLike module, StackTraceElement[] elements) {
        Arrays.stream(elements).forEach(stackTraceElement -> {
            logDebug(loggedModulePrefix(module) + stackTraceElement);
        });
    }

    /**
     * Sends a formatted message to the sender identified by {@code to}.
     * Logs a warning and returns early if the sender cannot be resolved.
     *
     * @param to      the UUID (or console identifier) of the recipient
     * @param message the message to send; supports Streamline colour codes
     */
    public static void sendMessage(String to, String message) {
        CosmicSender user = UserUtils.getOrCreateSender(to).orElse(null);
        if (user == null) {
            logWarning("Tried to send message to " + to + " but they are null.");
            return;
        }

        Singularity.getInstance().getMessenger().sendMessage(user, message);
    }

    /**
     * Sends a message to the sender identified by {@code to}, after applying
     * placeholder replacements using the context of the sender identified by
     * {@code otherUUID}.
     *
     * @param to        the UUID of the recipient; may be {@code null}
     * @param otherUUID the UUID of the player whose context is used for placeholder replacement
     * @param message   the message string (placeholders resolved against {@code otherUUID})
     */
    public static void sendMessage(@Nullable String to, String otherUUID, String message) {
        CosmicSender user = UserUtils.getOrCreateSender(to).orElse(null);
        if (user == null) {
            logWarning("Tried to send message to " + to + " but they are null.");
            return;
        }

        Singularity.getInstance().getMessenger().sendMessage(user, replaceAllPlayerBungee(otherUUID, message));
    }

    /**
     * Resolves all placeholders in {@code of} against the given sender's context,
     * using a short-lived Caffeine cache to avoid redundant calls within the same
     * second.
     *
     * @param user the sender whose data and permissions are used during replacement
     * @param of   the string containing placeholders to resolve
     * @return the string with all placeholders replaced; returns {@code of} unchanged
     *         if {@code user} is {@code null}
     */
    public static String replaceAllPlayerBungee(CosmicSender user, String of) {
        if (user == null) return of;

//        return SLAPI.getRatAPI().parseAllPlaceholders(user, of).completeOnTimeout(of, 77, TimeUnit.MILLISECONDS).join();

        SingleSet<String, String> key = new SingleSet<>(user.getUuid(), of);
        String cached = CACHE.getIfPresent(key);

        if (cached != null) return cached;
        else {
            String parsed = ModuleUtils.replacePlaceholders(user, of);
            CACHE.put(key, parsed);

            return parsed;
        }
    }

    /**
     * Resolves all placeholders in {@code of} using the sender identified by {@code to}.
     * Returns {@code of} unchanged (after logging a warning) if the sender cannot be found.
     *
     * @param to the UUID of the sender whose context is used for replacement
     * @param of the string containing placeholders to resolve
     * @return the string with all placeholders replaced
     */
    public static String replaceAllPlayerBungee(String to, String of) {
        CosmicSender user = UserUtils.getOrCreateSender(to).orElse(null);
        if (user == null) {
            logWarning("Tried to replace placeholders for " + to + " but they are null.");
            return of;
        }

        return replaceAllPlayerBungee(user, of);
    }

    /**
     * Short-lived cache used by {@link #replaceAllPlayerBungee(CosmicSender, String)}
     * to avoid redundant placeholder lookups.  Entries expire 1 second after writing.
     */
    public static Cache<SingleSet<String, String>, String> CACHE = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(1))
            .build();

    /**
     * Sends a placeholder-parse request to the proxy server and returns the result.
     *
     * <p>Because proxy-side parsing is asynchronous, the resolved value is stored in
     * the sender's replacement cache.  On the first call the method returns
     * {@code "Loading..."} and subsequent calls (after the proxy responds) will return
     * the fully parsed string.
     *
     * @param cosmicSender the sender providing the placeholder context
     * @param toParse      the string containing placeholders to resolve on the proxy
     * @return the cached resolved value, or {@code "Loading..."} if the result is
     *         not yet available
     */
    public static String parseOnProxy(CosmicSender cosmicSender, String toParse) {
        CosmicPlayer player;
        if (cosmicSender instanceof CosmicPlayer) {
            player = (CosmicPlayer) cosmicSender;
        } else {
            try {
                player = UserUtils.getLoadedPlayersSet().first();
            } catch (Exception e) {
                player = null;
            }
        }
        if (player == null) {
            cosmicSender.getReplacements().addReplacement(toParse, "&cNo Valid Proxy-able Player");
        }

        ReturnableMessage message = ProxyParseMessageBuilder.build(player, toParse, cosmicSender);

        message.registerEventCall(m -> {
            String parsed = ProxyParseMessageBuilder.parse(m);

            cosmicSender.getReplacements().addReplacement(toParse, parsed);
        });

        message.send();

        return cosmicSender.getReplacements().getReplacement(toParse, "Loading...");
    }

    /**
     * Formats a list of objects as a single string using the configured list-base and
     * list-last message templates from {@link singularity.configs.given.MainMessagesHandler}.
     *
     * @param list the list of items to format; each item is converted via {@link String#valueOf(Object)}
     * @return the items joined into a formatted string
     */
    public static String getListAsFormattedString(List<?> list) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < list.size(); i ++) {
            String tag = String.valueOf(list.get(i));

            if (i < list.size() - 1) {
                builder.append(MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.LISTS_BASE.get().replace("%value%", tag));
            } else {
                builder.append(MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.LISTS_LAST.get().replace("%value%", tag));
            }
        }

        return builder.toString();
    }

    /**
     * Collapses consecutive dots ({@code ..}) to a single dot and removes a trailing
     * dot if one remains after collapsing.
     *
     * @param string the input string to clean
     * @return the string with extra dots removed
     */
    public static String removeExtraDot(String string){
        String s = string.replace("..", ".");

        if (s.endsWith(".")) {
            s = s.substring(0, s.lastIndexOf('.'));
        }

        return s;
    }

    /**
     * Truncates or returns {@code text} clamped to at most {@code digits} characters.
     * If the string is shorter than {@code digits} the original string is returned.
     *
     * @param text   the input string
     * @param digits the maximum number of characters to retain
     * @return the (possibly truncated) string
     */
    public static String resize(String text, int digits) {
        try {
            digits = getDigits(digits, text.length());
            return text.substring(0, digits);
        } catch (Exception e) {
            return text;
        }
    }

    /**
     * Truncates the decimal portion of a numeric string to at most {@code digits}
     * decimal places.  If the string contains no decimal point it is returned unchanged.
     *
     * @param text   the numeric string to truncate
     * @param digits the maximum number of digits to keep after the decimal point
     * @return the truncated string, or {@code text} if it has no decimal point
     */
    public static String truncate(String text, int digits) {
        if (! text.contains(".")) return text;

        try {
            digits = getDigits(text.indexOf(".") + digits + 1, text.length());
            return text.substring(0, digits);
        } catch (Exception e) {
            return text;
        }
    }

    /**
     * Returns the smaller of {@code start} and {@code otherSize}, used to safely
     * clamp a substring end index to the actual string length.
     *
     * @param start     the desired end index
     * @param otherSize the maximum allowable end index (usually the string length)
     * @return the clamped index
     */
    public static int getDigits(int start, int otherSize){
        return Math.min(start, otherSize);
    }

    /**
     * Filters a list of completion candidates to those that start with {@code param}
     * (case-insensitive) and returns them as a sorted set.
     *
     * @param of    the full list of possible completions
     * @param param the prefix typed by the user
     * @return a sorted set of completions that match the prefix
     */
    public static ConcurrentSkipListSet<String> getCompletion(List<String> of, String param){
        return of.stream()
                .filter(completion -> completion.toLowerCase(Locale.ROOT).startsWith(param.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
    }

    /**
     * Filters a sorted set of completion candidates to those that start with {@code param}
     * (case-insensitive).
     *
     * @param of    the full set of possible completions
     * @param param the prefix typed by the user
     * @return a sorted set of completions that match the prefix
     */
    public static ConcurrentSkipListSet<String> getCompletion(ConcurrentSkipListSet<String> of, String param){
        return of.stream()
                .filter(completion -> completion.toLowerCase(Locale.ROOT).startsWith(param.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
    }

    /**
     * Returns a copy of {@code args} with the elements at the given indices removed.
     *
     * @param args     the original argument array
     * @param toRemove zero-based indices of elements to exclude
     * @return a new array containing all elements not listed in {@code toRemove}
     */
    public static String[] argsMinus(String[] args, int... toRemove) {
        TreeMap<Integer, String> argsSet = new TreeMap<>();

        for (int i = 0; i < args.length; i++) {
            argsSet.put(i, args[i]);
        }

        for (int remove : toRemove) {
            argsSet.remove(remove);
        }

        return argsSet.values().toArray(new String[0]);
    }

    /**
     * Removes elements at the given indices from {@code args} and joins the remainder
     * into a single space-separated string.
     *
     * @param args     the original argument array
     * @param toRemove zero-based indices of elements to exclude
     * @return the remaining arguments joined by spaces
     */
    public static String argsToStringMinus(String[] args, int... toRemove){
        return normalize(argsMinus(args, toRemove));
    }

    /**
     * Joins all elements of {@code args} into a single space-separated string.
     *
     * @param args the argument array to join
     * @return the arguments joined by spaces
     */
    public static String argsToString(String[] args){
        TreeMap<Integer, String> argsSet = new TreeMap<>();

        for (int i = 0; i < args.length; i++) {
            argsSet.put(i, args[i]);
        }

        return normalize(argsSet);
    }

    /**
     * Applies colour-code translation and {@code %newline%} expansion to the given
     * text, then applies any additional {@link #formatted(String)} transformations.
     *
     * @param text the raw text to process
     * @return the fully colour-translated and formatted string
     */
    public static String codedString(String text){
        return formatted(newLined(Singularity.getInstance().getMessenger().codedString(text)));
    }

    /**
     * Converts legacy ampersand colour codes (e.g., {@code &c}) to their section-sign
     * equivalents (e.g., {@code §c}).
     *
     * @param text the text whose ampersand codes should be converted
     * @return the text with {@code &[0-9a-fklmnor]} replaced by {@code §[...]}
     */
    public static String replaceAmpersand(String text) {
        String regex = "((&)([0-9a-fklmnor]))";
        return text.replaceAll(regex, "§$3");
    }

    /**
     * Applies inline case-transformation tokens:
     * <ul>
     *   <li>Words prefixed with {@code <to_upper>} are converted to upper case (prefix stripped).</li>
     *   <li>Words prefixed with {@code <to_lower>} are converted to lower case (prefix stripped).</li>
     * </ul>
     *
     * @param string the text to process
     * @return the text with case-transformation tokens applied
     */
    public static String formatted(String string) {
        String[] strings = string.split(" ");

        for (int i = 0; i < strings.length; i ++) {
            if (strings[i].toLowerCase(Locale.ROOT).startsWith("<to_upper>")) {
                strings[i] = strings[i].toUpperCase(Locale.ROOT).replace("<TO_UPPER>", "");
            }
            if (strings[i].toLowerCase(Locale.ROOT).startsWith("<to_lower>")) {
                strings[i] = strings[i].toLowerCase(Locale.ROOT).replace("<to_lower>", "");
            }
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < strings.length; i ++) {
            if (i == strings.length - 1) {
                builder.append(strings[i]);
            } else {
                builder.append(strings[i]).append(" ");
            }
        }

        return builder.toString();
    }

    /**
     * Replaces the literal token {@code %newline%} with the platform newline character
     * ({@code \n}).
     *
     * @param text the text to process
     * @return the text with {@code %newline%} expanded
     */
    public static String newLined(String text){
        try {
            return text.replace("%newline%", "\n")/*.replace("%uniques%", String.valueOf(StreamLine.getInstance().getPlDir().listFiles().length))*/;
        } catch (Exception e) {
            return text.replace("%newline%", "\n");
        }
    }

    /**
     * Checks whether the given message string represents a command (i.e., starts with
     * {@code /}).
     *
     * @param msg the message to check
     * @return {@code true} if {@code msg} starts with {@code /}
     */
    public static boolean isCommand(String msg){
        return msg.startsWith("/");
    }

    /**
     * Joins a string array into a single space-separated string, skipping empty elements.
     * The last element is appended without a trailing space.
     *
     * @param splitMsg the array of tokens to join
     * @return the tokens joined by spaces
     */
    public static String normalize(String[] splitMsg){
        int i = 0;
        StringBuilder text = new StringBuilder();

        for (String split : splitMsg){
            i++;
            if (split.isEmpty()) continue;

            if (i < splitMsg.length)
                text.append(split).append(" ");
            else
                text.append(split);
        }

        return text.toString();
    }

    /**
     * Joins a sorted set of strings into a single space-separated string, skipping
     * empty elements.
     *
     * @param splitMsg the sorted set of tokens to join
     * @return the tokens joined by spaces
     */
    public static String normalize(TreeSet<String> splitMsg) {
        int i = 0;
        StringBuilder text = new StringBuilder();

        for (String split : splitMsg){
            i++;
            if (split.isEmpty()) continue;

            if (i < splitMsg.size())
                text.append(split).append(" ");
            else
                text.append(split);
        }

        return text.toString();
    }

    /**
     * Joins the values of an integer-keyed map into a single space-separated string,
     * skipping empty values and preserving the map's iteration order.
     *
     * @param splitMsg the ordered map of index-to-token entries to join
     * @return the tokens joined by spaces
     */
    public static String normalize(TreeMap<Integer, String> splitMsg) {
        int i = 0;
        StringBuilder text = new StringBuilder();

        for (Integer split : splitMsg.keySet()){
            i++;
            if (splitMsg.get(split).isEmpty()) continue;

            if (i < splitMsg.size())
                text.append(splitMsg.get(split)).append(" ");
            else
                text.append(splitMsg.get(split));
        }

        return text.toString();
    }

    /**
     * Returns {@code true} only if {@code object} is equal to every element in
     * {@code toEqual}.
     *
     * @param object  the reference object
     * @param toEqual the values that must all equal {@code object}
     * @return {@code true} if {@code object.equals(e)} holds for every {@code e}
     */
    public static boolean equalsAll(Object object, Object... toEqual){
        for (Object equal : toEqual) {
            if (! object.equals(equal)) return false;
        }

        return true;
    }

    /**
     * Returns {@code true} only if {@code object} is equal to every element in the
     * given collection.
     *
     * @param object  the reference object
     * @param toEqual the collection of values that must all equal {@code object}
     * @return {@code true} if {@code object.equals(e)} holds for every {@code e}
     */
    public static boolean equalsAll(Object object, Collection<Object> toEqual){
        for (Object equal : toEqual) {
            if (! object.equals(equal)) return false;
        }

        return true;
    }

    /**
     * Returns {@code true} if {@code object} is equal to at least one element in
     * the given collection.
     *
     * @param object  the reference object
     * @param toEqual the collection of values to test against
     * @return {@code true} if {@code object.equals(e)} holds for any {@code e}
     */
    public static boolean equalsAny(Object object, Collection<?> toEqual){
        for (Object equal : toEqual) {
            if (object.equals(equal)) return true;
        }

        return false;
    }

    /**
     * Splits a comma-separated string into a list of individual tokens.
     *
     * @param string the comma-separated input
     * @return an immutable list of the split tokens
     */
    public static List<String> getStringListFromString(String string) {
        String[] strings = string.split(",");

        return List.of(strings);
    }

    /**
     * Returns {@code true} if the array is {@code null} or its length is less than or
     * equal to {@code lessThanOrEqualTo}.
     *
     * @param thingArray        the array to check; may be {@code null}
     * @param lessThanOrEqualTo the threshold length
     * @return {@code true} if {@code thingArray == null || thingArray.length <= lessThanOrEqualTo}
     */
    public static boolean isNullOrLessThanEqualTo(Object[] thingArray, int lessThanOrEqualTo) {
        if (thingArray == null) return true;
        return thingArray.length <= lessThanOrEqualTo;
    }
}
