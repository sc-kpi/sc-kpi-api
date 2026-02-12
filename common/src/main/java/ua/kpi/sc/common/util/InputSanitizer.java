package ua.kpi.sc.common.util;

import java.util.regex.Pattern;

/**
 * Defense-in-depth input sanitizer for stripping HTML tags.
 *
 * @since 0.1.0
 */
public final class InputSanitizer {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    private InputSanitizer() {}

    /**
     * Strips all HTML tags from the input string.
     *
     * @param input the raw input
     * @return sanitized string with HTML tags removed, or null if input is null
     */
    public static String stripHtml(String input) {
        if (input == null) {
            return null;
        }
        return HTML_TAG_PATTERN.matcher(input).replaceAll("");
    }
}
