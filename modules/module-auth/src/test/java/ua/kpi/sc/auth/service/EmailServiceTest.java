package ua.kpi.sc.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @Nested
    class SendPasswordResetEmailTests {

        @Test
        void sendsEmailViaMailSender() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            emailService.sendPasswordResetEmail("user@kpi.ua", "http://localhost:3000/reset-password?token=abc123");

            verify(mailSender).send(mimeMessage);
        }

        @Test
        void handlesMessagingExceptionGracefully() throws MessagingException {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MessagingException("Failed to set content"))
                    .when(mimeMessage).setContent(any(), any());

            assertThatCode(() -> emailService.sendPasswordResetEmail("user@kpi.ua", "http://localhost:3000/reset-password?token=abc123"))
                    .doesNotThrowAnyException();
        }
    }
}
