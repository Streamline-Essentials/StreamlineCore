package host.plas.savables;

import host.plas.configs.ConfiguredChatChannel;
import host.plas.database.MyLoader;

import java.util.concurrent.ConcurrentSkipListSet;

public class ChatterManager {
    public static ConcurrentSkipListSet<SavableChatter> getChattersViewingChannel(ConfiguredChatChannel channel) {
        ConcurrentSkipListSet<SavableChatter> r = new ConcurrentSkipListSet<>();

        MyLoader.getInstance().getLoaded().forEach(savableChatter -> {
            if (savableChatter.canMessageFrom(channel)) r.add(savableChatter);
        });

        return r;
    }
}
