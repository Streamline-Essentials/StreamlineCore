package host.plas.discord.data.channeling;

import host.plas.StreamlineDiscord;
import host.plas.database.DiscordMiddleware;
import singularity.database.modules.DBKeeper;
import singularity.loading.Loader;

import java.util.Optional;

public class EndPointLoader extends Loader<EndPoint> {
    public static EndPointLoader getInstance() {
        return StreamlineDiscord.getEndPointLoader();
    }

    @Override
    public Optional<EndPoint> get(String identifier) {
        Optional<EndPoint> middlewareEndPoint = DiscordMiddleware.getEndPoint(identifier);
        if (middlewareEndPoint.isPresent()) return middlewareEndPoint;

        return super.get(identifier);
    }

    @Override
    public DBKeeper<EndPoint> getKeeper() {
        return StreamlineDiscord.getEndPointKeeper();
    }

    @Override
    public EndPoint getConsole() {
        return null;
    }

    @Override
    public void fireLoadEvents(EndPoint endPoint) {
        // No events to fire for loading EndPoints.
    }

    @Override
    public EndPoint instantiate(String s) {
        return new EndPoint(s);
    }

    @Override
    public void fireCreateEvents(EndPoint endPoint) {
        // No events to fire for creating EndPoints.
    }
}
