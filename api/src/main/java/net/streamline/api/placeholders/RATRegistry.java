package net.streamline.api.placeholders;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.objects.AtomicString;
import net.streamline.api.placeholders.callbacks.RATCallback;
import net.streamline.api.placeholders.replaceables.*;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UserUtils;

import java.util.concurrent.ConcurrentSkipListMap;

public class RATRegistry {
    @Getter @Setter
    private static ConcurrentSkipListMap<String, AbstractReplaceable<? extends RATCallback>> replacements = new ConcurrentSkipListMap<>();

    public static void register(AbstractReplaceable<? extends RATCallback> replacement) {
        replacements.put(replacement.getHandledString().getRegex(), replacement);
    }

    public static void unregister(AbstractReplaceable<? extends RATCallback> replacement) {
        replacements.remove(replacement.getHandledString().getRegex());
    }

    public static void unregister(String from) {
        replacements.remove(from);
    }

    public static void unregisterAll() {
        replacements = new ConcurrentSkipListMap<>();
    }

    public static void unregisterAll(ModuleLike moduleLike) {
        replacements.values().removeIf(replacement -> replacement instanceof ModuleReplaceable moduleReplaceable && moduleReplaceable.getModule().equals(moduleLike) ||
                replacement instanceof UserModuleReplaceable userModuleReplaceable && userModuleReplaceable.getModule().equals(moduleLike));
    }

    public static void unregisterAll(String identifier) {
        replacements.values().removeIf(replacement -> replacement instanceof IdentifiedReplaceable identifiedReplaceable && identifiedReplaceable.getIdentifier().equals(identifier) ||
                replacement instanceof IdentifiedUserReplaceable identifiedUserReplaceable && identifiedUserReplaceable.getIdentifier().equals(identifier));
    }

    public static void unregisterAllStartsWith(String startsWith) {
        replacements.values().removeIf(replacements -> replacements.getHandledString().getRegex().startsWith(startsWith));
    }

    public static AbstractReplaceable<? extends RATCallback> getReplacement(String from) {
        return replacements.get(from);
    }

    public static String fetch(String from) {
        AtomicString result = new AtomicString(from);
        getReplacements().forEach((s, replacement) -> {
            if (replacement == null) return;
            if (! replacement.isReplaceWorthy()) return;
            if (replacement instanceof GenericReplaceable generic) {
                result.set(generic.fetch(result.get()));
            }
            if (replacement instanceof UserReplaceable user) {
                result.set(user.fetchAs(result.get(), UserUtils.getConsole()));
            }
        });

        return result.get();
    }

    public static String fetchDirty(String from) {
        String temp = from;
        temp = fetch(temp);
        while (temp != from) {
            from = temp;
            temp = fetch(temp);
        }
        return temp;
    }

    public static String fetch(String from, StreamlineUser user) {
        AtomicString result = new AtomicString(from);
        getReplacements().forEach((s, replacement) -> {
            if (replacement instanceof UserReplaceable userReplacement) {
                result.set(userReplacement.fetchAs(result.get(), user));
            }
        });
        result.set(fetch(result.get()));

        return result.get();
    }

    public static String fetchDirty(String from, StreamlineUser user) {
        String temp = from;
        temp = fetch(temp, user);
        while (temp != from) {
            from = temp;
            temp = fetch(temp, user);
        }
        return temp;
    }

    public static String defaultModuledFrom(String from, StreamlineModule module) {
        return module.getIdentifier() + "_" + from;
    }
}
