package host.plas.configs.obj;

import gg.drak.thebase.lib.leonhard.storage.sections.FlatFileSection;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import host.plas.StreamlineUtilities;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

@Setter
@Getter
public class ConfiguredServer implements Comparable<ConfiguredServer> {
    String actualServer;
    ConcurrentSkipListSet<String> aliases;
    String prettyName;

    public ConfiguredServer(String actualServer, ConcurrentSkipListSet<String> aliases, String prettyName) {
        this.actualServer = actualServer;
        this.aliases = aliases;
        this.prettyName = prettyName;
    }

    public boolean hasAlias(String alias) {
        return aliases.contains(alias);
    }

    public boolean isActualServer(String server) {
        return actualServer.equalsIgnoreCase(server);
    }

    public boolean hasAliasOrActualName(String server) {
        return hasAlias(server) || isActualServer(server);
    }

    public void addAlias(String alias) {
        aliases.add(alias);
    }

    public void removeAlias(String alias) {
        aliases.remove(alias);
    }

    public void save() {
        StreamlineUtilities.getServersConfig().save(this);
    }

    public void remove() {
        StreamlineUtilities.getServersConfig().remove(this);
    }

    @Override
    public int compareTo(@NotNull ConfiguredServer o) {
        return CharSequence.compare(actualServer, o.actualServer);
    }

    public static ConfiguredServer buildFrom(String key, FlatFileSection section) {
        ConcurrentSkipListSet<String> aliases = new ConcurrentSkipListSet<>(section.getOrSetDefault("aliases", List.of()));
        String prettyName = section.getOrSetDefault("pretty-name", key);
        try {
            return new ConfiguredServer(key, aliases, prettyName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
