package ua.kpi.sc.notification.sse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ua.kpi.sc.notification.dto.NotificationResponse;

/**
 * Registry managing SSE emitter connections per user. Supports multiple
 * concurrent connections per user (multiple tabs/devices).
 *
 * @since 0.5.0
 */
@Slf4j
@Component
public class SseEmitterRegistry {

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(UUID userId, long timeoutMs) {
        var emitter = new SseEmitter(timeoutMs);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

        log.debug("SSE emitter registered for user {}, total connections: {}", userId, getActiveConnectionCount());
        return emitter;
    }

    public void remove(UUID userId, SseEmitter emitter) {
        var userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    public void sendToUser(UUID userId, NotificationResponse notification) {
        var userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException e) {
                log.debug("Failed to send SSE to user {}, removing emitter", userId);
                remove(userId, emitter);
            }
        }
    }

    public void sendHeartbeat() {
        emitters.forEach((userId, userEmitters) -> {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException e) {
                    log.debug("Heartbeat failed for user {}, removing emitter", userId);
                    remove(userId, emitter);
                }
            }
        });
    }

    public int getActiveConnectionCount() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }

    public int getActiveUserCount() {
        return emitters.size();
    }
}
