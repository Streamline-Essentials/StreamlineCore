package host.plas.executables.aliases;

import gg.drak.thebase.lib.leonhard.storage.sections.FlatFileSection;
import host.plas.StreamlineUtilities;
import host.plas.executables.ExecutableHandler;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class AliasCompletions {
    public static ConcurrentSkipListMap<Integer, ConcurrentSkipListSet<String>> getCompletions(StreamAlias alias) {
        FlatFileSection section = alias.getCommandResource().getResource().getSection("basic.completion");

        ConcurrentSkipListMap<Integer, ConcurrentSkipListSet<String>> r = new ConcurrentSkipListMap<>();

        StreamlineUtilities.getGetters().forEach(a -> {
            ExecutableHandler.unloadGetter(a);
            ExecutableHandler.loadGetter(a);
        });

        for (String key : section.singleLayerKeySet()) {
            int i = 0;
            try {
                i = Integer.parseInt(key);
            } catch (Exception e) {
                StreamlineUtilities.getInstance().logWarning("StreamAlias '" + alias.getBase() + "' has a mis-configured completion argument! Argument '" + key + "' was found to not be an integer! It must be an integer!");
            }

            ConcurrentSkipListSet<String> completion = new ConcurrentSkipListSet<>(section.getOrSetDefault(key, new ArrayList<>()));
            for (String string : new ArrayList<>(completion)) {
                if (string.startsWith("@")) {
                    String identifier = string.substring("@".length());
                    if (! ExecutableHandler.isGetterLoaded(identifier)) continue;

                    completion.addAll(ExecutableHandler.getGetterAndGet(identifier));
                    completion.remove(string);
                }
            }

            r.put(i, completion);
        }

        return r;
    }
}
