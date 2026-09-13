package host.plas.config;

import host.plas.database.VerifiedUserKeeper;
import host.plas.discord.data.verified.VerifiedUser;
import host.plas.discord.data.verified.VerifiedUserLoader;
import host.plas.events.streamline.verification.off.UnVerificationAlreadyUnVerifiedEvent;
import host.plas.events.streamline.verification.off.UnVerificationFailureEvent;
import host.plas.events.streamline.verification.off.UnVerificationSuccessEvent;
import host.plas.events.streamline.verification.on.VerificationAlreadyVerifiedEvent;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import host.plas.StreamlineDiscord;
import host.plas.discord.MessagedString;
import host.plas.discord.messaging.BotMessageConfig;
import host.plas.discord.messaging.DiscordMessenger;
import host.plas.events.streamline.verification.on.VerificationFailureEvent;
import host.plas.events.streamline.verification.on.VerificationSuccessEvent;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;
import singularity.objects.SingleSet;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

@Setter
@Getter
public class VerifiedUsers {
    public static VerifiedUserLoader getLoader() {
        return StreamlineDiscord.getVerifiedUserLoader();
    }

    public static VerifiedUserKeeper getKeeper() {
        return StreamlineDiscord.getVerifiedUserKeeper();
    }

    public static VerifiedUser getOrCreate(String uuid) {
        return getLoader().getOrCreate(uuid);
    }

    public static Optional<VerifiedUser> getOrGet(String uuid) {
        return getLoader().getOrLoad(uuid);
    }

    public static ConcurrentSkipListSet<VerifiedUser> getAll() {
        return getKeeper().getAll();
    }

    public static boolean isVerified(String uuid) {
        return getOrGet(uuid).isPresent();
    }

    public static boolean isVerified(CosmicSender user) {
        return isVerified(user.getUuid());
    }

    public static boolean isVerified(long discordId) {
        return getById(discordId).isPresent();
    }

    public static Optional<VerifiedUser> getById(long discordId) {
        return getAll().stream().filter(v -> v.getDiscordIds().contains(discordId)).findFirst();
    }

    public static SingleSet<MessageCreateData, BotMessageConfig> verifyUser(String uuid, MessagedString messagedString, String verification, boolean fromCommand) {
        CosmicSender user = ModuleUtils.getOrCreateSender(uuid).orElse(null);
        if (user == null) {
            new VerificationFailureEvent(fromCommand, uuid, messagedString.getAuthor().getIdLong(), messagedString, verification).fire();
            return DiscordMessenger.verificationMessage(UserUtils.getConsole(), StreamlineDiscord.getMessages().verifiedFailureGenericDiscord());
        } else {
            VerifiedUser verified = getOrCreate(uuid);
            if (! isVerified(user)) {
                verified.addDiscordId(messagedString.getAuthor().getIdLong());
                verified.save();

                VerificationSuccessEvent event = new VerificationSuccessEvent(fromCommand, uuid, messagedString.getAuthor().getIdLong(), messagedString, verification).fire();
                if (event.isCancelled()) return DiscordMessenger.verificationMessage(user, StreamlineDiscord.getMessages().verifiedFailureGenericDiscord());

                return DiscordMessenger.verificationMessage(user, StreamlineDiscord.getMessages().verifiedSuccessDiscord());
            } else {
                if (! verified.getDiscordIds().contains(messagedString.getAuthor().getIdLong())) {
                    verified.addDiscordId(messagedString.getAuthor().getIdLong());
                    verified.save();

                    VerificationSuccessEvent event = new VerificationSuccessEvent(fromCommand, uuid, messagedString.getAuthor().getIdLong(), messagedString, verification).fire();
                    if (event.isCancelled()) return DiscordMessenger.verificationMessage(user, StreamlineDiscord.getMessages().verifiedFailureGenericDiscord());

                    return DiscordMessenger.verificationMessage(user, StreamlineDiscord.getMessages().verifiedSuccessDiscord());
                }

                new VerificationAlreadyVerifiedEvent(fromCommand, uuid, messagedString.getAuthor().getIdLong(), messagedString, verification).fire();
                return DiscordMessenger.verificationMessage(user, StreamlineDiscord.getMessages().verifiedFailureAlreadyVerifiedDiscord());
            }
        }
    }

    public static SingleSet<MessageCreateData, BotMessageConfig> unverifyUser(String uuid, MessagedString messagedString, String verification, boolean fromCommand) {
        CosmicSender user = ModuleUtils.getOrCreateSender(uuid).orElse(null);
        if (user == null) {
            new UnVerificationFailureEvent(fromCommand, uuid, messagedString.getAuthor().getIdLong(), messagedString, verification).fire();
            return DiscordMessenger.verificationMessage(UserUtils.getConsole(), StreamlineDiscord.getMessages().verifiedFailureGenericDiscord());
        } else {
            if (isVerified(user)) {
                UnVerificationSuccessEvent event = new UnVerificationSuccessEvent(fromCommand, uuid, messagedString.getAuthor().getIdLong(), messagedString, verification).fire();
                if (event.isCancelled()) return DiscordMessenger.verificationMessage(user, StreamlineDiscord.getMessages().verifiedFailureGenericDiscord());

                VerifiedUser verified = getOrCreate(uuid);
                verified.removeDiscordId(messagedString.getAuthor().getIdLong());
                verified.save();
                return DiscordMessenger.verificationMessage(user, StreamlineDiscord.getMessages().verifiedSuccessDiscord());
            } else {
                new UnVerificationAlreadyUnVerifiedEvent(fromCommand, uuid, messagedString.getAuthor().getIdLong(), messagedString, verification).fire();
                return DiscordMessenger.verificationMessage(user, StreamlineDiscord.getMessages().verifiedFailureGenericDiscord());
            }
        }
    }

    public static void unverifyUser(CosmicSender streamlineUser) {
        if (streamlineUser == null) return;
        unverifyUser(streamlineUser.getUuid());
    }

    public static void unverifyUser(String uuid) {
        if (uuid == null) return;
        VerifiedUser verified = getOrGet(uuid).orElse(null);
        if (verified == null) return;

        verified.drop();
    }

    public static ConcurrentSkipListSet<Long> getDiscordIdsOf(String uuid) {
        ConcurrentSkipListSet<Long> r = new ConcurrentSkipListSet<>();

        VerifiedUser user = getOrGet(uuid).orElse(null);
        if (user == null) return r;

        return user.getDiscordIds();
    }

    public static void setPreferredDiscord(String uuid, long discordId) {
        VerifiedUser verified = getOrGet(uuid).orElse(null);
        if (verified == null) return;

        verified.setPreferredDiscordId(discordId);
        verified.save();
    }

    public static Optional<Long> getOrGetPreferredDiscord(String uuid) {
        VerifiedUser verified = getOrGet(uuid).orElse(null);
        if (verified == null) return Optional.empty();

        return verified.getPreferredDiscordIdOptional();
    }

    public static Optional<String> getUUIDfromDiscordID(long discordId) {
        return getById(discordId).map(VerifiedUser::getUuid);
    }

    public static void validateAllUsers() {
        getAll().forEach(user -> {
            if (user.getDiscordIds().isEmpty()) {
                user.drop();
            }
        });
    }
}
