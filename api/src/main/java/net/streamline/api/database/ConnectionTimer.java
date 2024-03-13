package net.streamline.api.database;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.scheduler.BaseDelayedRunnable;

import java.sql.Connection;
import java.util.Date;

@Getter @Setter
public class ConnectionTimer extends BaseDelayedRunnable {
    private Date qStart;

    public ConnectionTimer(Date qStart) {
        super(20 * 20); // 20 seconds

        this.qStart = qStart;
    }

    @Override
    public void runDelayed() {
//        Connection connection = SLAPI.getMainDatabase().getConnectionMap().get(getQStart());
//        if (connection == null) return;
//
//        try {
//            if (! connection.isClosed()) connection.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        SLAPI.getMainDatabase().getConnectionMap().remove(getQStart());
//        SLAPI.getMainDatabase().getConnectionTimers().remove(getQStart());
    }
}
