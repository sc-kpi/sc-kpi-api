package ua.kpi.sc.audit.actuator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import ua.kpi.sc.audit.repository.AuditEventRepository;

@Component
@RequiredArgsConstructor
public class AuditInfoContributor implements InfoContributor {

    private final AuditEventRepository repository;

    @Override
    public void contribute(Info.Builder builder) {
        long totalEvents = repository.count();
        long eventsLast24h = repository.countByCreatedAtAfter(Instant.now().minus(24, ChronoUnit.HOURS));
        Instant latestEvent = repository.findLatestEventTimestamp();

        builder.withDetail("audit", Map.of(
                "totalEvents", totalEvents,
                "eventsLast24h", eventsLast24h,
                "latestEvent", latestEvent != null ? latestEvent.toString() : "none"
        ));
    }
}
