package singularity.redis;

import singularity.configs.given.GivenConfigs;
import singularity.data.teleportation.TPTicket;

import java.util.Arrays;

public class TPTicketListener extends RedisListener {
    public TPTicketListener() {
        super("main-tpticket-listener");
    }

    @Override
    public String[] getChannelsArray() {
        return new String[]{ "tp-ticket:put" };
    }

    @Override
    public void onMessage(String channel, String message) {
        if (Arrays.stream(getChannelsArray()).noneMatch(channel::equals)) {
            return; // Ignore messages not on the subscribed channels
        }

        RedisMessage redisMessage = new RedisMessage(channel, message);

        TPTicket tpTicket = TPTicket.fromRedisMessage(redisMessage);

        String server = GivenConfigs.getServerName();
        if (! tpTicket.getTargetServer().getIdentifier().equals(server)) return;

        TPTicket.addTicket(tpTicket);
    }
}
