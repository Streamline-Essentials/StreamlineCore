package host.plas.discord.data.verified;

import host.plas.StreamlineDiscord;
import host.plas.database.DiscordMiddleware;
import host.plas.config.VerifiedUsers;
import lombok.Getter;
import lombok.Setter;
import singularity.loading.Loadable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Getter @Setter
public class VerifiedUser implements Loadable<VerifiedUser> {
    private String identifier;
    private ConcurrentSkipListSet<Long> discordIds;
    private Optional<Long> preferredDiscordIdOptional;

    private boolean fullyLoaded = false;

    public VerifiedUser(String identifier) {
        this.identifier = identifier;
        this.discordIds = new ConcurrentSkipListSet<>();
        this.preferredDiscordIdOptional = Optional.empty();
    }

    public String getUuid() {
        return getIdentifier();
    }

    @Override
    public void load() {
        StreamlineDiscord.getVerifiedUserLoader().load(this);
    }

    @Override
    public boolean isLoaded() {
        return StreamlineDiscord.getVerifiedUserLoader().isLoaded(this);
    }

    @Override
    public void unload() {
        StreamlineDiscord.getVerifiedUserLoader().unload(this);
    }

    @Override
    public void save(boolean b) {
        DiscordMiddleware.saveVerifiedUser(this);
    }

    @Override
    public void saveAndUnload(boolean b) {
        save(b);
        unload();
    }

    @Override
    public VerifiedUser augment(CompletableFuture<Optional<VerifiedUser>> completableFuture, boolean isGet) {
        this.fullyLoaded = false;

        completableFuture.whenComplete((optional, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                this.fullyLoaded = true;
                return;
            }

            if (optional.isPresent()) {
                VerifiedUser user = optional.get();
                this.setIdentifier(user.getIdentifier());
                this.getDiscordIds().addAll(user.getDiscordIds());
                user.getPreferredDiscordIdOptional().ifPresent(this::setPreferredDiscordId);
            } else {
                if (! isGet) {
                    save();
                }
            }

            this.fullyLoaded = true;
        });

        return this;
    }

    public void addDiscordId(long discordId) {
        this.discordIds.add(discordId);
        save();
    }

    public void removeDiscordId(long discordId) {
        this.discordIds.remove(discordId);
        save();
    }

    public void drop() {
        DiscordMiddleware.dropVerifiedUser(this);
        unload();
        this.discordIds.clear();
        this.preferredDiscordIdOptional = Optional.empty();
    }

    public void setPreferredDiscordId(long discordId) {
        this.preferredDiscordIdOptional = Optional.of(discordId);
        validatePreferredDiscordId(true, true, false);
    }

    public void unsetPreferredDiscordId() {
        this.preferredDiscordIdOptional = Optional.empty();
    }

    public boolean containsDiscordId(long discordId) {
        return containsDiscordIdStrict(discordId) ||
               (this.preferredDiscordIdOptional.isPresent() && this.preferredDiscordIdOptional.get() == discordId);
    }

    public boolean containsDiscordIdStrict(long discordId) {
        return this.discordIds.contains(discordId);
    }

    public void validatePreferredDiscordId(boolean update, boolean save, boolean backwards) {
        getPreferredDiscordIdOptional().ifPresent(discordId -> {
            if (! isValidPreferredDiscordId(discordId)) {
                if (backwards) {
                    if (update) this.preferredDiscordIdOptional = Optional.empty();
                } else {
                    if (update) addDiscordId(discordId);
                }

                if (save) save();
            }
        });
    }

    public boolean isValidPreferredDiscordId(long discordId) {
        return containsDiscordIdStrict(discordId);
    }

    public ConcurrentSkipListSet<String> getDiscordIdsAsStringSet() {
        return this.discordIds.stream()
                .map(String::valueOf)
                .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
    }

    public String[] getDiscordIdsAsStringArray() {
        return getDiscordIdsAsStringSet().toArray(String[]::new);
    }

    public String getDiscordIdsAsString() {
        String r = String.join(";", getDiscordIdsAsStringSet());
        if (! r.isBlank() && ! r.endsWith(";")) r += ";";
        return r;
    }

    public void mapStringToDiscordId(String string) {
        if (string == null || string.isBlank()) return;

        String[] ids = string.split(";");
        for (String id : ids) {
            try {
                if (id.isBlank()) continue;
                long discordId = Long.parseLong(id);
                addDiscordId(discordId);
            } catch (Throwable e) {
                // Ignore invalid IDs
            }
        }
    }

    public void unverify() {
        VerifiedUsers.unverifyUser(this.getUuid());
    }


    public long getDiscordId() {
        if (preferredDiscordIdOptional.isPresent()) {
            return preferredDiscordIdOptional.get();
        } else if (! discordIds.isEmpty()) {
            return discordIds.first();
        } else {
            return -1L;
        }
    }
}
