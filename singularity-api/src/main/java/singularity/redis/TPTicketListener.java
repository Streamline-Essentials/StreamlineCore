package singularity.redis;

import singularity.data.teleportation.TPTicket;
import singularity.utils.MessageUtils;

/**
 * A {@link RedisListener} dedicated to receiving and processing
 * {@link TPTicket} messages published on the teleport-ticket Redis channel.
 *
 * <p>On startup this listener subscribes to {@link TPTicket#REDIS_CHANNEL}. When
 * a message arrives, the raw payload is deserialised into a {@link TPTicket} and
 * its {@link TPTicket#onFromRedis()} handler is invoked to execute the
 * cross-server teleport logic.</p>
 *
 * <p>This listener is initialised and registered automatically by
 * {@link OwnRedisClient#testConnection()} after a successful Redis connection.</p>
 */
public class TPTicketListener extends RedisListener {

    /**
     * Creates and registers the TP-ticket listener on the
     * {@link TPTicket#REDIS_CHANNEL} channel.
     */
    public TPTicketListener() {
        super("main-tpticket-listener", TPTicket.REDIS_CHANNEL);

        MessageUtils.logInfo("Loading &cTPTicket Redis Listener&r...");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Deserialises the incoming {@link RedisMessage} into a {@link TPTicket}
     * and delegates to {@link TPTicket#onFromRedis()} to handle the teleport
     * request. Any error during processing is caught and logged as a warning
     * without interrupting the listener.</p>
     *
     * @param message the received Redis message containing the serialised ticket
     */
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
