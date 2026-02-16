package ua.kpi.sc.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import ua.kpi.sc.common.notification.NotificationCategory;
import ua.kpi.sc.common.notification.NotificationEvent;

@ExtendWith(MockitoExtension.class)
class NotificationEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationEmailService service;

    private void initFields() {
        ReflectionTestUtils.setField(service, "maxRetries", 3);
        ReflectionTestUtils.setField(service, "fromAddress", "test@kpi.ua");
    }

    @Nested
    class SendNotificationEmail {

        @Test
        void sendsMessage_onFirstAttempt() {
            initFields();

            var event = new NotificationEvent("title", "body", null,
                    NotificationCategory.SECURITY, "auth", null, null, null);
            service.sendNotificationEmail("user@kpi.ua", event);

            verify(mailSender).send(any(SimpleMailMessage.class));
        }

        @Test
        void retriesOnFailure_untilExhausted() {
            initFields();
            doThrow(new MailSendException("fail")).when(mailSender).send(any(SimpleMailMessage.class));

            var event = new NotificationEvent("title", "body", null,
                    NotificationCategory.SECURITY, "auth", null, null, null);
            service.sendNotificationEmail("user@kpi.ua", event);

            verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
        }

        @Test
        void succeedsOnSecondAttempt() {
            initFields();
            doThrow(new MailSendException("fail"))
                    .doNothing()
                    .when(mailSender).send(any(SimpleMailMessage.class));

            var event = new NotificationEvent("title", "body", null,
                    NotificationCategory.SECURITY, "auth", null, null, null);
            service.sendNotificationEmail("user@kpi.ua", event);

            verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        }

        @Test
        void sendsMessage_withBodyArgs() {
            initFields();

            var event = new NotificationEvent("title", "body",
                    new String[]{"arg1", "arg2"}, NotificationCategory.ADMIN, "user",
                    null, null, null);
            service.sendNotificationEmail("user@kpi.ua", event);

            verify(mailSender).send(any(SimpleMailMessage.class));
        }

        @Test
        void sendsMessage_withEmptyBodyArgs() {
            initFields();

            var event = new NotificationEvent("title", "body",
                    new String[]{}, NotificationCategory.SYSTEM, "notification",
                    null, null, null);
            service.sendNotificationEmail("user@kpi.ua", event);

            verify(mailSender).send(any(SimpleMailMessage.class));
        }

        @Test
        void sendsMessage_withSingleRetry() {
            ReflectionTestUtils.setField(service, "maxRetries", 1);
            ReflectionTestUtils.setField(service, "fromAddress", "test@kpi.ua");
            doThrow(new MailSendException("fail")).when(mailSender).send(any(SimpleMailMessage.class));

            var event = new NotificationEvent("title", "body", null,
                    NotificationCategory.SECURITY, "auth", null, null, null);
            service.sendNotificationEmail("user@kpi.ua", event);

            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }
    }
}
