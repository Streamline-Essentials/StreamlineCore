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

public class UUIDFetcher {
    private static final String API_URL = "https://playerdb.co/api/player/minecraft/%s";

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

    public static String getName(UUID uuid) {
        return getName(uuid.toString());
    }
}