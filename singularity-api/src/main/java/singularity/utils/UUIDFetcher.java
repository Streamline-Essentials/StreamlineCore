package singularity.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * Utility class for fetching Minecraft player UUIDs and usernames from the
 * <a href="https://playerdb.co">playerdb.co</a> API.
 *
 * <p>All methods perform blocking HTTP requests and return {@code null} when the
 * player cannot be found or any network error occurs.
 */
public class UUIDFetcher {
    /** Base URL template for the playerdb.co Minecraft player API endpoint. */
    private static final String API_URL = "https://playerdb.co/api/player/minecraft/%s";

    /**
     * Looks up the {@link UUID} of a Minecraft player by their username.
     *
     * <p>The lookup is case-insensitive; the username is converted to lower-case
     * before the request is sent to avoid API rejections.
     *
     * @param name the player's Minecraft username
     * @return the player's {@link UUID}, or {@code null} if the player does not
     *         exist or the request fails
     */
    @Nullable
    public static UUID getUUID(@NotNull String name) {
        name = name.toLowerCase(); // Had some issues with upper-case letters in the username, so I added this to make sure that doesn't happen.

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(API_URL, name)).openConnection();

            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            connection.setReadTimeout(5000);

            // These connection parameters need to be set or the API won't accept the connection.

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) response.append(line);

                final JsonElement parsed = new JsonParser().parse(response.toString());

                if (parsed == null || !parsed.isJsonObject()) {
                    return null;
                }

                JsonObject data = parsed.getAsJsonObject(); // Read the returned JSON data.

                return UUID.fromString(
                        data.get("data")
                                .getAsJsonObject()
                                .get("player")
                                .getAsJsonObject()
                                .get("id") // Grab the UUID.
                                .getAsString()
                );
            }
        } catch (Exception ignored) {
            // Ignoring exception since this is usually caused by non-existent usernames.
        }

        return null;
    }

    /**
     * Looks up the current username of a Minecraft player by their UUID string.
     *
     * <p>The UUID is converted to lower-case before the request is sent to
     * avoid API rejections caused by mixed-case input.
     *
     * @param uuid the player's UUID as a string
     * @return the player's current username, or {@code null} if the player does
     *         not exist or the request fails
     */
    @Nullable
    public static String getName(@NotNull String uuid) {
        uuid = uuid.toLowerCase(); // Had some issues with upper-case letters in the username, so I added this to make sure that doesn't happen.

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(API_URL, uuid)).openConnection();

            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            connection.setReadTimeout(5000);

            // These connection parameters need to be set or the API won't accept the connection.

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) response.append(line);

                final JsonElement parsed = new JsonParser().parse(response.toString());

                if (parsed == null || !parsed.isJsonObject()) {
                    return null;
                }

                JsonObject data = parsed.getAsJsonObject(); // Read the returned JSON data.

                return data.get("data")
                        .getAsJsonObject()
                        .get("player")
                        .getAsJsonObject()
                        .get("username") // Grab the UUID.
                        .getAsString();
            }
        } catch (Exception ignored) {
            // Ignoring exception since this is usually caused by non-existent usernames.
        }

        return null;
    }

    /**
     * Looks up the current username of a Minecraft player by their {@link UUID}.
     *
     * <p>Convenience overload that delegates to {@link #getName(String)}.
     *
     * @param uuid the player's {@link UUID}
     * @return the player's current username, or {@code null} if the player does
     *         not exist or the request fails
     */
    public static String getName(UUID uuid) {
        return getName(uuid.toString());
    }
}