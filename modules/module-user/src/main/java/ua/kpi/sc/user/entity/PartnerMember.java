package ua.kpi.sc.user.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ua.kpi.sc.common.security.PartnerLevel;
import ua.kpi.sc.common.security.PartnerLevelConverter;

/**
 * JPA entity representing a user's membership in a partner organization.
 *
 * @since 0.2.0
 */
@Entity
@Table(name = "partner_members")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Convert(converter = PartnerLevelConverter.class)
    @Column(nullable = false, length = 20)
    private PartnerLevel level;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant assignedAt = Instant.now();

    @Column(name = "assigned_by")
    private UUID assignedBy;
}
