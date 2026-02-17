package ua.kpi.sc.auth.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
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

/**
 * JPA entity representing an encrypted TOTP secret for two-factor authentication.
 *
 * @since 0.6.0
 */
@Entity
@Table(name = "totp_secrets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotpSecret {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "encrypted_secret", nullable = false)
    private String encryptedSecret;

    @Column(nullable = false)
    @Builder.Default
    private String algorithm = "SHA1";

    @Column(nullable = false)
    @Builder.Default
    private int digits = 6;

    @Column(nullable = false)
    @Builder.Default
    private int period = 30;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
