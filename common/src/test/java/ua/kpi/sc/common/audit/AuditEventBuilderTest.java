package ua.kpi.sc.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserPrincipal;

class AuditEventBuilderTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void buildsEventWithAllFields() {
        UUID actorId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        Instant ts = Instant.now();

        AuditEvent event = AuditEventBuilder.builder()
                .actorId(actorId)
                .actorEmail("admin@kpi.ua")
                .action(AuditAction.CREATED)
                .entityType(AuditEntityType.USER)
                .entityId(entityId)
                .entityName("test@kpi.ua")
                .fieldName("tier")
                .oldValue("1")
                .newValue("5")
                .details("Admin promotion")
                .sourceModule("user")
                .ipAddress("10.0.0.1")
                .timestamp(ts)
                .build();

        assertThat(event.actorId()).isEqualTo(actorId);
        assertThat(event.actorEmail()).isEqualTo("admin@kpi.ua");
        assertThat(event.action()).isEqualTo("CREATED");
        assertThat(event.entityType()).isEqualTo("USER");
        assertThat(event.entityId()).isEqualTo(entityId);
        assertThat(event.entityName()).isEqualTo("test@kpi.ua");
        assertThat(event.fieldName()).isEqualTo("tier");
        assertThat(event.oldValue()).isEqualTo("1");
        assertThat(event.newValue()).isEqualTo("5");
        assertThat(event.details()).isEqualTo("Admin promotion");
        assertThat(event.sourceModule()).isEqualTo("user");
        assertThat(event.ipAddress()).isEqualTo("10.0.0.1");
        assertThat(event.timestamp()).isEqualTo(ts);
    }

    @Test
    void autoPopulatesActorFromSecurityContext() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder()
                .id(userId).email("ctx@kpi.ua").tier(CapabilityTier.ADMIN).active(true).build();

        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        AuditEvent event = AuditEventBuilder.builder()
                .action(AuditAction.LOGIN)
                .entityType(AuditEntityType.AUTH)
                .sourceModule("auth")
                .build();

        assertThat(event.actorId()).isEqualTo(userId);
        assertThat(event.actorEmail()).isEqualTo("ctx@kpi.ua");
    }

    @Test
    void actorMethodOverridesSecurityContext() {
        // Set up security context with one user
        UUID ctxUserId = UUID.randomUUID();
        UserPrincipal ctxPrincipal = UserPrincipal.builder()
                .id(ctxUserId).email("ctx@kpi.ua").tier(CapabilityTier.BASIC).active(true).build();
        var auth = new UsernamePasswordAuthenticationToken(ctxPrincipal, null, ctxPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Override with explicit actor
        UUID explicitId = UUID.randomUUID();
        UserPrincipal explicitPrincipal = UserPrincipal.builder()
                .id(explicitId).email("explicit@kpi.ua").tier(CapabilityTier.ADMIN).active(true).build();

        AuditEvent event = AuditEventBuilder.builder()
                .actor(explicitPrincipal)
                .action(AuditAction.CREATED)
                .entityType(AuditEntityType.USER)
                .sourceModule("user")
                .build();

        assertThat(event.actorId()).isEqualTo(explicitId);
        assertThat(event.actorEmail()).isEqualTo("explicit@kpi.ua");
    }

    @Test
    void worksWithNoSecurityContext() {
        SecurityContextHolder.clearContext();

        AuditEvent event = AuditEventBuilder.builder()
                .action(AuditAction.LOGIN_FAILED)
                .entityType(AuditEntityType.AUTH)
                .sourceModule("auth")
                .entityName("unknown@kpi.ua")
                .build();

        assertThat(event.actorId()).isNull();
        assertThat(event.actorEmail()).isNull();
        assertThat(event.action()).isEqualTo("LOGIN_FAILED");
    }

    @Test
    void actorMethodHandlesNull() {
        AuditEvent event = AuditEventBuilder.builder()
                .actor(null)
                .action(AuditAction.LOGIN_FAILED)
                .entityType(AuditEntityType.AUTH)
                .sourceModule("auth")
                .build();

        assertThat(event.actorId()).isNull();
        assertThat(event.actorEmail()).isNull();
    }
}
