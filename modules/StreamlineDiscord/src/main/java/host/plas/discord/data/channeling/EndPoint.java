package host.plas.discord.data.channeling;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import host.plas.StreamlineDiscord;
import host.plas.database.DiscordMiddleware;
import host.plas.discord.DiscordHandler;
import singularity.loading.Loadable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Setter
@Getter
public class EndPoint implements Loadable<EndPoint> {
    private String identifier; // UUID for database purposes.

    private EndPointType type;
    private String endPointIdentifier; // Server name, uuid, or permission, or room name.
    private String toFormat;

    private boolean fullyLoaded = false;

    public EndPoint(String identifier) {
        this.identifier = identifier;
    }

    public EndPoint() {
        this(UUID.randomUUID().toString());
    }

    public TextChannel asServerTextChannel() {
        if (! type.equals(EndPointType.DISCORD_TEXT)) return null;
        try {
            long channelId = Long.parseLong(endPointIdentifier);
            return DiscordHandler.getTextChannelById(channelId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EndPoint) {
            EndPoint o = (EndPoint) obj;
            return o.getType().equals(getType()) &&
                    o.getIdentifier().equals(getIdentifier());
        } else return super.equals(obj);
    }

    @Override
    public void save(boolean async) {
        DiscordMiddleware.saveEndPoint(this);
    }

    @Override
    public void load() {
        StreamlineDiscord.getEndPointLoader().load(this);
    }

    @Override
    public void unload() {
        StreamlineDiscord.getEndPointLoader().unload(this);
    }

    @Override
    public boolean isLoaded() {
        return StreamlineDiscord.getEndPointLoader().isLoaded(this.getIdentifier());
    }

    @Override
    public void saveAndUnload(boolean b) {
        save(b);
        unload();
    }

    @Override
    public EndPoint augment(CompletableFuture<Optional<EndPoint>> completableFuture, boolean isGet) {
        this.fullyLoaded = false;

        completableFuture.whenComplete((optional, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                this.fullyLoaded = true;
                return;
            }

            if (optional.isPresent()) {
                EndPoint endPoint = optional.get();

                this.setIdentifier(endPoint.getIdentifier());
                this.setType(endPoint.getType());
                this.setEndPointIdentifier(endPoint.getEndPointIdentifier());
                this.setToFormat(endPoint.getToFormat());
            } else {
                if (!isGet) {
                    save();
                }
            }

            this.fullyLoaded = true;
        });

        return this;
    }

    public void drop() {
        DiscordMiddleware.dropEndPoint(this);
        unload();
        this.type = null;
        this.endPointIdentifier = null;
        this.toFormat = null;
    }

}
