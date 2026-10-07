package singularity.utils.profiles;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Setter;
import singularity.configs.given.GivenConfigs;
import singularity.utils.MessageUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Player names, UUIDs and skins, without Mojang's API.
 *
 * <p>Sources, in order:</p>
 * <ol>
 *     <li>the platform, for players online on this server ({@link #setLocalSource});</li>
 *     <li>Bedrock players (Floodgate UUIDs, {@code 00000000-0000-0000-xxxx-xxxxxxxxxxxx}):
 *     Floodgate for online players, then GeyserMC's global API, which keeps the skins Geyser
 *     uploaded for them;</li>
 *     <li>Java players: playerdb.co, then api.ashcon.app.</li>
 * </ol>
 *
 * <p>Built to never get in the way of a server without internet or in offline mode:</p>
 * <ul>
 *     <li>nothing is fetched while the core's {@code no-internet} setting is on or lookups are
 *     {@linkplain #setEnabled disabled};</li>
 *     <li>a request that cannot connect stops every lookup for {@link #OFFLINE_PAUSE}, and a
 *     rate-limited or failing source is skipped for a growing pause, so a dead network costs
 *     one short timeout instead of one per lookup;</li>
 *     <li>offline-mode UUIDs (version 3, derived from a name) are never sent anywhere; they are
 *     looked up by name when one is known;</li>
 *     <li>lookups run on their own two daemon threads and return futures, results (and misses)
 *     are cached, and concurrent lookups of the same player share one request;</li>
 *     <li>failures are logged at debug level only.</li>
 * </ul>
 */
public final class PlayerLookup {
    private PlayerLookup() {
    }

    /** How long a found profile is reused. */
    public static final Duration FOUND_TTL = Duration.ofHours(6);
    /** How long a profile read from an online player is reused. */
    public static final Duration LOCAL_TTL = Duration.ofMinutes(10);
    /** How long a player no source knows stays unknown before being asked about again. */
    public static final Duration MISSING_TTL = Duration.ofMinutes(30);
    /** How long every lookup pauses after a request could not reach the internet. */
    public static final Duration OFFLINE_PAUSE = Duration.ofMinutes(5);
    /** The first pause for a rate-limited or failing source; it doubles up to {@link #MAX_BACKOFF}. */
    public static final Duration MIN_BACKOFF = Duration.ofSeconds(30);
    public static final Duration MAX_BACKOFF = Duration.ofMinutes(15);

    private static final int CACHE_SIZE = 10_000;
    private static final String PLAYERDB = "https://playerdb.co/api/player/minecraft/";
    private static final String ASHCON = "https://api.ashcon.app/mojang/v2/user/";
    private static final String GEYSER = "https://api.geysermc.org/v2/";
    private static final String USER_AGENT = "StreamlineCore (+https://github.com/Streamline-Essentials/StreamlineCore)";
    private static final Pattern JAVA_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    /** Whether lookups may use the network at all; the core's {@code no-internet} setting also stops them. */
    @Setter
    private static volatile boolean enabled = true;

    /** The Floodgate username prefix, used when Floodgate is not installed here (on a proxy's backend, say). */
    @Setter
    private static volatile String bedrockPrefix = ".";

    /** Reads the profile of a player online on this server; set by the platform. */
    @Setter
    private static volatile Function<UUID, Optional<CosmicProfile>> localSource = uuid -> Optional.empty();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final AtomicInteger THREADS = new AtomicInteger();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "Streamline-PlayerLookup-" + THREADS.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    private static final class Entry {
        private final CosmicProfile profile;
        private final long expiresAt;

        private Entry(CosmicProfile profile, Duration ttl) {
            this.profile = profile;
            this.expiresAt = System.currentTimeMillis() + ttl.toMillis();
        }

        private boolean expired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }

    /** Least recently used profiles beyond {@link #CACHE_SIZE} are dropped. Guarded by itself. */
    private static final Map<UUID, Entry> byUuid = new LinkedHashMap<UUID, Entry>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Entry> eldest) {
            return size() > CACHE_SIZE;
        }
    };
    /** Lower-cased name to UUID, for names looked up before; a missing name maps to {@code null} in {@link #missingNames}. */
    private static final Map<String, UUID> byName = new LinkedHashMap<String, UUID>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, UUID> eldest) {
            return size() > CACHE_SIZE;
        }
    };
    private static final Map<String, Long> missingNames = new ConcurrentHashMap<>();

    private static final Map<String, CompletableFuture<Optional<CosmicProfile>>> inFlight = new ConcurrentHashMap<>();

    private static volatile long offlineUntil = 0L;
    private static final Map<String, Long> hostPausedUntil = new ConcurrentHashMap<>();
    private static final Map<String, Long> hostBackoff = new ConcurrentHashMap<>();

    // ---- UUID kinds ---------------------------------------------------------------------------

    /** Whether {@code uuid} is a Floodgate (Bedrock) UUID: the high 64 bits are zero and the low ones hold the XUID. */
    public static boolean isBedrock(UUID uuid) {
        return uuid != null && uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() != 0L;
    }

    /** The Xbox user id inside a Floodgate UUID. */
    public static long xuid(UUID bedrockUuid) {
        return bedrockUuid.getLeastSignificantBits();
    }

    /** The Floodgate UUID of an Xbox user id. */
    public static UUID bedrockUuid(long xuid) {
        return new UUID(0L, xuid);
    }

    /** Whether {@code uuid} is an offline-mode UUID (version 3, derived from {@code OfflinePlayer:<name>}). */
    public static boolean isOfflineModeUuid(UUID uuid) {
        return uuid != null && uuid.version() == 3;
    }

    /** The UUID an offline-mode server gives {@code name}. */
    public static UUID offlineModeUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    /** The prefix Bedrock usernames carry: Floodgate's own setting when it is installed here. */
    public static String getBedrockPrefix() {
        return FloodgateHook.getPlayerPrefix().orElse(bedrockPrefix == null ? "" : bedrockPrefix);
    }

    /** Whether {@code name} carries the Bedrock prefix (never true for an empty prefix). */
    public static boolean isBedrockName(String name) {
        String prefix = getBedrockPrefix();
        return name != null && ! prefix.isEmpty() && name.startsWith(prefix) && name.length() > prefix.length();
    }

    // ---- cache --------------------------------------------------------------------------------

    /** The cached profile of {@code uuid}, or of the online player with it; never touches the network. */
    public static Optional<CosmicProfile> getCached(UUID uuid) {
        if (uuid == null) return Optional.empty();
        synchronized (byUuid) {
            Entry entry = byUuid.get(uuid);
            if (entry != null && ! entry.expired()) return Optional.ofNullable(entry.profile);
        }
        Optional<CosmicProfile> local = local(uuid);
        local.ifPresent(profile -> remember(uuid, profile, LOCAL_TTL));
        return local;
    }

    /**
     * The cached profile of {@code uuid}, starting a lookup when there is none, so a later call
     * finds it.
     */
    public static Optional<CosmicProfile> getCachedOrLookup(UUID uuid) {
        Optional<CosmicProfile> cached = getCached(uuid);
        if (cached.isEmpty() && ! isKnownMissing(uuid)) lookup(uuid);
        return cached;
    }

    /** Whether {@code uuid} was looked up recently and no source knew it. */
    public static boolean isKnownMissing(UUID uuid) {
        synchronized (byUuid) {
            Entry entry = byUuid.get(uuid);
            return entry != null && entry.profile == null && ! entry.expired();
        }
    }

    /** Forgets everything cached about {@code uuid}, so the next lookup asks again. */
    public static void invalidate(UUID uuid) {
        synchronized (byUuid) {
            byUuid.remove(uuid);
        }
    }

    /** Records a profile learned elsewhere, such as from a player who just joined. */
    public static void remember(CosmicProfile profile) {
        if (profile != null && profile.getUuid() != null) remember(profile.getUuid(), profile, FOUND_TTL);
    }

    private static void remember(UUID uuid, CosmicProfile profile, Duration ttl) {
        synchronized (byUuid) {
            byUuid.put(uuid, new Entry(profile, ttl));
        }
        if (profile != null && profile.getName() != null) {
            String key = profile.getName().toLowerCase(Locale.ROOT);
            synchronized (byName) {
                byName.put(key, uuid);
            }
            missingNames.remove(key);
        }
    }

    // ---- lookups ------------------------------------------------------------------------------

    /** Looks {@code uuid} up; see {@link #lookup(UUID, String)}. */
    public static CompletableFuture<Optional<CosmicProfile>> lookup(UUID uuid) {
        return lookup(uuid, null);
    }

    /**
     * Looks a player up by UUID. The future never fails: it completes empty when no source
     * knows the player or none can be reached.
     *
     * @param uuid     the player's UUID
     * @param nameHint the player's name, if known; an offline-mode UUID can only be looked up
     *                 through it, and it names the profile when the source returns none
     */
    public static CompletableFuture<Optional<CosmicProfile>> lookup(UUID uuid, String nameHint) {
        if (uuid == null) return CompletableFuture.completedFuture(Optional.empty());

        synchronized (byUuid) {
            Entry entry = byUuid.get(uuid);
            if (entry != null && ! entry.expired()) return CompletableFuture.completedFuture(Optional.ofNullable(entry.profile));
        }
        Optional<CosmicProfile> local = local(uuid);
        if (local.isPresent() && local.get().hasTextures()) {
            remember(uuid, local.get(), LOCAL_TTL);
            return CompletableFuture.completedFuture(local);
        }

        return dedupe("uuid:" + uuid, () -> {
            Optional<CosmicProfile> found = fetchByUuid(uuid, nameHint);
            if (found.isPresent()) {
                CosmicProfile profile = found.get().withNameIfMissing(local.map(CosmicProfile::getName).orElse(nameHint));
                remember(uuid, profile, FOUND_TTL);
                return Optional.of(profile);
            }
            if (local.isPresent()) return local;
            // An offline-mode UUID is only ever looked up through a name, so missing it says
            // nothing about the next lookup, which may bring one.
            if (networkUsable() && ! isOfflineModeUuid(uuid)) remember(uuid, null, MISSING_TTL);
            return Optional.empty();
        });
    }

    /**
     * Looks a player up by name. Names with the Bedrock prefix are looked up as Xbox gamertags.
     * The future never fails; it completes empty when no source knows the name.
     */
    public static CompletableFuture<Optional<CosmicProfile>> lookupByName(String name) {
        if (name == null || name.trim().isEmpty()) return CompletableFuture.completedFuture(Optional.empty());
        String key = name.trim().toLowerCase(Locale.ROOT);

        UUID known;
        synchronized (byName) {
            known = byName.get(key);
        }
        if (known != null) {
            Optional<CosmicProfile> cached = getCached(known);
            if (cached.isPresent()) return CompletableFuture.completedFuture(cached);
        }
        Long missingUntil = missingNames.get(key);
        if (missingUntil != null && missingUntil > System.currentTimeMillis()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return dedupe("name:" + key, () -> {
            Optional<CosmicProfile> found = fetchByName(name.trim());
            if (found.isPresent()) {
                remember(found.get().getUuid(), found.get(), FOUND_TTL);
            } else if (networkUsable()) {
                missingNames.put(key, System.currentTimeMillis() + MISSING_TTL.toMillis());
            }
            return found;
        });
    }

    /** The player's name: cached, from the platform, or looked up. */
    public static CompletableFuture<Optional<String>> getName(UUID uuid) {
        return lookup(uuid).thenApply(profile -> profile.map(CosmicProfile::getName).filter(n -> ! n.isEmpty()));
    }

    /** The UUID of the player with {@code name}. Bedrock names give the Floodgate UUID. */
    public static CompletableFuture<Optional<UUID>> getUuid(String name) {
        return lookupByName(name).thenApply(profile -> profile.map(CosmicProfile::getUuid));
    }

    /** The {@code textures.minecraft.net} URL of the player's skin. */
    public static CompletableFuture<Optional<String>> getSkinUrl(UUID uuid) {
        return lookup(uuid).thenApply(profile -> profile.flatMap(CosmicProfile::getSkinUrl));
    }

    /**
     * Blocks for at most {@code timeout} on {@code future}; empty when it takes longer. For code
     * that must answer synchronously; prefer the futures.
     */
    public static <T> Optional<T> await(CompletableFuture<Optional<T>> future, Duration timeout) {
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // ---- image URLs ---------------------------------------------------------------------------

    /**
     * A face image of the player, from mc-heads.net, for web pages, embeds and webhooks. Uses
     * the skin's texture id when the profile has one, which also works for Bedrock players.
     */
    public static String avatarUrl(CosmicProfile profile, int size) {
        String id = profile.getSkinUrl().map(PlayerLookup::textureId).orElse(null);
        if (id == null) id = profile.getUuid() == null ? profile.getName() : profile.getUuid().toString().replace("-", "");
        return "https://mc-heads.net/avatar/" + id + "/" + Math.max(8, size);
    }

    /** A face image of the player with {@code uuid}, from mc-heads.net. */
    public static String avatarUrl(UUID uuid, int size) {
        return getCached(uuid).map(profile -> avatarUrl(profile, size))
                .orElse("https://mc-heads.net/avatar/" + uuid.toString().replace("-", "") + "/" + Math.max(8, size));
    }

    private static String textureId(String skinUrl) {
        int slash = skinUrl.lastIndexOf('/');
        return slash >= 0 && slash + 1 < skinUrl.length() ? skinUrl.substring(slash + 1) : null;
    }

    // ---- sources ------------------------------------------------------------------------------

    private static Optional<CosmicProfile> local(UUID uuid) {
        try {
            Optional<CosmicProfile> profile = localSource.apply(uuid);
            if (profile != null && profile.isPresent()) return profile;
        } catch (Throwable e) {
            debug("Reading the live profile of " + uuid + " failed: " + e);
        }
        if (FloodgateHook.isFloodgatePlayer(uuid)) {
            Optional<String> name = FloodgateHook.getJavaUsername(uuid);
            if (name.isPresent()) return Optional.of(new CosmicProfile(uuid, name.get(), null, null, true, CosmicProfile.Source.LOCAL));
        }
        return Optional.empty();
    }

    private static Optional<CosmicProfile> fetchByUuid(UUID uuid, String nameHint) {
        if (isBedrock(uuid)) return fetchBedrock(uuid, null);

        if (isOfflineModeUuid(uuid)) {
            // Offline-mode UUIDs exist only on this server; the skin is the premium account of that name.
            if (nameHint == null || ! JAVA_NAME.matcher(nameHint).matches()) return Optional.empty();
            return fetchJava(nameHint).map(premium -> new CosmicProfile(uuid, nameHint,
                    premium.getTexturesValue(), premium.getTexturesSignature(), false, premium.getSource()));
        }

        return fetchJava(uuid.toString().replace("-", ""));
    }

    private static Optional<CosmicProfile> fetchByName(String name) {
        if (isBedrockName(name)) {
            String gamertag = name.substring(getBedrockPrefix().length());
            Optional<JsonObject> xuid = get(GEYSER + "xbox/xuid/" + encode(gamertag));
            if (xuid.isEmpty() || ! xuid.get().has("xuid")) return Optional.empty();
            try {
                return fetchBedrock(bedrockUuid(xuid.get().get("xuid").getAsLong()), gamertag);
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        }
        if (! JAVA_NAME.matcher(name).matches()) return Optional.empty();
        return fetchJava(name);
    }

    private static Optional<CosmicProfile> fetchJava(String id) {
        Optional<JsonObject> playerDb = get(PLAYERDB + encode(id));
        if (playerDb.isPresent()) {
            try {
                JsonObject player = playerDb.get().getAsJsonObject("data").getAsJsonObject("player");
                UUID uuid = UUID.fromString(player.get("id").getAsString());
                String name = player.get("username").getAsString();
                String[] textures = texturesProperty(player.get("properties"));
                return Optional.of(new CosmicProfile(uuid, name, textures[0], textures[1], false, CosmicProfile.Source.PLAYERDB));
            } catch (RuntimeException e) {
                debug("Unreadable playerdb.co answer for " + id + ": " + e);
            }
        }

        Optional<JsonObject> ashcon = get(ASHCON + encode(id));
        if (ashcon.isPresent()) {
            try {
                JsonObject json = ashcon.get();
                UUID uuid = UUID.fromString(json.get("uuid").getAsString());
                String name = json.get("username").getAsString();
                String value = null;
                String signature = null;
                if (json.has("textures") && json.getAsJsonObject("textures").has("raw")) {
                    JsonObject raw = json.getAsJsonObject("textures").getAsJsonObject("raw");
                    value = raw.has("value") ? raw.get("value").getAsString() : null;
                    signature = raw.has("signature") ? raw.get("signature").getAsString() : null;
                }
                return Optional.of(new CosmicProfile(uuid, name, value, signature, false, CosmicProfile.Source.ASHCON));
            } catch (RuntimeException e) {
                debug("Unreadable api.ashcon.app answer for " + id + ": " + e);
            }
        }
        return Optional.empty();
    }

    private static Optional<CosmicProfile> fetchBedrock(UUID uuid, String knownGamertag) {
        long xuid = xuid(uuid);
        String prefix = getBedrockPrefix();

        String name = FloodgateHook.getJavaUsername(uuid).orElse(null);
        if (name == null && knownGamertag != null) name = prefix + knownGamertag;
        if (name == null) {
            Optional<JsonObject> gamertag = get(GEYSER + "xbox/gamertag/" + xuid);
            if (gamertag.isPresent() && gamertag.get().has("gamertag")) name = prefix + gamertag.get().get("gamertag").getAsString();
        }

        String value = null;
        String signature = null;
        Optional<JsonObject> skin = get(GEYSER + "skin/" + xuid);
        if (skin.isPresent()) {
            JsonObject json = skin.get();
            value = json.has("value") && ! json.get("value").isJsonNull() ? json.get("value").getAsString() : null;
            signature = json.has("signature") && ! json.get("signature").isJsonNull() ? json.get("signature").getAsString() : null;
        }

        if (name == null && value == null) return Optional.empty();
        return Optional.of(new CosmicProfile(uuid, name, value, signature, true, CosmicProfile.Source.GEYSER));
    }

    /** The {@code textures} property's value and signature from a profile's {@code properties} array. */
    private static String[] texturesProperty(JsonElement properties) {
        String[] result = new String[2];
        if (properties == null || ! properties.isJsonArray()) return result;
        for (JsonElement element : (JsonArray) properties) {
            if (! element.isJsonObject()) continue;
            JsonObject property = element.getAsJsonObject();
            if (! property.has("name") || ! "textures".equals(property.get("name").getAsString())) continue;
            result[0] = property.has("value") ? property.get("value").getAsString() : null;
            result[1] = property.has("signature") && ! property.get("signature").isJsonNull() ? property.get("signature").getAsString() : null;
        }
        return result;
    }

    // ---- networking ---------------------------------------------------------------------------

    /** Whether lookups may go out right now. */
    public static boolean networkUsable() {
        if (! isEnabled()) return false;
        return System.currentTimeMillis() >= offlineUntil;
    }

    /**
     * Whether lookups are on: {@link #setEnabled}, the core's {@code player-lookups.enabled},
     * and not {@code no-internet}. The config is re-read at most every 30 seconds.
     */
    public static boolean isEnabled() {
        if (! enabled) return false;
        long now = System.currentTimeMillis();
        if (now - configReadAt >= CONFIG_REFRESH_MILLIS) {
            configReadAt = now;
            try {
                if (GivenConfigs.getMainConfig() != null) {
                    configAllows = GivenConfigs.getMainConfig().isPlayerLookupsEnabled()
                            && ! GivenConfigs.getMainConfig().isNoInternet();
                    bedrockPrefix = GivenConfigs.getMainConfig().getPlayerLookupsBedrockPrefix();
                }
            } catch (Throwable ignored) {
                // The core config is not loaded yet; nothing says to stay offline.
            }
        }
        return configAllows;
    }

    private static final long CONFIG_REFRESH_MILLIS = 30_000L;
    private static volatile long configReadAt = 0L;
    private static volatile boolean configAllows = true;

    /**
     * GETs {@code url} as JSON. Empty on any failure: not found, rate limited, unreachable or
     * unreadable. Pauses the host or all lookups as described on the class.
     */
    private static Optional<JsonObject> get(String url) {
        if (! networkUsable()) return Optional.empty();
        String host = URI.create(url).getHost();
        Long paused = hostPausedUntil.get(host);
        if (paused != null && paused > System.currentTimeMillis()) return Optional.empty();

        try {
            HttpResponse<String> response = CLIENT.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 200) {
                hostBackoff.remove(host);
                JsonElement json = new JsonParser().parse(response.body());
                return json.isJsonObject() ? Optional.of(json.getAsJsonObject()) : Optional.empty();
            }
            if (status == 429 || status == 403 || status >= 500) {
                pauseHost(host, response.headers().firstValue("Retry-After").orElse(null));
                debug("Player lookup source " + host + " answered HTTP " + status + "; pausing it.");
            }
            return Optional.empty();
        } catch (HttpConnectTimeoutException | ConnectException | UnknownHostException | NoRouteToHostException e) {
            goOffline(e);
            return Optional.empty();
        } catch (HttpTimeoutException e) {
            pauseHost(host, null);
            return Optional.empty();
        } catch (IOException e) {
            if (e.getCause() instanceof UnknownHostException || e.getCause() instanceof ConnectException) goOffline(e);
            else pauseHost(host, null);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (RuntimeException e) {
            debug("Player lookup at " + url + " failed: " + e);
            return Optional.empty();
        }
    }

    private static void goOffline(Exception cause) {
        boolean wasOnline = System.currentTimeMillis() >= offlineUntil;
        offlineUntil = System.currentTimeMillis() + OFFLINE_PAUSE.toMillis();
        if (wasOnline) {
            debug("Player lookups cannot reach the internet (" + cause + "); pausing them for "
                    + OFFLINE_PAUSE.toMinutes() + " minutes.");
        }
    }

    private static void pauseHost(String host, String retryAfter) {
        long pause;
        try {
            pause = retryAfter == null ? -1L : Long.parseLong(retryAfter.trim()) * 1000L;
        } catch (NumberFormatException e) {
            pause = -1L;
        }
        if (pause <= 0) {
            long previous = hostBackoff.getOrDefault(host, 0L);
            pause = previous <= 0 ? MIN_BACKOFF.toMillis() : Math.min(previous * 2, MAX_BACKOFF.toMillis());
            hostBackoff.put(host, pause);
        }
        hostPausedUntil.put(host, System.currentTimeMillis() + Math.min(pause, MAX_BACKOFF.toMillis()));
    }

    private static CompletableFuture<Optional<CosmicProfile>> dedupe(String key, java.util.function.Supplier<Optional<CosmicProfile>> task) {
        CompletableFuture<Optional<CosmicProfile>> created = new CompletableFuture<>();
        CompletableFuture<Optional<CosmicProfile>> existing = inFlight.putIfAbsent(key, created);
        if (existing != null) return existing;

        try {
            EXECUTOR.execute(() -> {
                Optional<CosmicProfile> result = Optional.empty();
                try {
                    result = task.get();
                } catch (Throwable e) {
                    debug("Player lookup " + key + " failed: " + e);
                } finally {
                    inFlight.remove(key, created);
                    created.complete(result == null ? Optional.empty() : result);
                }
            });
        } catch (RuntimeException e) {
            inFlight.remove(key, created);
            created.complete(Optional.empty());
        }
        return created;
    }

    /** Logs at debug level; logging must never break a lookup, so its own failures are dropped. */
    private static void debug(String message) {
        try {
            MessageUtils.logDebug(message);
        } catch (Throwable ignored) {
            // No logger yet (or none at all); the message is not worth failing over.
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
