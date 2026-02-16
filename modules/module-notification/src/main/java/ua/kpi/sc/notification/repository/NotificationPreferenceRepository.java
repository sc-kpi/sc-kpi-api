package ua.kpi.sc.notification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.kpi.sc.notification.entity.NotificationPreferenceEntity;

/**
 * Spring Data JPA repository for {@link NotificationPreferenceEntity}.
 *
 * @since 0.5.0
 */
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, UUID> {

    List<NotificationPreferenceEntity> findByUserId(UUID userId);

    Optional<NotificationPreferenceEntity> findByUserIdAndCategoryAndChannel(UUID userId, String category, String channel);
}
