package ua.kpi.sc.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ua.kpi.sc.common.notification.NotificationEvent;

/**
 * Async email delivery service for notifications.
 *
 * @since 0.5.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.notifications.email-max-retries:3}")
    private int maxRetries;

    @Value("${spring.mail.username:noreply@sc.kpi.ua}")
    private String fromAddress;

    @Async
    public void sendNotificationEmail(String toEmail, NotificationEvent event) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                var message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(toEmail);
                message.setSubject("[SC-KPI] " + event.titleKey());
                message.setText(formatBody(event));
                mailSender.send(message);
                log.debug("Notification email sent to {}: titleKey={}", toEmail, event.titleKey());
                return;
            } catch (Exception e) {
                log.warn("Failed to send notification email (attempt {}/{}): to={}, titleKey={}",
                        attempt, maxRetries, toEmail, event.titleKey(), e);
            }
        }
        log.error("Exhausted retries sending notification email: to={}, titleKey={}", toEmail, event.titleKey());
    }

    private String formatBody(NotificationEvent event) {
        var sb = new StringBuilder();
        sb.append("Notification: ").append(event.titleKey()).append("\n\n");
        sb.append("Category: ").append(event.category().name()).append("\n");
        sb.append("Module: ").append(event.sourceModule()).append("\n");
        if (event.bodyArgs() != null && event.bodyArgs().length > 0) {
            sb.append("Details: ").append(String.join(", ", event.bodyArgs())).append("\n");
        }
        return sb.toString();
    }
}
