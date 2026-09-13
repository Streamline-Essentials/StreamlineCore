package host.plas.executables.functions;

import gg.drak.thebase.utils.MatcherUtils;
import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.data.uuid.UuidManager;
import singularity.modules.ModuleUtils;
import singularity.objects.SingleSet;
import singularity.utils.UserUtils;
import org.jetbrains.annotations.NotNull;
import host.plas.StreamlineUtilities;
import host.plas.executables.ExecutableHandler;
import host.plas.executables.ExecutableUser;
import host.plas.executables.MultipleUser;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class StreamFunction extends File {
    @Setter
    private boolean enabled;
    private final boolean valid;
    private final String identifier;

    public StreamFunction(File parent, @NotNull String child) {
        super(parent, child);
        this.valid = child.endsWith(".sf");
        this.identifier = this.valid ? child.substring(0, child.lastIndexOf(".")) : child;
    }

    public int run() {
        return runAs(UserUtils.getConsole());
    }

    public int runAs(CosmicSender user) {
        return runAs(user, new ConcurrentSkipListMap<>());
    }

    public int runAs(CosmicSender user, ConcurrentSkipListMap<String, String> assignables) {
        AtomicInteger count = new AtomicInteger();
        ConcurrentSkipListMap<Integer, SingleSet<ExecutableUser<?>, String>> map = getCommandsWithAs(user, assignables);
        for (int i : map.keySet()) {
            SingleSet<ExecutableUser<?>, String> set = map.get(i);
            if (set == null) continue;
            int result = set.getKey().runCommand(set.getValue());
            if (result > 0) count.getAndAdd(result);
        }

        return count.get();
    }

    public ConcurrentSkipListMap<Integer, String> lines() {
        try {
            Scanner reader = new Scanner(this);

            ConcurrentSkipListMap<Integer, String> lines = new ConcurrentSkipListMap<>();
            while (reader.hasNext()) {
                String s = reader.nextLine();
                lines.put(lines.size() + 1, s);
            }
            return lines;
        } catch (Exception e) {
            e.printStackTrace();
            return new ConcurrentSkipListMap<>();
        }
    }

    public ConcurrentSkipListMap<Integer, String> uncommentedLines() {
        ConcurrentSkipListMap<Integer, String> r = new ConcurrentSkipListMap<>();

        lines().forEach((integer, s) -> {
            String toAdd = s;
            while (toAdd.startsWith(" ")) {
                toAdd = toAdd.substring(1);
            }
            if (toAdd.startsWith("#")) return;
            r.put(integer, s);
        });

        return r;
    }

    public ConcurrentSkipListMap<Integer, SingleSet<ExecutableUser<?>, String>> getCommandsWithAs(CosmicSender as, ConcurrentSkipListMap<String, String> assignables) {
        ConcurrentSkipListMap<Integer, SingleSet<ExecutableUser<?>, String>> r = new ConcurrentSkipListMap<>();

        for (int integer : uncommentedLines().keySet()) {
            String s = uncommentedLines().get(integer);
            s = replaceAssignables(s, assignables);
            if (s.isEmpty() || s.isBlank()) continue;

            if (s.startsWith("@o")) {
//                r.put(integer, new SingleSet<>(new ExecutableUser<>(new OperatorUser(as)), ModuleUtils.replaceAllPlayerBungee(as, s.split(" ", 2)[1])));
            } else if (s.startsWith("@c")) {
                r.put(integer, new SingleSet<>(new ExecutableUser<>(UserUtils.getConsole()), ModuleUtils.replaceAllPlayerBungee(as, s.split(" ", 2)[1])));
            } else if (s.startsWith("@a")) {
                r.put(integer, new SingleSet<>(new ExecutableUser<>(new MultipleUser(ModuleUtils.getLoadedSendersSet())), ModuleUtils.replaceAllPlayerBungee(as, s.split(" ", 2)[1])));
            } else if (s.startsWith("@n:")) {
                List<String[]> groups = MatcherUtils.getGroups(MatcherUtils.matcherBuilder("[\\\"](.*?)[\\\"]", s), 1);
                if (groups.size() <= 0) continue;
                Optional<String> uuid = UuidManager.getUuidFromName(groups.get(0)[0]);
                if (uuid.isEmpty()) continue;
                r.put(integer, new SingleSet<>(new ExecutableUser<>(UserUtils.getOrCreateSender(uuid.get())), ModuleUtils.replaceAllPlayerBungee(as, s.split(" ", 2)[1])));
            } else if (s.startsWith("@u:")) {
                List<String[]> groups = MatcherUtils.getGroups(MatcherUtils.matcherBuilder("[\\\"](.*?)[\\\"]", s), 1);
                if (groups.size() <= 0) continue;
                Optional<String> uuid = UuidManager.getUuidFromName(groups.get(0)[0]);
                if (uuid.isEmpty()) continue;
                r.put(integer, new SingleSet<>(new ExecutableUser<>(UserUtils.getOrCreateSender(uuid.get())), ModuleUtils.replaceAllPlayerBungee(as, s.split(" ", 2)[1])));
            } else {
                r.put(integer, new SingleSet<>(new ExecutableUser<>(as), s));
            }
        }

        return r;
    }

    public static String replaceAssignables(String string, ConcurrentSkipListMap<String, String> assignables) {
        for (String s : assignables.keySet()) {
            string = string.replace(getReplaceableAssignable(s), assignables.get(s));
        }
        return string;
    }

    public static String getReplaceableAssignable(String before) {
        String after = before;
        after = "@!!" + after + "!!";

        return after;
    }

    public boolean isLoaded() {
        return ExecutableHandler.isFunctionLoaded(this);
    }

    public boolean load() {
        if (! isValid()) return false;
        if (isLoaded()) return false;
        ExecutableHandler.loadFunction(this);
        return true;
    }

    public boolean unload() {
        if (! isLoaded()) return false;
        ExecutableHandler.unloadFunction(this);
        return true;
    }

    public boolean enable() {
        if (! isValid()) {
            StreamlineUtilities.getInstance().logWarning("Could not enable function with identifier of '" + getIdentifier() + "' as it was invalid!");
            return false;
        }
        if (isEnabled()) return false;
        setEnabled(true);
        StreamlineUtilities.getInstance().logInfo("Enabled function with identifier of '" + getIdentifier() + "'!");
        return true;
    }

    public boolean disable() {
        if (! isEnabled()) return false;
        setEnabled(false);
        StreamlineUtilities.getInstance().logInfo("Disabled function with identifier of '" + getIdentifier() + "'!");
        return true;
    }
}
