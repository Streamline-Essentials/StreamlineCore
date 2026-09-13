package host.plas.executables;

import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class MultipleUser {
    @Getter @Setter
    private ConcurrentSkipListSet<CosmicSender> users;

    public MultipleUser() {
        this.users = new ConcurrentSkipListSet<>();
    }

    public MultipleUser(CosmicSender... users) {
        this.users = new ConcurrentSkipListSet<>(List.of(users));
    }

    public MultipleUser(ConcurrentSkipListSet<CosmicSender> users) {
        this.users = users;
    }

    public void add(String uuid) {
        CosmicSender sender = UserUtils.getOrCreateSender(uuid).orElse(null);
        if (sender == null) return;
        add(sender);
    }

    public void add(CosmicSender user) {
        users.add(user);
    }

    public void remove(String uuid) {
        new ArrayList<>(users).forEach(a -> {
            if (a.getUuid().equals(uuid)) remove(a);
        });
    }

    public void remove(CosmicSender user) {
        users.remove(user);
    }
}
