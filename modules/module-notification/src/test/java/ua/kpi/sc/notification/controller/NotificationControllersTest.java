package ua.kpi.sc.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationControllersTest {

    @Test
    void canInstantiateNotificationSettingsController() {
        assertThat(new NotificationSettingsController()).isNotNull();
    }

    @Test
    void canInstantiateTelegramWebhookController() {
        assertThat(new TelegramWebhookController()).isNotNull();
    }
}
