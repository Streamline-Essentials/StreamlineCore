package host.plas.utils;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.onarandombox.multiverseinventories.MultiverseInventories;
import com.onarandombox.multiverseinventories.WorldGroup;
import host.plas.configs.MainConfig;
import net.playavalon.mythicdungeons.dungeons.Instance;
import org.bukkit.Bukkit;
import org.bukkit.World;
import singularity.utils.MessageUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class MVUtils {
    public static MultiverseCore getMVCore() {
        return (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
    }

    public static MultiverseInventories getMVInventories() {
        return (MultiverseInventories) Bukkit.getPluginManager().getPlugin("Multiverse-Inventories");
    }

    public static MultiverseWorld getMVWorld(World world) {
        return getMVCore().getMVWorldManager().getMVWorld(world);
    }

    public static MultiverseWorld getOrCreateMVWorld(World world) {
        MultiverseWorld mvWorld = getMVWorld(world);
        if (mvWorld == null) {
            getMVCore().getMVWorldManager().addWorld(world.getName(), world.getEnvironment(), null, null, null, null);
            mvWorld = getMVWorld(world);
        }
        return mvWorld;
    }

    public static void onWorldLoad(World world) {
        MultiverseWorld w = getOrCreateMVWorld(world);

        String worldName = world.getName();
        String abrWorldName;

        World.Environment environment = null;
        if (worldName.endsWith("_nether")) {
            environment = World.Environment.NETHER;
            abrWorldName = worldName.substring(0, worldName.length() - "_nether".length());
        } else if (worldName.endsWith("_the_end")) {
            environment = World.Environment.THE_END;
            abrWorldName = worldName.substring(0, worldName.length() - "_the_end".length());
        } else {
            abrWorldName = worldName;
        }

        AtomicBoolean hasFired = new AtomicBoolean(false);

        MainConfig.getWorldActions().forEach(worldAction -> {
            if (hasFired.get()) return;

            if (abrWorldName.matches(worldAction.getRegex())) {
                if (worldAction.isCreateOwn()) {
                    getDefMVInvShare().removeWorld(worldName);
                    getOrCreateMVInvShare(abrWorldName).addWorld(worldName);
                } else {
                    getDefMVInvShare().addWorld(w.getName());
                }

                hasFired.set(true);
            }
        });
    }

    public static void onDungeonLoad(Instance dungeon) {
        World w = dungeon.getInstanceWorld();

        onWorldLoad(w);
    }

    public static void onDungeonUnload(Instance dungeon) {
        World w = dungeon.getInstanceWorld();

        WorldGroup group = getMVInvShare(w.getName());
        if (group != null) {
            group.removeWorld(w.getName());
            if (group.getWorlds().isEmpty()) {
                getMVInventories().getGroupManager().removeGroup(group);
            }
        }

        boolean b = getMVCore().deleteWorld(w.getName());
        if (! b) {
            try {
                Bukkit.unloadWorld(w, false);
            } catch (Exception e) {
//                e.printStackTrace();
            }
            if (! w.getWorldFolder().delete()) {
                MessageUtils.logDebug("Failed to delete world folder: " + w.getWorldFolder().getName());
            }
        }
    }

    public static WorldGroup getDefMVInvShare() {
        return getMVInventories().getGroupManager().getDefaultGroup();
    }

    public static WorldGroup getMVInvShare(String worldName) {
        return getMVInventories().getGroupManager().getGroup(worldName);
    }

    public static WorldGroup getOrCreateMVInvShare(String worldName) {
        WorldGroup group = getMVInvShare(worldName);
        if (group == null) {
            group = getMVInventories().getGroupManager().newEmptyGroup(worldName);
        }
        return group;
    }
}
