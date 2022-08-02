package net.streamline.api.holders;

import de.leonhard.storage.Config;
import net.streamline.api.configs.FlatFileResource;
import net.streamline.base.Streamline;
import net.streamline.utils.MessagingUtils;
import org.geysermc.api.Geyser;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.api.session.Connection;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.io.File;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

public class GeyserHolder extends AbstractHolder<GeyserApiBase> {
    public static class Saver extends FlatFileResource<Config> {
        public Saver() {
            super(Config.class, "geyser-uuids.yml", Streamline.getInstance().getDataFolder(), false);
        }

        public void put(String uuid, String name) {
            resource.set(uuid, name);
        }

        public String getName(String uuid) {
            reloadResource();

            return resource.getString(uuid);
        }

        public TreeMap<String, String> getEntries() {
            TreeMap<String, String> r = new TreeMap<>();

            for (String key : resource.singleLayerKeySet()) {
                r.put(key, resource.getString(key));
            }

            return r;
        }

        public String getUUID(String name) {
            for (String uuid : getEntries().keySet()) {
                if (resource.getString(uuid).equals(name)) return uuid;
            }

            return null;
        }
    }

    public Saver saver;

    public GeyserHolder() {
        super("Geyser");
        if (super.isPresent()) {
            try {
                setApi(Geyser.api());
                saver = new Saver();
                MessagingUtils.logInfo("Hooked into Geyser! Enabling Geyser support!");
            } catch (Exception e) {
                MessagingUtils.logSevere("Error hooking into Geyser! Disabling Geyser support!");
            }
        }
    }

    public List<? extends Connection> getOnline() {
        return getApi().onlineConnections();
    }

    public boolean isGeyserPlayerByUUID(String uuid) {
        for (Connection connection : getOnline()) {
            if (connection.uuid().toString().equals(uuid)) return true;
        }

        return saver.getName(uuid) != null;
    }

    public boolean isGeyserPlayerByName(String name) {
        for (Connection connection : getOnline()) {
            if (connection.name().equals(name)) return true;
        }

        return saver.getUUID(name) != null;
    }

    public String getUUID(String name) {
        for (Connection connection : getOnline()) {
            if (connection.name().equals(name)) {
                String uuid = connection.uuid().toString();
                saver.put(uuid, name);
                return uuid;
            }
        }

        return saver.getUUID(name);
    }

    public String getName(String uuid) {
        for (Connection connection : getOnline()) {
            if (connection.uuid().toString().equals(uuid)) {
                String name = connection.name();
                saver.put(uuid, name);
                return name;
            }
        }

        return saver.getName(uuid);
    }
}
