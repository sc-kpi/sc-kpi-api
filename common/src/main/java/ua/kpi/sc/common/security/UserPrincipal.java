package ua.kpi.sc.common.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Application-specific {@link UserDetails} implementation that carries the user's
 * identity and {@link CapabilityTier}.
 *
 * <p>The granted authority is a single {@code TIER_N} value derived from the user's
 * capability tier level (e.g. {@code TIER_5} for {@link CapabilityTier#ADMIN}).
 * Account locking and enabling are both governed by the {@code active} flag.
 *
 * @see CapabilityTier
 * @since 0.1.0
 */
@Getter
@Builder
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    @Builder.Default
    private final CapabilityTier tier = CapabilityTier.GUEST;
    private final boolean active;
    @Builder.Default
    private final Map<UUID, PartnerLevel> partnerRoles = Map.of();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("TIER_" + tier.getLevel()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
