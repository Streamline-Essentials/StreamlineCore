package host.plas.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.ProxProtect;
import host.plas.data.Hazard;
import host.plas.data.action.HazardAction;
import host.plas.data.action.HazardActionType;
import host.plas.data.cause.HazardCauseType;
import host.plas.data.condition.HazardCondition;
import host.plas.data.condition.HazardConditionType;

import java.util.concurrent.ConcurrentSkipListSet;

public class HazardsConfig extends SimpleConfiguration {
    public HazardsConfig() {
        super("hazards.yml", // The name of the file.
                ProxProtect.getInstance(), // The module instance.
                true); // Whether to copy the file from the jar. (Would have to be placed in the resources folder.)
    }

    @Override
    public void init() {
        getHazards();
    }

    public ConcurrentSkipListSet<Hazard> getHazards() {
        reloadResource();

        ConcurrentSkipListSet<Hazard> hazards = new ConcurrentSkipListSet<>();

        singleLayerKeySet().forEach(key1 -> {
            HazardCauseType causeType = HazardCauseType.valueOf(getOrSetDefault(key1 + ".cause", HazardCauseType.CHAT.name()));

            ConcurrentSkipListSet<HazardCondition> conditions = new ConcurrentSkipListSet<>();

            singleLayerKeySet(key1 + ".conditions").forEach(key2 -> {
                String path = key1 + ".conditions." + key2;

                HazardConditionType conditionType = HazardConditionType.valueOf(getOrSetDefault(path + ".type", HazardConditionType.MESSAGE_CONTAINS.name()));
                String value = getOrSetDefault(path + ".value", "bad word");

                HazardCondition condition = new HazardCondition(
                        key2,
                        conditionType,
                        value
                );

                conditions.add(condition);
            });

            ConcurrentSkipListSet<HazardAction> actions = new ConcurrentSkipListSet<>();

            singleLayerKeySet(key1 + ".actions").forEach(key2 -> {
                String path = key1 + ".actions." + key2;

                HazardActionType actionType = HazardActionType.valueOf(getOrSetDefault(path + ".type", HazardActionType.MESSAGE.name()));
                String value = getOrSetDefault(path + ".value", "You have been banned.");

                HazardAction action = new HazardAction(
                        key2,
                        actionType,
                        value
                );

                actions.add(action);
            });

            Hazard hazard = new Hazard(
                    key1,
                    causeType,
                    conditions,
                    actions
            );

            hazards.add(hazard);
        });

        return hazards;
    }
}
