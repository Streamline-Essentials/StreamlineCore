package host.plas.managers;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.scheduler.BaseRunnable;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

@Getter @Setter
public class NotificationTimer extends BaseRunnable implements Identifiable {
    @Getter @Setter
    private static ConcurrentSkipListSet<NotificationTimer> notifications = new ConcurrentSkipListSet<>();

    public static Optional<NotificationTimer> addNotification(String identifier, CosmicSender sender) {
        if (hasNotification(identifier, sender)) return Optional.empty();

        NotificationTimer notificationTimer = new NotificationTimer(identifier, sender);
        notifications.add(notificationTimer);

        return Optional.of(notificationTimer);
    }

    public static void removeNotification(String identifier, CosmicSender sender) {
        if (! hasNotification(identifier, sender)) return;

        getNotificationTimer(identifier, sender).ifPresent(notification -> notifications.remove(notification));
    }

    public static Optional<NotificationTimer> getNotificationTimer(String identifier, CosmicSender sender) {
        AtomicReference<NotificationTimer> notificationTimer = new AtomicReference<>();

        notifications.forEach(notification -> {
            if (notification.getIdentifier().equals(identifier) && notification.getSender().equals(sender)) {
                notificationTimer.set(notification);
            }
        });

        return Optional.ofNullable(notificationTimer.get());
    }

    public static boolean hasNotification(String identifier, CosmicSender sender) {
        return getNotificationTimer(identifier, sender).isPresent();
    }

    private String identifier;
    private CosmicSender sender;

    private NotificationTimer(String identifier, CosmicSender sender) {
        super(5 * 20, 1); // 5 second delayed then cancels. Asynchronous.

        this.identifier = identifier;
        this.sender = sender;
    }

    @Override
    public void run() {
        removeNotification(identifier, sender);

        cancel();
    }
}
