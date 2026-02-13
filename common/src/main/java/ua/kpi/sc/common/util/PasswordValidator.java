package ua.kpi.sc.common.util;

import ua.kpi.sc.common.exception.BadRequestException;

/**
 * Validates password length against BCrypt's 72-byte limit (CVE-2025-22228).
 *
 * @since 0.2.0
 */
public final class PasswordValidator {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    private PasswordValidator() {}

    /**
     * Validates that the password length is within BCrypt-safe bounds.
     *
     * @param password the raw password to validate
     * @throws BadRequestException if the password is null, too short, or too long
     */
    public static void validateLength(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new BadRequestException("Password must be between 8 and 72 characters");
        }
    }
}
