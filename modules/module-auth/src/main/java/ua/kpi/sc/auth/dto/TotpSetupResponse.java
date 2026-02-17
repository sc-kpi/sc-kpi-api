package ua.kpi.sc.auth.dto;

/**
 * Response containing TOTP setup data: QR code and manual entry key.
 *
 * @since 0.6.0
 */
public record TotpSetupResponse(
        String qrCodeDataUri,
        String manualEntryKey
) {
}
