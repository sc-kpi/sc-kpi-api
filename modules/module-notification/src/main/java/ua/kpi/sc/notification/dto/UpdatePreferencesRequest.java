package ua.kpi.sc.notification.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for batch-updating notification preferences.
 *
 * @since 0.5.0
 */
public record UpdatePreferencesRequest(
        @Valid List<PreferenceUpdate> preferences
) {

    /**
     * A single preference update entry.
     *
     * @since 0.5.0
     */
    public record PreferenceUpdate(
            @NotBlank String category,
            @NotBlank String channel,
            boolean enabled
    ) {
    }
}
