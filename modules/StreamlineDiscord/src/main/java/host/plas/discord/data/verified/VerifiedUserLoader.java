package host.plas.discord.data.verified;

import host.plas.StreamlineDiscord;
import host.plas.database.DiscordMiddleware;
import host.plas.database.VerifiedUserKeeper;
import singularity.database.modules.DBKeeper;
import singularity.loading.Loader;

import java.util.Optional;

public class VerifiedUserLoader extends Loader<VerifiedUser> {
    public VerifiedUserLoader getInstance() {
        return StreamlineDiscord.getVerifiedUserLoader();
    }

    @Override
    public DBKeeper<VerifiedUser> getKeeper() {
        return StreamlineDiscord.getVerifiedUserKeeper();
    }

    @Override
    public VerifiedUser getConsole() {
        return null;
    }

    @Override
    public void fireLoadEvents(VerifiedUser verifiedUser) {

    }

    @Override
    public VerifiedUser instantiate(String s) {
        return new VerifiedUser(s);
    }

    @Override
    public void fireCreateEvents(VerifiedUser verifiedUser) {

    }

    @Override
    public Optional<VerifiedUser> get(String identifier) {
        Optional<VerifiedUser> middlewareUser = DiscordMiddleware.getVerifiedUser(identifier);
        if (middlewareUser.isPresent()) return middlewareUser;

        return super.get(identifier);
    }

    public boolean isLoaded(VerifiedUser user) {
        return isLoaded(user.getIdentifier());
    }
}
