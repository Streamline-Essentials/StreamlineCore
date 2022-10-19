package net.streamline.api.placeholder;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.BaseRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class RATExpansion implements Comparable<RATExpansion> {
    public static class CacheTimer extends BaseRunnable {

        @Getter
        private final RATExpansion parent;

        public CacheTimer(RATExpansion parent) {
            super(GivenConfigs.getMainConfig().placeholderCacheReleaseTicks(), GivenConfigs.getMainConfig().placeholderCacheReleaseTicks());
            this.parent = parent;
        }

        @Override
        public void run() {
            getParent().setCache(new ConcurrentSkipListMap<>());
            this.setPeriod(GivenConfigs.getMainConfig().placeholderCacheReleaseTicks());
        }
    }

    public void release() {
        setCache(new ConcurrentSkipListMap<>());
        setCachedFutures(new ConcurrentSkipListMap<>());
    }

    @Getter @Setter
    private ConcurrentSkipListMap<StreamlineUser, ConcurrentSkipListMap<String, String>> cache = new ConcurrentSkipListMap<>();

    public void cache(StreamlineUser user, String params, String outcome) {
        if (outcome == null) return;
        ConcurrentSkipListMap<String, String> map = getCache().get(user);
        if (map == null) map = new ConcurrentSkipListMap<>();
        map.put(params, outcome);
        getCache().put(user, map);
    }

    public boolean containsCached(StreamlineUser user, String params) {
        ConcurrentSkipListMap<String, String> map = getCache().get(user);
        if (map == null) {
            return false;
        }
        String cached = map.get(params);
        return cached != null;
    }

    public String getCached(StreamlineUser user, String params) {
        ConcurrentSkipListMap<String, String> map = getCache().get(user);
        if (map == null) {
            map = new ConcurrentSkipListMap<>();
            getCache().put(user, map);
            return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        }
        String cached = map.get(params);
        if (cached == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        }
        return cached;
    }

    @Getter @Setter
    private ConcurrentSkipListMap<StreamlineUser, ConcurrentSkipListMap<String, CompletableFuture<String>>> cachedFutures = new ConcurrentSkipListMap<>();

    public void cacheFuture(StreamlineUser user, String params, CompletableFuture<String> outcome) {
        if (outcome == null) return;
        ConcurrentSkipListMap<String, CompletableFuture<String>> map = getCachedFutures().get(user);
        if (map == null) map = new ConcurrentSkipListMap<>();
        map.put(params, outcome);
        getCachedFutures().put(user, map);
    }

    public boolean containsCachedFuture(StreamlineUser user, String params) {
        ConcurrentSkipListMap<String, CompletableFuture<String>> map = getCachedFutures().get(user);
        if (map == null) {
            return false;
        }
        CompletableFuture<String> cached = map.get(params);
        return cached != null;
    }

    public CompletableFuture<String> getCachedFuture(StreamlineUser user, String params) {
        ConcurrentSkipListMap<String, CompletableFuture<String>> map = getCachedFutures().get(user);
        if (map == null) {
            map = new ConcurrentSkipListMap<>();
            getCachedFutures().put(user, map);
        }
        CompletableFuture<String> cached = map.get(params);
        if (cached == null) {
            map.put(params, new CompletableFuture<>());
        }
        return cached;
    }

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
    @Getter @Setter
    private CacheTimer cacheTimer;

    public RATExpansion setCallForUser(StreamlineUser user, int set) {
        this.userCalls.put(user.getUuid(), set);
        return this;
    }

    public RATExpansion addCallForUser(StreamlineUser user, int add) {
        if (! this.userCalls.containsKey(user.getUuid())) return this.setCallForUser(user, add);
        else {
            return this.setCallForUser(user, this.userCalls.get(user.getUuid()) + add);
        }
    }

    public RATExpansion addCallForUser(StreamlineUser user) {
        return addCallForUser(user, 1);
    }

    public RATExpansion removeCallForUser(StreamlineUser user, int remove) {
        if (! getUserCalls().containsKey(user.getUuid())) return this.setCallForUser(user, remove);
        else {
            return this.setCallForUser(user, getUserCalls().get(user.getUuid()) - remove);
        }
    }

    public int getUserCalls(StreamlineUser user) {
        Integer calls = getUserCalls().get(user.getUuid());
        return calls == null ? 0 : calls;
    }

    public RATExpansion(String identifier, String author, String version) {
        setIdentifier(identifier);
        setAuthor(author);
        setVersion(version);
        setCalls(0);
        setUserCalls(new ConcurrentSkipListMap<>());
        setChecked(new ConcurrentSkipListMap<>());
        setCacheTimer(new CacheTimer(this));

        register();
    }

    public String doLogic(String params) {
        incrementChecked(params);

        if (params.equals("logic_version")) return getVersion();
        if (params.equals("logic_author")) return getAuthor();
        if (params.equals("logic_identifier")) return getIdentifier();
        if (params.equals("logic_class_name")) return getClass().getSimpleName();
        if (params.equals("logic_calls_previous")) return String.valueOf(this.calls);
        this.calls++;
        if (params.equals("logic_calls_now")) return String.valueOf(this.calls);
        if (params.equals("logic_checked_most")) return getMostUsedCheck().getKey();

        return onLogic(params);
    }

    public String doRequest(StreamlineUser user, String params) {
        String logic = doLogic(params);
        if (logic != null) if (!logic.equals("")) return logic;

        if (params.equals("request_calls_previous")) return String.valueOf(getUserCalls(user));
        addCallForUser(user);
        if (params.equals("request_calls_now")) return String.valueOf(getUserCalls(user));

        return onRequest(user, params);
    }

    public abstract String onLogic(String params);

    public abstract String onRequest(StreamlineUser user, String params);

    public void unregister() {
        if (! isRegistered()) return;
        SLAPI.getRatAPI().unregisterExpansion(this);
    }

    public void register() {
        if (isRegistered()) return;
        SLAPI.getRatAPI().registerExpansion(this);
    }

    public SingleSet<String, Integer> getMostUsedCheck() {
        SingleSet<String, AtomicInteger> currentMostUsed = new SingleSet<>("", new AtomicInteger(0));

        getChecked().forEach((s, integer) -> {
            if (integer > currentMostUsed.getValue().get()) {
                currentMostUsed.getValue().set(integer);
                currentMostUsed.setKey(s);
            }
        });

        return new SingleSet<>(currentMostUsed.getKey(), currentMostUsed.getValue().get());
    }

    public boolean isRegistered() {
        return SLAPI.getRatAPI().isRegistered(this);
    }

    public void incrementChecked(String params) {
        int i = 1;
        if (getChecked().containsKey(params)) {
            i = getChecked().get(params);
        }

        getChecked().put(params, i);
    }

    @Override
    public int compareTo(@NotNull RATExpansion o) {
        return CharSequence.compare(getIdentifier(), o.getIdentifier());
    }
}
