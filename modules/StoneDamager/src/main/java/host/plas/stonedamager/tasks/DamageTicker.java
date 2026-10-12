package host.plas.stonedamager.tasks;

import host.plas.stonedamager.StoneDamager;
import host.plas.stonedamager.data.DamagerSelection;
import host.plas.stonedamager.events.StoneDamageEvent;
import singularity.Singularity;
import singularity.events.entity.CosmicEntityEvent;
import singularity.interfaces.IGameplayHandler;
import singularity.interfaces.LivingEntityView;
import singularity.modules.ModuleUtils;
import singularity.scheduler.ModuleRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Counts down each selection's {@code ticks-per-damage} and, when one reaches zero, asks the
 * platform for a damage pass over every living entity with that selection.
 *
 * <p>Runs on Streamline's task thread, which only keeps the countdowns; the pass itself is
 * handed to the server, so this never waits on it.</p>
 */
public class DamageTicker extends ModuleRunnable {
    /** Ticks left per selection identifier. Only touched from this runnable's thread. */
    private final Map<String, Long> ticksLeft = new HashMap<>();

    public DamageTicker() {
        super(StoneDamager.getInstance(), 0, 1);
    }

    @Override
    public void run() {
        IGameplayHandler handler = Singularity.gameplay().orElse(null);
        if (handler == null) return;

        List<DamagerSelection> selections = StoneDamager.getDamagerConfig().getSelections();
        ticksLeft.keySet().removeIf(id -> selections.stream().noneMatch(s -> s.getIdentifier().equals(id)));

        for (DamagerSelection selection : selections) {
            if (! selection.isEnabled()) continue;

            long left = ticksLeft.getOrDefault(selection.getIdentifier(), selection.getTicksPerDamage()) - 1;
            if (left > 0) {
                ticksLeft.put(selection.getIdentifier(), left);
                continue;
            }
            ticksLeft.put(selection.getIdentifier(), selection.getTicksPerDamage());

            handler.damageLivingEntities(entity -> damageFor(selection, entity),
                    StoneDamager.getDamagerConfig().getAttributionRadius());
        }
    }

    private static double damageFor(DamagerSelection selection, LivingEntityView entity) {
        if (! selection.matches(entity)) return 0;
        if (! CosmicEntityEvent.hasListeners(StoneDamageEvent.class)) return selection.getDamageAmount();

        StoneDamageEvent event = new StoneDamageEvent(selection, entity.getUuid(), entity.getType(), entity.isPlayer(),
                entity.getWorldName(), entity.getX(), entity.getY(), entity.getZ(), selection.getDamageAmount());
        ModuleUtils.fireEvent(event);
        return event.isCancelled() ? 0 : event.getDamage();
    }
}
