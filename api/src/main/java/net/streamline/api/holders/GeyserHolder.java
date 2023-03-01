//package net.streamline.api.holders;
//
//import net.streamline.api.SLAPI;
//import net.streamline.api.utils.MessageUtils;
//import org.geysermc.api.Geyser;
//import org.geysermc.api.GeyserApiBase;
//import org.geysermc.api.session.Connection;
//import tv.quaint.storage.resources.flat.simple.SimpleConfiguration;
//
//import java.util.List;
//import java.util.TreeMap;
//
//public class GeyserHolder extends StreamlineDependencyHolder<GeyserApiBase> {
//    public static class Saver extends SimpleConfiguration {
//        public Saver() {
//            super("geyser-uuids.yml", SLAPI.getInstance().getDataFolder(), false);
//        }
//
//        public void put(String uuid, String name) {
//            getResource().set(uuid, name);
//        }
//
//        public String getName(String uuid) {
//            reloadResource();
//
//            return getResource().getString(uuid);
//        }
//
//        public TreeMap<String, String> getEntries() {
//            TreeMap<String, String> r = new TreeMap<>();
//
//            for (String key : getResource().singleLayerKeySet()) {
//                r.put(key, getResource().getString(key));
//            }
//
//            return r;
//        }
//
//        public String getUUID(String name) {
//            for (String uuid : getEntries().keySet()) {
//                if (getResource().getString(uuid).equals(name)) return uuid;
//            }
//
//            return null;
//        }
//
//        @Override
//        public void init() {
//
//        }
//    }
//
//    public Saver saver;
//
//    public GeyserHolder() {
//        super("Geyser", "Geyser-BungeeCord", "Geyser-Velocity", "Geyser-Spigot", "geyser", "Geyser");
//        if (super.isPresent()) {
//            tryLoad(() -> {
//                setApi(Geyser.api());
//                saver = new Saver();
//                MessageUtils.logInfo("Hooked into Geyser! Enabling Geyser support!");
//                return null;
//            });
//        } else {
//            MessageUtils.logInfo("Did not detect a '" + getIdentifier() + "' plugin... Disabling support for '" + getIdentifier() + "'...");
//        }
//    }
//
//    public List<? extends Connection> getOnline() {
//        return getApi().onlineConnections();
//    }
//
//    public boolean isGeyserPlayerByUUID(String uuid) {
//        for (Connection connection : getOnline()) {
//            if (connection.uuid().toString().equals(uuid)) return true;
//        }
//
//        return saver.getName(uuid) != null;
//    }
//
//    public boolean isGeyserPlayerByName(String name) {
//        for (Connection connection : getOnline()) {
//            if (connection.name().equals(name)) return true;
//        }
//
//        return saver.getUUID(name) != null;
//    }
//
//    public String getUUID(String name) {
//        for (Connection connection : getOnline()) {
//            if (connection.name().equals(name)) {
//                String uuid = connection.uuid().toString();
//                saver.put(uuid, name);
//                return uuid;
//            }
//        }
//
//        return saver.getUUID(name);
//    }
//
//    public String getName(String uuid) {
//        for (Connection connection : getOnline()) {
//            if (connection.uuid().toString().equals(uuid)) {
//                String name = connection.name();
//                saver.put(uuid, name);
//                return name;
//            }
//        }
//
//        return saver.getName(uuid);
//    }
//}
