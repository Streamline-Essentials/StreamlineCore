package host.plas.managers;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;
import singularity.scheduler.BaseRunnable;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public class TimedEntry<T> extends BaseRunnable implements Identifiable {
    private String identifier;
    private String discriminator;
    private T token;

    public TimedEntry(int delay, String identifier, String discriminator, T token) {
        super(delay, 1); // ({delay} / 20) second delayed then cancels. Asynchronous.

        this.identifier = identifier;
        this.discriminator = discriminator;
        this.token = token;

        addEntry(this);
    }

    @Override
    public void run() {
        cancel();
        removeEntry(this);
    }

    private static ConcurrentSkipListMap<String, ConcurrentSkipListSet<TimedEntry<?>>> entries = new ConcurrentSkipListMap<>();

    public static <T> Optional<TimedEntry<T>> getEntry(String identifier, String discriminator) {
        try {
            return entries.get(identifier).stream().filter(entry -> entry.getDiscriminator().equals(discriminator)).map(entry -> (TimedEntry<T>) entry).findFirst();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static <T> Optional<TimedEntry<T>> addEntry(TimedEntry<T> entry) {
        Optional<TimedEntry<T>> existingEntry = getEntry(entry.getIdentifier(), entry.getDiscriminator());
        if (existingEntry.isPresent()) return Optional.empty();

        ConcurrentSkipListSet<TimedEntry<?>> entrySet = entries.getOrDefault(entry.getIdentifier(), new ConcurrentSkipListSet<>());
        entrySet.add(entry);
        entries.put(entry.getIdentifier(), entrySet);

        return Optional.of(entry);
    }

    public static void removeEntry(String identifier, String discriminator) {
        getEntry(identifier, discriminator).ifPresent(entry -> {
            entry.cancel();
            entries.get(identifier).removeIf(e -> e.getDiscriminator().equals(discriminator));

            if (entries.get(identifier).isEmpty()) entries.remove(identifier);
        });

        if (entries.get(identifier).isEmpty()) entries.remove(identifier);
    }

    public static <T> void removeEntry(TimedEntry<T> entry) {
        removeEntry(entry.getIdentifier(), entry.getDiscriminator());
    }

    public static boolean hasEntry(String identifier, String discriminator) {
        return getEntry(identifier, discriminator).isPresent();
    }
}
