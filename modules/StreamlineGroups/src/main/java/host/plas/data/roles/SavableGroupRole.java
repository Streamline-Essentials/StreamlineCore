package host.plas.data.roles;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import host.plas.data.flags.GroupFlag;

import java.util.Locale;
import java.util.concurrent.ConcurrentSkipListSet;

@Setter @Getter
public class SavableGroupRole implements Comparable<SavableGroupRole> {
    private String identifier;
    private int priority;
    private String name;
    private int max;
    private ConcurrentSkipListSet<String> flags;

    public SavableGroupRole(String identifier, int priority, String name, int max, ConcurrentSkipListSet<String> flags) {
        setIdentifier(identifier);
        setPriority(priority);
        setName(name);
        setMax(max);
        setFlags(flags);
    }

    public void addFlag(GroupFlag flag) {
        getFlags().add(flag.toString().toLowerCase(Locale.ROOT));
    }

    public void removeFlag(GroupFlag flag) {
        getFlags().remove(flag.toString().toLowerCase(Locale.ROOT));
    }

    public boolean hasFlag(GroupFlag flag) {
        return getFlags().contains(flag.toString().toLowerCase(Locale.ROOT));
    }

    @Override
    public int compareTo(@NotNull SavableGroupRole o) {
        return Integer.compare(getPriority(), o.getPriority());
    }
}
