package ua.kpi.sc.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.kpi.sc.user.entity.User;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * @since 0.1.0
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
