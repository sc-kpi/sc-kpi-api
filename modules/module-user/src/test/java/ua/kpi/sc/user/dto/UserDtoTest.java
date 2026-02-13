package ua.kpi.sc.user.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserDtoTest {

    @Test
    void userResponse_recordAccessors() {
        UUID id = UUID.randomUUID();
        var partnerRole = new PartnerMemberResponse(UUID.randomUUID(), "full", Instant.now());
        var response = new UserResponse(id, "e@kpi.ua", "F", "L", 3, true, Instant.now(), List.of(partnerRole));

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.email()).isEqualTo("e@kpi.ua");
        assertThat(response.firstName()).isEqualTo("F");
        assertThat(response.lastName()).isEqualTo("L");
        assertThat(response.capabilityTier()).isEqualTo(3);
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.partnerRoles()).hasSize(1);
    }

    @Test
    void userListResponse_recordAccessors() {
        var response = new UserListResponse(UUID.randomUUID(), "e@kpi.ua", "F", "L", 1, true);

        assertThat(response.email()).isEqualTo("e@kpi.ua");
        assertThat(response.active()).isTrue();
    }

    @Test
    void createUserRequest_recordAccessors() {
        var request = new CreateUserRequest("e@kpi.ua", "password", "F", "L", 2);

        assertThat(request.email()).isEqualTo("e@kpi.ua");
        assertThat(request.tier()).isEqualTo(2);
    }

    @Test
    void updateUserRequest_recordAccessors() {
        var request = new UpdateUserRequest("F", "L");

        assertThat(request.firstName()).isEqualTo("F");
        assertThat(request.lastName()).isEqualTo("L");
    }

    @Test
    void updateTierRequest_recordAccessors() {
        var request = new UpdateTierRequest(3);
        assertThat(request.tier()).isEqualTo(3);
    }

    @Test
    void updateStatusRequest_recordAccessors() {
        var request = new UpdateStatusRequest(false);
        assertThat(request.active()).isFalse();
    }

    @Test
    void assignPartnerLevelRequest_recordAccessors() {
        UUID partnerId = UUID.randomUUID();
        var request = new AssignPartnerLevelRequest(partnerId, "documents");

        assertThat(request.partnerId()).isEqualTo(partnerId);
        assertThat(request.level()).isEqualTo("documents");
    }

    @Test
    void partnerMemberResponse_recordAccessors() {
        UUID partnerId = UUID.randomUUID();
        Instant assignedAt = Instant.now();
        var response = new PartnerMemberResponse(partnerId, "basic", assignedAt);

        assertThat(response.partnerId()).isEqualTo(partnerId);
        assertThat(response.level()).isEqualTo("basic");
        assertThat(response.assignedAt()).isEqualTo(assignedAt);
    }
}
