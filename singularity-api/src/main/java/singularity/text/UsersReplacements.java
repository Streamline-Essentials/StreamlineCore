package singularity.text;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.Optional;

@Getter @Setter
public class UsersReplacements implements Identifiable {
    private String identifier;

    private Cache<String, String> replacements;

    public UsersReplacements(String identifier, Cache<String, String> replacements) {
        this.identifier = identifier;
        this.replacements = replacements;
    }

    public UsersReplacements(String identifier) {
        this(identifier, Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .build()
        );
    }

    public void addReplacement(String key, String value) {
        if (hasReplacement(key)) {
            removeReplacement(key);
        }
        replacements.put(key, value);
    }

    public void removeReplacement(String key) {
        replacements.invalidate(key);
    }

    public Optional<String> getReplacement(String key) {
        return Optional.ofNullable(replacements.getIfPresent(key));
    }

    public boolean hasReplacement(String key) {
        return getReplacement(key).isPresent();
    }

    public String getReplacement(String key, String orElse) {
        return getReplacement(key).orElse(orElse);
    }
}
