package host.plas.config;

import host.plas.MobGriefControl;
import lombok.Getter;
import singularity.configs.ModularizedConfig;

/**
 * The module's config.yml. Values are read once per {@link #init()} and cached, because
 * the listeners consult them on every entity damage event; {@code /mobgrief reload}
 * re-runs {@link #init()}.
 */
@Getter
public class GriefConfig extends ModularizedConfig {
    private volatile boolean creeperBlockDamage;
    private volatile boolean creeperEntityDamage;

    private volatile boolean ghastBlockDamage;
    private volatile boolean ghastEntityDamage;

    private volatile boolean endermanPickUpBlocks;

    private volatile boolean witherSkullBlockDamage;
    private volatile boolean witherSkullEntityDamage;
    private volatile boolean witherSelfBlockDamage;

    public GriefConfig() {
        super(MobGriefControl.getInstance(), "config.yml", true);
        init();
    }

    @Override
    public void init() {
        reloadResource();

        creeperBlockDamage = getOrSetDefault("creeper.explosion.block-damage", false);
        creeperEntityDamage = getOrSetDefault("creeper.explosion.entity-damage", true);

        ghastBlockDamage = getOrSetDefault("ghast.fireball.block-damage", false);
        ghastEntityDamage = getOrSetDefault("ghast.fireball.entity-damage", true);

        endermanPickUpBlocks = getOrSetDefault("enderman.pick-up-blocks", false);

        witherSkullBlockDamage = getOrSetDefault("wither.skulls.block-damage", false);
        witherSkullEntityDamage = getOrSetDefault("wither.skulls.entity-damage", true);
        witherSelfBlockDamage = getOrSetDefault("wither.self.block-damage", false);
    }
}
