package singularity.redis;

import singularity.data.teleportation.TPTicket;
import singularity.utils.MessageUtils;

public class TPTicketListener extends RedisListener {
    public TPTicketListener() {
        super("main-tpticket-listener", TPTicket.REDIS_CHANNEL);

        MessageUtils.logInfo("Loading &cTPTicket Redis Listener&r...");
    }

    @Override
    public void onMessage(RedisMessage message) {
        try {
            MessageUtils.logDebug("&cTPTicketListener&f: Received message on channel &d" + message.getChannel() + "&f: &d" + message.getMessage());

            TPTicket tpTicket = TPTicket.fromRedisMessage(message);
            tpTicket.onFromRedis();
        } catch (Throwable e) {
            MessageUtils.logWarning("&cTPTicketListener: &fError processing message: &d" + message.getMessage());
            MessageUtils.logWarning("&cTPTicketListener: &fError: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
