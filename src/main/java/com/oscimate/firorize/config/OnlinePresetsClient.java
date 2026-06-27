package com.oscimate.firorize.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Talks to the Firorize preset Worker (a small Cloudflare Worker that fronts the D1 database — see
 * {@code cloudflare-worker/}). The Worker holds the D1 credentials; this client only knows the
 * public Worker URL, so no secret ships in the mod jar.
 *
 * <p>Uploads/deletes are attributed to the player by the UUID + name from their Minecraft session
 * (sent as headers). The Worker stores only a salted hash of the UUID, which is what powers "My
 * Uploads" and self-delete. This identifies who you are without sending any Minecraft access token
 * anywhere.
 *
 * <p>All requests are sent asynchronously off the render thread; callers must marshal the result
 * back onto the client thread (via {@code MinecraftClient.getInstance().execute(...)}) before
 * touching screen state.
 */
public final class OnlinePresetsClient {
    private OnlinePresetsClient() {}

    /**
     * Base URL of the deployed Worker, with NO trailing slash. Replace with your own
     * {@code https://<name>.<account>.workers.dev} (or custom domain) after deploying the Worker.
     */
    public static final String WORKER_URL = "https://firorize-presets.raffers-smith.workers.dev";

    /** Public privacy-policy page served by the same Worker. */
    public static final String PRIVACY_URL = WORKER_URL + "/privacy";

    public static final Logger LOGGER = LoggerFactory.getLogger("firorize-presets");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson GSON = new Gson();

    /** The player's identity (from their Minecraft session), sent as headers on authored requests. */
    public record McAuth(String uuid, String name) {}

    /** Outcome of a write; {@code error} carries the Worker's error code when {@code success} is false. */
    public record ApiResult(boolean success, String error) {}

    /** Reads the local player's identity from their Minecraft session, or null if unavailable. */
    public static McAuth currentIdentity() {
        Session session = MinecraftClient.getInstance().getSession();
        UUID uuid = session.getUuidOrNull();
        String name = session.getUsername();
        if (uuid == null || name == null || name.isBlank()) return null;
        return new McAuth(uuid.toString(), name);
    }

    /** Cached count of items in the player's inbox, for the config-screen notification badge. */
    private static volatile int inboxCount = 0;

    public static int inboxCount() {
        return inboxCount;
    }

    /** Fires a background fetch of the inbox and updates {@link #inboxCount()} (0 when not signed in). */
    public static void refreshInboxCount() {
        McAuth auth = currentIdentity();
        if (auth == null) {
            inboxCount = 0;
            return;
        }
        fetchInbox(auth).whenComplete((list, err) -> {
            if (err == null && list != null) inboxCount = list.size();
        });
    }

    /** GET /presets → newest-first list of published presets (public, no identity needed). */
    public static CompletableFuture<List<OnlinePreset>> fetchPresets() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WORKER_URL + "/presets"))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(OnlinePresetsClient::parseList);
    }

    /** GET /mine → the player's own uploads. */
    public static CompletableFuture<List<OnlinePreset>> fetchMine(McAuth auth) {
        HttpRequest request = authored(HttpRequest.newBuilder()
                .uri(URI.create(WORKER_URL + "/mine"))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET(), auth).build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(OnlinePresetsClient::parseList);
    }

    /**
     * POST /presets — publishes a preset. {@code data} is the Base64 serialized-profile code;
     * {@code title}/{@code description} are user metadata. Resolves to an {@link ApiResult} —
     * {@code success=false} with the Worker's error code (e.g. "profanity", "invalid_title") on rejection.
     */
    public static CompletableFuture<ApiResult> upload(McAuth auth, String data, String title, String description) {
        JsonObject body = new JsonObject();
        body.addProperty("data", data);
        body.addProperty("title", title);
        body.addProperty("description", description);

        HttpRequest request = authored(HttpRequest.newBuilder()
                .uri(URI.create(WORKER_URL + "/presets"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8)), auth).build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(OnlinePresetsClient::parseResult);
    }

    /** DELETE /presets/{id} — deletes one of the player's own uploads. */
    public static CompletableFuture<ApiResult> deletePreset(McAuth auth, int id) {
        HttpRequest request = authored(HttpRequest.newBuilder()
                .uri(URI.create(WORKER_URL + "/presets/" + id))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.noBody()), auth).build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(OnlinePresetsClient::parseResult);
    }

    /** GET /inbox → presets that were privately sent to the player (author = sender). */
    public static CompletableFuture<List<OnlinePreset>> fetchInbox(McAuth auth) {
        HttpRequest request = authored(HttpRequest.newBuilder()
                .uri(URI.create(WORKER_URL + "/inbox"))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET(), auth).build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(OnlinePresetsClient::parseList);
    }

    /** GET /sent → the player's own outgoing (pending) sends (each carries its {@code recipient}). */
    public static CompletableFuture<List<OnlinePreset>> fetchSent(McAuth auth) {
        HttpRequest request = authored(HttpRequest.newBuilder()
                .uri(URI.create(WORKER_URL + "/sent"))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET(), auth).build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(OnlinePresetsClient::parseList);
    }

    /**
     * POST /sends — privately sends a profile to one or more Minecraft usernames. {@code data} is the
     * Base64 serialized-profile code; one copy is delivered to each recipient's inbox. Resolves to an
     * {@link ApiResult} carrying the Worker's error code (e.g. "invalid_username", "no_recipients") on rejection.
     */
    public static CompletableFuture<ApiResult> share(McAuth auth, String data, String title, String description, List<String> recipients) {
        JsonObject body = new JsonObject();
        body.addProperty("data", data);
        body.addProperty("title", title);
        body.addProperty("description", description);
        JsonArray recipientArray = new JsonArray();
        for (String r : recipients) recipientArray.add(r);
        body.add("recipients", recipientArray);

        HttpRequest request = authored(HttpRequest.newBuilder()
                .uri(URI.create(WORKER_URL + "/sends"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8)), auth).build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(OnlinePresetsClient::parseResult);
    }

    /** DELETE /sends/{id} — cancels one of the player's outgoing sends, or dismisses one from their inbox. */
    public static CompletableFuture<ApiResult> deleteSend(McAuth auth, int id) {
        HttpRequest request = authored(HttpRequest.newBuilder()
                .uri(URI.create(WORKER_URL + "/sends/" + id))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.noBody()), auth).build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(OnlinePresetsClient::parseResult);
    }

    private static HttpRequest.Builder authored(HttpRequest.Builder builder, McAuth auth) {
        return builder.header("X-MC-Uuid", auth.uuid()).header("X-MC-Name", auth.name());
    }

    private static List<OnlinePreset> parseList(HttpResponse<String> response) {
        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("Server returned HTTP " + response.statusCode());
        }
        OnlinePreset[] presets = GSON.fromJson(response.body(), OnlinePreset[].class);
        return presets == null ? List.of() : Arrays.asList(presets);
    }

    private static ApiResult parseResult(HttpResponse<String> response) {
        if (response.statusCode() / 100 == 2) {
            return new ApiResult(true, null);
        }
        String error = "server";
        try {
            JsonObject obj = GSON.fromJson(response.body(), JsonObject.class);
            if (obj != null && obj.has("error") && !obj.get("error").isJsonNull()) {
                error = obj.get("error").getAsString();
            }
        } catch (RuntimeException ignored) {
            // non-JSON error body; keep the generic "server" code
        }
        return new ApiResult(false, error);
    }
}
