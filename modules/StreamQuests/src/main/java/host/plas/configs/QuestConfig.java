package host.plas.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.StreamQuests;
import host.plas.data.Quest;
import host.plas.data.require.Requirement;
import host.plas.data.require.RequirementType;
import host.plas.data.reward.Reward;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class QuestConfig extends SimpleConfiguration {
    @Getter @Setter
    private static ConcurrentSkipListSet<Quest> quests;

    public QuestConfig() {
        super("quests.yml", // The name of the file.
                StreamQuests.getInstance(), // The module instance.
                true); // Whether to copy the file from the jar. (Would have to be placed in the resources folder.)
    }

    @Override
    public void init() {
        setQuests(getQuestsFromConfig());
    }

    public void reloadConfig() {
        init();
    }

    public ConcurrentSkipListSet<Quest> getQuestsFromConfig() {
        reloadResource();

        ConcurrentSkipListSet<Quest> r = new ConcurrentSkipListSet<>();

        singleLayerKeySet().forEach(key1 -> {
            Quest quest = new Quest(key1);

            String prettyName = getOrSetDefault(key1 + ".pretty-name", "&c" + key1);
            quest.setPrettyName(prettyName);

            ConcurrentSkipListSet<Requirement> requirements = new ConcurrentSkipListSet<>();

            singleLayerKeySet(key1 + ".requirements").forEach(key2 -> {
                try {
                    String path = key1 + ".requirements." + key2;

                    String type = getResource().getString(path + ".type");
                    String value = getResource().getString(path + ".value");
                    double amount = getResource().getDouble(path + ".amount");

                    requirements.add(new Requirement(key2, RequirementType.valueOf(type), value, amount));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            quest.setRequirements(requirements);

            ConcurrentSkipListSet<Reward> rewards = new ConcurrentSkipListSet<>();

            singleLayerKeySet(key1 + ".rewards").forEach(key2 -> {
                try {
                    String path = key1 + ".rewards." + key2;

                    Double points = getResource().getDouble(path + ".points");
                    List<String> commands = getResource().getStringList(path + ".commands");

                    rewards.add(new Reward(key2, points, commands));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            quest.setRewards(rewards);

            r.add(quest);
        });

        return r;
    }
}
