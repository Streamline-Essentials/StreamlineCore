package singularity.redis;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RedisMessage {
    private String channel;
    private String message;

    public RedisMessage(String channel, String message) {
        this.channel = channel;
        this.message = message;
    }

    public void send() {
        OwnRedisClient.sendMessage(this);
    }
}
