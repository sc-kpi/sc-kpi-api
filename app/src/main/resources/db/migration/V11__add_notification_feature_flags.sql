INSERT INTO feature_flags (key, name, description, enabled, rollout_percentage)
VALUES
    ('notifications.enabled', 'Notifications', 'Enable the notification system', true, 100),
    ('notifications.email', 'Notification Emails', 'Enable email delivery for notifications', true, 100),
    ('notifications.sse', 'Notification SSE', 'Enable real-time SSE streaming for notifications', true, 100),
    ('notifications.admin-broadcast', 'Admin Broadcast', 'Enable admin broadcast notifications', true, 100);
