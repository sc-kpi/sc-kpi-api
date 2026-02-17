package ua.kpi.sc.auth.service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

/**
 * Generates QR code images as Base64 data URIs for TOTP enrollment.
 *
 * @since 0.6.0
 */
@Service
public class QrCodeService {

    private static final int QR_WIDTH = 250;
    private static final int QR_HEIGHT = 250;

    /**
     * Generates a QR code as a data URI from an otpauth:// URI.
     *
     * @param otpauthUri the TOTP URI (e.g. otpauth://totp/SC-KPI:user@email.com?secret=...)
     * @return a data:image/png;base64,... string
     */
    public String generateDataUri(String otpauthUri) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(otpauthUri, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);

            String base64 = Base64.getEncoder().encodeToString(out.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate QR code", e);
        }
    }
}
