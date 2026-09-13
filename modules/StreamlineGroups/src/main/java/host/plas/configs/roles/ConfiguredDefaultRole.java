package host.plas.configs.roles;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class ConfiguredDefaultRole {
    @Getter @Setter
    private String identifier;
    @Getter @Setter
    private int priority;
    @Getter @Setter
    private String name;
    @Getter @Setter
    private int max;
    @Getter @Setter
    private ConcurrentSkipListSet<String> flags;

    public ConfiguredDefaultRole(String identifier, int priority, String name, int max, ConcurrentSkipListSet<String> flags) {
        setIdentifier(identifier);
        setPriority(priority);
        setName(name);
        setMax(max);
        setFlags(flags);
    }
}
