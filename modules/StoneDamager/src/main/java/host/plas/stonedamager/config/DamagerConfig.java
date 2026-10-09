package host.plas.stonedamager.config;

import host.plas.stonedamager.StoneDamager;
import host.plas.stonedamager.data.DamagerSelection;
import host.plas.stonedamager.data.IdList;
import lombok.Getter;
import singularity.configs.ModularizedConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The module's config.yml. Everything is read once per {@link #init()} into immutable values,
 * because the damage pass reads them for every living entity on the server;
 * {@code /reloadstonedamager} re-runs {@link #init()}.
 */
@Getter
public class DamagerConfig extends ModularizedConfig {
    private volatile List<DamagerSelection> selections = Collections.emptyList();

    private volatile boolean tryFindPlayer;
    private volatile double findPlayerRadius;

    public DamagerConfig() {
        super(StoneDamager.getInstance(), "config.yml", true);
        init();
    }

    @Override
    public void init() {
        reloadResource();

        List<DamagerSelection> loaded = new ArrayList<>();
        for (String key : singleLayerKeySet("damagers")) {
            loaded.add(readSelection(key));
        }
        selections = Collections.unmodifiableList(loaded);

        tryFindPlayer = bool("settings.try-find-player.enabled", true);
        findPlayerRadius = number("settings.try-find-player.radius", 30.0);
    }

    /** How far to look for a player to credit with each hit, or zero when that is off. */
    public double getAttributionRadius() {
        return tryFindPlayer ? Math.max(0, findPlayerRadius) : 0;
    }

    private DamagerSelection readSelection(String key) {
        String path = "damagers." + key;
        return DamagerSelection.builder()
                .identifier(key)
                .enabled(bool(path + ".enabled", true))
                // At least one tick, so a selection never fires more than once per tick.
                .ticksPerDamage(Math.max(1L, (long) number(path + ".ticks-per-damage", 10)))
                .damageAmount(number(path + ".damage-amount", 3.0))
                .xOffset(number(path + ".offset.x", 0.0))
                .yOffset(number(path + ".offset.y", -0.1))
                .zOffset(number(path + ".offset.z", 0.0))
                .includePermission(string(path + ".permission.include", ""))
                .excludePermission(string(path + ".permission.exclude", "stonedamager.bypass"))
                .materials(idList(path + ".materials"))
                .worlds(idList(path + ".worlds"))
                .entities(idList(path + ".entities"))
                .build();
    }

    private IdList idList(String path) {
        Object raw = getOrSetDefault(path + ".list", (Object) new ArrayList<String>());
        List<String> ids = new ArrayList<>();
        if (raw instanceof Iterable) {
            for (Object id : (Iterable<?>) raw) {
                if (id != null) ids.add(id.toString());
            }
        }
        return new IdList(ids, bool(path + ".is-blacklist", false));
    }

    /*
     * YAML hands back whatever type the value was written as (0 is an Integer, 0.5 a Double,
     * "true" a String), so values are read as Object and converted here.
     */

    private double number(String path, double def) {
        Object raw = getOrSetDefault(path, (Object) def);
        if (raw instanceof Number) return ((Number) raw).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            StoneDamager.getInstance().logWarning("'" + path + "' is not a number (" + raw + "); using " + def + ".");
            return def;
        }
    }

    private boolean bool(String path, boolean def) {
        Object raw = getOrSetDefault(path, (Object) def);
        if (raw instanceof Boolean) return (Boolean) raw;
        return Boolean.parseBoolean(String.valueOf(raw).trim());
    }

    private String string(String path, String def) {
        Object raw = getOrSetDefault(path, (Object) def);
        return raw == null ? def : raw.toString();
    }
}
