package singularity.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class UUIDFetcher {
    private static final String API_URL = "https://playerdb.co/api/player/minecraft/";

    @Nullable
    public static UUID getUUID(@NotNull String name) {
        name = name.toLowerCase(); // Had some issues with upper-case letters in the username, so I added this to make sure that doesn't happen.

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + name))
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    MessageUtils.logInfo("Failed to fetch UUID for username: " + name + ". HTTP status code: " + response.statusCode());
                    return null; // If the response is not OK, return null.
                }

                JsonElement parsed = JsonParser.parseString(response.body());
                if (parsed == null || !parsed.isJsonObject()) {
                    MessageUtils.logInfo("Failed to parse JSON response for name: " + name);
                    return null;
                }

                JsonObject data = parsed.getAsJsonObject();
                String uuid = data.get("data")
                        .getAsJsonObject()
                        .get("player")
                        .getAsJsonObject()
                        .get("username")
                        .getAsString();

                return UUID.fromString(uuid);
            } catch (IOException | InterruptedException e) {
                MessageUtils.logInfo("Error fetching UUID for username: " + name + " - " + e.getMessage());
                return null;
            }
        } catch (Exception ignored) {
            // Ignoring exception since this is usually caused by non-existent usernames.
        }

        MessageUtils.logInfo("Failed to get UUID for username: " + name);
        return null;
    }

    @Nullable
    public static String getName(@NotNull String uuid) {
        uuid = uuid.toLowerCase(); // Had some issues with upper-case letters in the username, so I added this to make sure that doesn't happen.

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + uuid))
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    MessageUtils.logInfo("Failed to fetch username for UUID: " + uuid + ". HTTP status code: " + response.statusCode());
                    return null; // If the response is not OK, return null.
                }

                JsonElement parsed = JsonParser.parseString(response.body());
                if (parsed == null || !parsed.isJsonObject()) {
                    MessageUtils.logInfo("Failed to parse JSON response for UUID: " + uuid);
                    return null;
                }

                JsonObject data = parsed.getAsJsonObject();
                return data.get("data")
                        .getAsJsonObject()
                        .get("player")
                        .getAsJsonObject()
                        .get("username")
                        .getAsString();
            } catch (IOException | InterruptedException e) {
                MessageUtils.logInfo("Error fetching username for UUID: " + uuid + " - " + e.getMessage());
                return null;
            }
        } catch (Exception ignored) {
            // Ignoring exception since this is usually caused by non-existent usernames.
        }

        MessageUtils.logInfo("Failed to get name for UUID: " + uuid);
        return null;
    }

    public static String getName(UUID uuid) {
        return getName(uuid.toString());
    }
}