package ua.kpi.sc.notification.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically sends heartbeat events to all active SSE connections
 * to keep them alive and detect dead emitters.
 *
 * @since 0.5.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {

    private final SseEmitterRegistry registry;

    @Scheduled(fixedRateString = "${app.notifications.sse-heartbeat-interval-ms:30000}")
    public void sendHeartbeats() {
        int connections = registry.getActiveConnectionCount();
        if (connections > 0) {
            log.trace("Sending SSE heartbeat to {} connections", connections);
            registry.sendHeartbeat();
        }
    }
}
