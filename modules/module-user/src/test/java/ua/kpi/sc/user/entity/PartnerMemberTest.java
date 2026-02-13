package ua.kpi.sc.user.entity;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import ua.kpi.sc.common.security.PartnerLevel;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerMemberTest {

    @Test
    void builderCreatesEntity() {
        UUID userId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        UUID assignedBy = UUID.randomUUID();

        PartnerMember member = PartnerMember.builder()
                .userId(userId)
                .partnerId(partnerId)
                .level(PartnerLevel.FULL)
                .assignedBy(assignedBy)
                .build();

        assertThat(member.getUserId()).isEqualTo(userId);
        assertThat(member.getPartnerId()).isEqualTo(partnerId);
        assertThat(member.getLevel()).isEqualTo(PartnerLevel.FULL);
        assertThat(member.getAssignedBy()).isEqualTo(assignedBy);
        assertThat(member.getAssignedAt()).isNotNull();
    }

    @Test
    void noArgsConstructor() {
        PartnerMember member = new PartnerMember();
        member.setUserId(UUID.randomUUID());
        member.setPartnerId(UUID.randomUUID());
        member.setLevel(PartnerLevel.BASIC);

        assertThat(member.getLevel()).isEqualTo(PartnerLevel.BASIC);
    }

    @Test
    void allArgsConstructor() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        Instant assignedAt = Instant.now();
        UUID assignedBy = UUID.randomUUID();

        PartnerMember member = new PartnerMember(id, userId, partnerId, PartnerLevel.DOCUMENTS, assignedAt, assignedBy);

        assertThat(member.getId()).isEqualTo(id);
        assertThat(member.getLevel()).isEqualTo(PartnerLevel.DOCUMENTS);
    }
}
