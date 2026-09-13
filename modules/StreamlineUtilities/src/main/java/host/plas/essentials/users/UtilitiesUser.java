package host.plas.essentials.users;

import gg.drak.thebase.lib.re2j.Matcher;
import gg.drak.thebase.utils.MatcherUtils;
import host.plas.StreamlineUtilities;
import host.plas.database.MyLoader;
import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.data.teleportation.TPTicket;
import singularity.loading.Loadable;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

@Setter
@Getter
public class UtilitiesUser implements Loadable<UtilitiesUser> {
    private String identifier;

    public String getUuid() {
        return identifier;
    }

    private ConcurrentSkipListSet<StreamlineHome> homes;
    private String lastServer;

    private boolean fullyLoaded = false;

    public UtilitiesUser(String uuid) {
        this.identifier = uuid;

        homes = new ConcurrentSkipListSet<>();
        lastServer = "";

        register();
    }

    public String computableHomes() {
        StringBuilder builder = new StringBuilder();

        homes.forEach((home) -> {
            builder.append("!!!")
                    .append(home.getName()).append("::")
                    .append(home.getServer()).append("::")
                    .append(home.getWorld()).append("::")
                    .append(home.getX()).append("::")
                    .append(home.getY()).append("::")
                    .append(home.getZ()).append("::")
                    .append(home.getYaw()).append("::")
                    .append(home.getPitch())
                    .append(":::");
        });

        return builder.toString();
    }

    @Override
    public void load() {
        MyLoader.getInstance().load(this);
    }

    @Override
    public void unload() {
        MyLoader.getInstance().unload(this);
    }

    @Override
    public void saveAndUnload(boolean b) {
        save(b);
        unload();
    }

    @Override
    public boolean isLoaded() {
        return MyLoader.getInstance().isLoaded(this);
    }

    public static ConcurrentSkipListSet<StreamlineHome> computableHomes(String homes) {
        ConcurrentSkipListSet<StreamlineHome> r = new ConcurrentSkipListSet<>();

        Matcher matcher = MatcherUtils.matcherBuilder(
                "!!!([^:]+)::([^:]+)::([^:]+)::([^:]+)::([^:]+)::([^:]+)::([^:]+)::([^:]+):::",
                homes);
        List<String[]> groups = MatcherUtils.getGroups(matcher, 8);
        groups.forEach((group) -> {
            try {
                r.add(new StreamlineHome(
                        group[0], group[1], group[2],
                        Double.parseDouble(group[3]),
                        Double.parseDouble(group[4]),
                        Double.parseDouble(group[5]),
                        Float.parseFloat(group[6]),
                        Float.parseFloat(group[7])));
            } catch (Exception e) {
                StreamlineUtilities.getInstance().logWarning("Failed to parse home: " + e.getMessage());
                StreamlineUtilities.getInstance().logWarning(e.getStackTrace());
            }
        });

        return r;
    }

    public Optional<StreamlineHome> getHome(String name) {
        return homes.stream().filter((home) -> home.getName().equals(name)).findFirst();
    }

    public boolean hasHome(String name) {
        return getHome(name).isPresent();
    }

    public ConcurrentSkipListSet<String> getHomesOnServer(String server) {
        ConcurrentSkipListSet<String> homesOnServer = new ConcurrentSkipListSet<>();

        homes.forEach((home) -> {
            if (home.getServer().getIdentifier().equals(server)) {
                homesOnServer.add(home.getName());
            }
        });

        return homesOnServer;
    }

    public boolean hasAnyHomeOnServer(String server) {
        return ! getHomesOnServer(server).isEmpty();
    }

    public boolean hasHomeOnServer(String name, String server) {
        AtomicBoolean hasHome = new AtomicBoolean(false);

        getHomesOnServer(server).forEach((home) -> {
            if (home.equals(name)) {
                hasHome.set(true);
            }
        });

        return hasHome.get();
    }

    @Override
    public void save(boolean async) {
        try {
            StreamlineUtilities.getKeeper().save(this, async);

            StreamlineUtilities.getInstance().logInfo("Saved user " + getUuid());
        } catch (Throwable e) {
            StreamlineUtilities.getInstance().logWarning("Failed to save user " + getUuid() + ": " + e.getMessage());
            StreamlineUtilities.getInstance().logWarning(e.getStackTrace());
        }
    }

    @Override
    public UtilitiesUser augment(CompletableFuture<Optional<UtilitiesUser>> completableFuture, boolean isGet) {
        fullyLoaded = false;

        completableFuture.whenComplete((optional, throwable) -> {
            if (throwable != null) {
                StreamlineUtilities.getInstance().logWarning("Failed to load user " + getUuid() + ": " + throwable.getMessage());
                StreamlineUtilities.getInstance().logWarning(throwable.getStackTrace());

                fullyLoaded = true;
                return;
            }

            if (optional.isPresent()) {
                UtilitiesUser user = optional.get();

                this.homes = user.homes;
                this.lastServer = user.lastServer;
            } else {
                if (! isGet) {
                    save();
                }
            }

            fullyLoaded = true;
        });

        return this;
    }

    public void addHome(StreamlineHome home) {
        homes.add(home);
    }

    public void removeHome(StreamlineHome home) {
        removeHome(home.getName());
    }

    public void removeHome(String name) {
        homes.removeIf((home) -> home.getName().equals(name));
    }

    public void teleportTo(String homeName) {
        Optional<StreamlineHome> optional = getHome(homeName);
        if (optional.isEmpty()) return;
        StreamlineHome home = optional.get();

        CosmicPlayer player = UserUtils.getOrCreatePlayer(getUuid()).orElse(null);
        if (player == null) return;

        ModuleUtils.connect(player, home.getServer().getIdentifier());

        TPTicket ticket = new TPTicket(player.getIdentifier(), home);
        ticket.post();
    }

    public void goToLastServer() {
        CosmicPlayer player = UserUtils.getOrCreatePlayer(getUuid()).orElse(null);
        if (player == null) return;

        String lastServer = getLastServer();
        if (Objects.equals(lastServer, "null"))
            lastServer = StreamlineUtilities.getConfigs().lastServerDefaultServer();

        ModuleUtils.connect(player, lastServer);
    }

    public int getHomesCount() {
        return homes.size();
    }

    public void register() {
        MyLoader.getInstance().load(this);
    }

    public void unregister() {
        MyLoader.getInstance().unload(this);
    }

    public String getLastServerForDB() {
        return lastServer == null ? "" : lastServer;
    }

    public void setLastServerFromDB(String lastServer) {
        if (lastServer.isEmpty()) {
            lastServer = null;
            return;
        }
        this.lastServer = lastServer;
    }
}
