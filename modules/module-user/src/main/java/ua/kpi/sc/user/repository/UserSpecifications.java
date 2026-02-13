package ua.kpi.sc.user.repository;

import org.springframework.data.jpa.domain.Specification;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.user.entity.User;

public final class UserSpecifications {

    private UserSpecifications() {}

    public static Specification<User> always() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<User> searchByKeyword(String keyword) {
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern),
                cb.like(cb.lower(root.get("email")), pattern)
        );
    }

    public static Specification<User> hasTier(int tier) {
        return (root, query, cb) -> cb.equal(root.get("capabilityTier"), CapabilityTier.fromLevel(tier));
    }

    public static Specification<User> isActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}
