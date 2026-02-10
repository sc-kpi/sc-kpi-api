package ua.kpi.sc.common.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Access levels for external partner organizations.
 *
 * <p>Both {@link #FULL} and {@link #DOCUMENTS} share effective tier 2
 * ({@link CapabilityTier#INTERNAL}) because differentiation between them
 * happens at the feature level (e.g. which API endpoints are accessible),
 * not at the capability tier level.
 *
 * <ul>
 *   <li>{@link #FULL} — complete partner access to engagement features</li>
 *   <li>{@link #DOCUMENTS} — partner access limited to document-related features</li>
 *   <li>{@link #BASIC} — minimal partner access (tier 1)</li>
 * </ul>
 *
 * @see CapabilityTier
 * @since 0.1.0
 */
@Getter
@RequiredArgsConstructor
public enum PartnerLevel {

    /** Complete partner access to engagement features (effective tier 2). */
    FULL(2, "full"),
    /** Partner access limited to document-related features (effective tier 2). */
    DOCUMENTS(2, "documents"),
    /** Minimal partner access (effective tier 1). */
    BASIC(1, "basic");

    private final int effectiveTier;
    private final String value;
}
