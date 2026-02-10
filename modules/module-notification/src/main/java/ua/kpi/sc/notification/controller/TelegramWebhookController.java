package ua.kpi.sc.notification.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for receiving Telegram bot webhook callbacks.
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/v1/webhooks/telegram")
@Tag(name = "Telegram Webhook", description = "Telegram bot webhook")
public class TelegramWebhookController {
}
