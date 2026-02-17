package ua.kpi.sc.ratelimit.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.kpi.sc.ratelimit.entity.RateLimitRule;

public interface RateLimitRuleRepository extends JpaRepository<RateLimitRule, UUID> {

    boolean existsByName(String name);

    List<RateLimitRule> findByEnabledTrue();
}
