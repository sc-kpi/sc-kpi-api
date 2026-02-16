package ua.kpi.sc.notification.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import ua.kpi.sc.notification.entity.NotificationEntity;

/**
 * Reusable JPA {@link Specification} builders for notification queries.
 *
 * @since 0.5.0
 */
public final class NotificationSpecification {

    private NotificationSpecification() {}

    public static Specification<NotificationEntity> always() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<NotificationEntity> hasUserId(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<NotificationEntity> hasCategory(String category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<NotificationEntity> isRead(boolean read) {
        return (root, query, cb) -> cb.equal(root.get("read"), read);
    }

    public static Specification<NotificationEntity> createdAfter(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<NotificationEntity> createdBefore(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<NotificationEntity> searchByText(String search) {
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("titleKey")), pattern),
                cb.like(cb.lower(root.get("bodyKey")), pattern)
        );
    }
}
