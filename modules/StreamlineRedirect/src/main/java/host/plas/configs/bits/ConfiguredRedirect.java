package host.plas.configs.bits;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

@Setter
@Getter
public class ConfiguredRedirect implements Comparable<ConfiguredRedirect> {
    private String identifier;
    private ConcurrentSkipListSet<String> fromServers;
    private List<String> toServers;

    public ConfiguredRedirect(String identifier, ConcurrentSkipListSet<String> fromServers, List<String> toServers) {
        this.identifier = identifier;
        this.fromServers = fromServers;
        this.toServers = toServers;
    }

    @Override
    public int compareTo(@NotNull ConfiguredRedirect o) {
        return identifier.compareTo(o.getIdentifier());
    }
}
