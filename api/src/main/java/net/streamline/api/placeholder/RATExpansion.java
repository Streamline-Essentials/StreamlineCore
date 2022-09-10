package net.streamline.api.placeholder;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class RATExpansion {
    @Getter @Setter
    private ConcurrentSkipListMap<String, Integer> checked = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private boolean enabled;
    @Getter @Setter
    private String identifier;
    @Getter @Setter
    private String author;
    @Getter @Setter
    private String version;
    @Getter @Setter
    private int calls;
    @Getter @Setter
    private ConcurrentSkipListMap<String, Integer> userCalls;

    public RATExpansion setCallForUser(StreamlineUser user, int set) {
        this.userCalls.put(user.getUUID(), set);
        return this;
    }

    public RATExpansion addCallForUser(StreamlineUser user, int add) {
        if (! this.userCalls.containsKey(user.getUUID())) return this.setCallForUser(user, add);
        else {
            return this.setCallForUser(user, this.userCalls.get(user.getUUID()) + add);
        }
    }

    public RATExpansion addCallForUser(StreamlineUser user) {
        return addCallForUser(user, 1);
    }

    public RATExpansion removeCallForUser(StreamlineUser user, int remove) {
        if (! getUserCalls().containsKey(user.getUUID())) return this.setCallForUser(user, remove);
        else {
            return this.setCallForUser(user, getUserCalls().get(user.getUUID()) - remove);
        }
    }

    public int getUserCalls(StreamlineUser user) {
        Integer calls = getUserCalls().get(user.getUUID());
        return calls == null ? 0 : calls;
    }

    public RATExpansion(String identifier, String author, String version) {
        setIdentifier(identifier);
        setAuthor(author);
        setVersion(version);
        setCalls(0);
        setUserCalls(new ConcurrentSkipListMap<>());
        setChecked(new ConcurrentSkipListMap<>());

        register();
    }

    public String doLogic(String params) {
        if (params.equals("logic_version")) return getVersion();
        if (params.equals("logic_author")) return getAuthor();
        if (params.equals("logic_identifier")) return getIdentifier();
        if (params.equals("logic_class_name")) return getClass().getSimpleName();
        if (params.equals("logic_calls_previous")) return String.valueOf(this.calls);
        this.calls ++;
        if (params.equals("logic_calls_now")) return String.valueOf(this.calls);
        if (params.equals("logic_checked_most")) return getMostUsedCheck().key;

        String logic = onLogic(params);
        return logic == null ? "" : logic;
    }

    public String doRequest(StreamlineUser user, String params) {
        String logic = doLogic(params);
        if (! logic.equals("")) return logic;

        if (params.equals("request_calls_previous")) return String.valueOf(getUserCalls(user));
        addCallForUser(user);
        if (params.equals("request_calls_now")) return String.valueOf(getUserCalls(user));

        String request = onRequest(user, params);
        return request == null ? "" : request;
    }

    public abstract String onLogic(String params);

    public abstract String onRequest(StreamlineUser user, String params);

    public void unregister() {
        if (! isRegistered()) return;
        SLAPI.getInstance().getPlatform().getRATAPI().unregisterExpansion(this);
    }

    public void register() {
        if (isRegistered()) return;
        SLAPI.getInstance().getPlatform().getRATAPI().registerExpansion(this);
    }

    public SingleSet<String, Integer> getMostUsedCheck() {
        SingleSet<String, AtomicInteger> currentMostUsed = new SingleSet<>("", new AtomicInteger(0));

        getChecked().forEach((s, integer) -> {
            if (integer > currentMostUsed.value.get()) {
                currentMostUsed.value.set(integer);
                currentMostUsed.key = s;
            }
        });

        return new SingleSet<>(currentMostUsed.key, currentMostUsed.value.get());
    }

    public boolean isRegistered() {
        return SLAPI.getInstance().getPlatform().getRATAPI().isRegistered(this);
    }
}
