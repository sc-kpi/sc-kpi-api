package ua.kpi.sc.common.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationEnumsTest {

    @Test
    void notificationCategory_hasExpectedValues() {
        NotificationCategory[] values = NotificationCategory.values();
        assertThat(values).containsExactlyInAnyOrder(
                NotificationCategory.SECURITY,
                NotificationCategory.ADMIN,
                NotificationCategory.SYSTEM,
                NotificationCategory.FEATURE_FLAG,
                NotificationCategory.RATE_LIMIT
        );
    }

    @Test
    void notificationCategory_valueOfWorks() {
        assertThat(NotificationCategory.valueOf("SECURITY")).isEqualTo(NotificationCategory.SECURITY);
        assertThat(NotificationCategory.valueOf("ADMIN")).isEqualTo(NotificationCategory.ADMIN);
        assertThat(NotificationCategory.valueOf("SYSTEM")).isEqualTo(NotificationCategory.SYSTEM);
        assertThat(NotificationCategory.valueOf("FEATURE_FLAG")).isEqualTo(NotificationCategory.FEATURE_FLAG);
    }

    @Test
    void notificationChannel_hasExpectedValues() {
        NotificationChannel[] values = NotificationChannel.values();
        assertThat(values).containsExactlyInAnyOrder(
                NotificationChannel.IN_APP,
                NotificationChannel.EMAIL
        );
    }

    @Test
    void notificationChannel_valueOfWorks() {
        assertThat(NotificationChannel.valueOf("IN_APP")).isEqualTo(NotificationChannel.IN_APP);
        assertThat(NotificationChannel.valueOf("EMAIL")).isEqualTo(NotificationChannel.EMAIL);
    }
}
