package host.plas.config;

import host.plas.StreamlineDiscord;
import singularity.configs.ModularizedConfig;

public class Messages extends ModularizedConfig {
    public Messages() {
        super(StreamlineDiscord.getInstance(), "messages.yml", true);

        init();
    }

    @Override
    public void init() {
        verifySuccessMinecraft();
        verifiedSuccessDiscord();
        verifiedFailureGenericDiscord();
        verifiedFailureAlreadyVerifiedDiscord();

        unVerifySuccessMinecraft();
        unVerifiedSuccessDiscord();
        unVerifiedFailureGenericDiscord();
        unVerifiedFailureAlreadyUnVerifiedDiscord();

        forwardedStreamlineLogin();
        forwardedStreamlineLogout();
        forwardedSpigotAdvancement();
        forwardedSpigotDeath();
    }

    public String verifySuccessMinecraft() {
        reloadResource();

        return getOrSetDefault("verification.success.minecraft", "&a&lSuccess&8! &eYou verified &d%streamline_user_absolute% &eas &d%discord_user_name_tagged%");
    }

    public String verifiedSuccessDiscord() {
        reloadResource();

        return getOrSetDefault("verification.success.discord", "--file:verify-success-response.json");
    }

    public String verifiedFailureGenericDiscord() {
        reloadResource();

        return getOrSetDefault("verification.failure.generic", "--file:verify-failure-response.json");
    }

    public String verifiedFailureAlreadyVerifiedDiscord() {
        reloadResource();

        return getOrSetDefault("verification.failure.already-verified", "--file:verify-already-response.json");
    }

    public String unVerifySuccessMinecraft() {
        reloadResource();

        return getOrSetDefault("unverification.success.minecraft", "&a&lSuccess&8! &eYou unverified &d%streamline_user_absolute%");
    }

    public String unVerifiedSuccessDiscord() {
        reloadResource();

        return getOrSetDefault("unverification.success.discord", "--file:unverify-success-response.json");
    }

    public String unVerifiedFailureGenericDiscord() {
        reloadResource();

        return getOrSetDefault("unverification.failure.generic", "--file:unverify-failure-response.json");
    }

    public String unVerifiedFailureAlreadyUnVerifiedDiscord() {
        reloadResource();

        return getOrSetDefault("unverification.failure.already-unverified", "--file:unverify-already-response.json");
    }

    public String forwardedStreamlineLogin() {
        reloadResource();

        return getOrSetDefault("forwarded.streamline.login", "--file:on-login.json");
    }

    public String forwardedStreamlineLogout() {
        reloadResource();

        return getOrSetDefault("forwarded.streamline.logout", "--file:on-logout.json");
    }

    public String forwardedSpigotAdvancement() {
        reloadResource();

        return getOrSetDefault("forwarded.spigot.advancement", "--file:on-advancement.json");
    }

    public String forwardedSpigotDeath() {
        reloadResource();

        return getOrSetDefault("forwarded.spigot.death", "--file:on-death.json");
    }
}
