package ua.kpi.sc.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.kpi.sc.user.entity.PartnerMember;

/**
 * Spring Data JPA repository for {@link PartnerMember} entities.
 *
 * @since 0.2.0
 */
public interface PartnerMemberRepository extends JpaRepository<PartnerMember, UUID> {

    List<PartnerMember> findByUserId(UUID userId);

    Optional<PartnerMember> findByUserIdAndPartnerId(UUID userId, UUID partnerId);

    List<PartnerMember> findByPartnerId(UUID partnerId);

    void deleteByUserIdAndPartnerId(UUID userId, UUID partnerId);
}
