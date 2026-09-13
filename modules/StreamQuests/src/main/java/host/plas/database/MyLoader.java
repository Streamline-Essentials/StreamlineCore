package host.plas.database;

import host.plas.StreamQuests;
import host.plas.data.players.QuestPlayer;
import singularity.database.modules.DBKeeper;
import singularity.loading.Loader;

public class MyLoader extends Loader<QuestPlayer> {
    @Override
    public DBKeeper<QuestPlayer> getKeeper() {
        return StreamQuests.getKeeper();
    }

    @Override
    public QuestPlayer getConsole() {
        return null;
    }

    @Override
    public void fireLoadEvents(QuestPlayer questPlayer) {

    }

    @Override
    public QuestPlayer instantiate(String s) {
        return new QuestPlayer(s);
    }

    @Override
    public void fireCreateEvents(QuestPlayer questPlayer) {

    }
}