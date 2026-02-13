package ua.kpi.sc.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails asynchronously.
 *
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Sends a password-reset email with the given reset link.
     *
     * @param to        recipient email address
     * @param resetLink the full URL the user should visit to reset their password
     */
    @Async
    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Password Reset — SC KPI");
            helper.setText(buildPasswordResetHtml(resetLink), true);
            mailSender.send(message);
            log.info("Password reset email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}", to, e);
        }
    }

    private String buildPasswordResetHtml(String resetLink) {
        return """
                <html>
                <body style="font-family: sans-serif; padding: 20px;">
                  <h2>Password Reset</h2>
                  <p>You requested a password reset for your SC KPI account.</p>
                  <p>Click the link below to set a new password. This link expires in 1 hour.</p>
                  <p><a href="%s" style="display:inline-block;padding:10px 20px;\
                background:#18181b;color:#fff;border-radius:6px;text-decoration:none;">Reset Password</a></p>
                  <p>If you did not request this, please ignore this email.</p>
                </body>
                </html>
                """.formatted(resetLink);
    }
}
