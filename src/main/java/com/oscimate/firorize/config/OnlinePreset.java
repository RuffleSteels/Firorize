package com.oscimate.firorize.config;

import com.google.gson.annotations.SerializedName;
import net.minecraft.network.chat.Component;

/**
 * One published preset as returned by the online Worker (see {@link OnlinePresetsClient}).
 * {@code data} is the Base64 serialized-profile code — the exact same string produced by
 * {@link ChangeFireColorScreen#serializeToString} and consumed by
 * {@link AddProfileScreen#deserializeFromString}. Fields are populated by Gson from the
 * Worker's JSON, so names match the D1 columns ({@code date_created} via {@link SerializedName}).
 */
public record OnlinePreset(
        int id,
        String title,
        String description,
        @SerializedName("date_created") String dateCreated,
        String author,
        String recipient,
        String data
) {
    public String displayTitle() {
        return title == null || title.isBlank() ? "Untitled" : title;
    }

    public String displayDescription() {
        return description == null ? "" : description;
    }

    public String displayAuthor() {
        return author == null || author.isBlank() ? "?" : author;
    }

    /** Only populated by {@code GET /sent}: the username this preset was sent to. */
    public String displayRecipient() {
        return recipient == null || recipient.isBlank() ? "?" : recipient;
    }

    /** Human-friendly "3 days ago" string derived from the UTC {@code date_created}. */
    public Component relativeTime() {
        return RelativeTime.format(dateCreated);
    }
}
