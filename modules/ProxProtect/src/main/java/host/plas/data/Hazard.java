package host.plas.data;

import gg.drak.thebase.objects.Identifiable;
import host.plas.data.action.HazardAction;
import host.plas.data.cause.HazardCause;
import host.plas.data.cause.HazardCauseType;
import host.plas.data.condition.HazardCondition;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter @Setter
public class Hazard implements Identifiable {
    private String identifier;

    private HazardCauseType causeType;

    private ConcurrentSkipListSet<HazardCondition> conditions;
    private ConcurrentSkipListSet<HazardAction> actions;

    public Hazard(String identifier, HazardCauseType causeType, ConcurrentSkipListSet<HazardCondition> conditions, ConcurrentSkipListSet<HazardAction> actions) {
        this.identifier = identifier;
        this.causeType = causeType;

        this.conditions = conditions;
        this.actions = actions;
    }

    public Hazard(String identifier, HazardCauseType causeType) {
        this(identifier, causeType, new ConcurrentSkipListSet<>(), new ConcurrentSkipListSet<>());
    }

    public void addCondition(HazardCondition condition) {
        conditions.add(condition);
    }

    public void addAction(HazardAction action) {
        actions.add(action);
    }

    public void removeCondition(HazardCondition condition) {
        conditions.remove(condition);
    }

    public void removeAction(HazardAction action) {
        actions.remove(action);
    }

    public void clearConditions() {
        conditions.clear();
    }

    public void clearActions() {
        actions.clear();
    }

    public boolean checkCause(HazardCause cause) {
        AtomicBoolean pass = new AtomicBoolean(true);

        getConditions().forEach(condition -> {
            if (! pass.get()) return;
            if (! condition.checkHazard(cause)) pass.set(false);
        });

        return pass.get();
    }

    public void executeActions(HazardCause cause) {
        getActions().forEach(action -> action.execute(cause));
    }

    public boolean checkAndConclude(HazardCause cause) {
        if (checkCause(cause)) {
            executeActions(cause);
            return true;
        }

        return false;
    }
}
