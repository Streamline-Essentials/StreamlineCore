package host.plas.data;

import host.plas.StreamQuests;
import host.plas.data.require.QuestKeys;
import host.plas.data.require.RequirementType;
import singularity.Singularity;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerWorld;
import singularity.interfaces.IGameplayHandler;
import singularity.scheduler.ModuleRunnable;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts {@code DIMENSION_JOINS} and {@code ENVIRONMENT_JOINS} on game servers without Bukkit,
 * which fire no world-change event: every online player's world is read each second and a
 * change counts as joining the new one. A player's first reading after login sets where they
 * are without counting it, matching Spigot, where only a change of world counts.
 */
public class WorldTracker extends ModuleRunnable {
    private final ConcurrentHashMap<String, String> lastWorlds = new ConcurrentHashMap<>();

    public WorldTracker() {
        super(StreamQuests.getInstance(), 20, 20);
    }

    @Override
    public void run() {
        Optional<IGameplayHandler> gameplay = Singularity.gameplay();
        if (gameplay.isEmpty()) return;

        lastWorlds.keySet().retainAll(UserUtils.getOnlinePlayers().keySet());

        UserUtils.getOnlinePlayers().keySet().forEach(uuid -> {
            String world = gameplay.get().getLocation(uuid)
                    .map(CosmicLocation::getWorld).map(PlayerWorld::getIdentifier).orElse(null);
            if (world == null) return;

            String last = lastWorlds.put(uuid, world);
            if (last == null || last.equals(world)) return;

            QuestManager.fireQuestEvent(uuid, RequirementType.DIMENSION_JOINS, world, 1.0);
            QuestManager.fireQuestEvent(uuid, RequirementType.ENVIRONMENT_JOINS, QuestKeys.environmentOf(world), 1.0);
        });
    }
}
