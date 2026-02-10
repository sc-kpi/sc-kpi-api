package ua.kpi.sc.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserPrincipalTest {

    @Test
    void getAuthorities_returnsTierAuthority() {
        var principal = UserPrincipal.builder()
                .id(UUID.randomUUID())
                .email("user@kpi.ua")
                .password("secret")
                .tier(CapabilityTier.ADMIN)
                .active(true)
                .build();

        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly("TIER_5");
    }

    @Test
    void getUsername_returnsEmail() {
        var principal = UserPrincipal.builder()
                .email("user@kpi.ua")
                .tier(CapabilityTier.GUEST)
                .build();

        assertThat(principal.getUsername()).isEqualTo("user@kpi.ua");
    }

    @Test
    void isAccountNonExpired_alwaysTrue() {
        var principal = UserPrincipal.builder().tier(CapabilityTier.GUEST).build();
        assertThat(principal.isAccountNonExpired()).isTrue();
    }

    @Test
    void isCredentialsNonExpired_alwaysTrue() {
        var principal = UserPrincipal.builder().tier(CapabilityTier.GUEST).build();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void isAccountNonLocked_reflectsActiveField_whenTrue() {
        var principal = UserPrincipal.builder().tier(CapabilityTier.GUEST).active(true).build();
        assertThat(principal.isAccountNonLocked()).isTrue();
    }

    @Test
    void isAccountNonLocked_reflectsActiveField_whenFalse() {
        var principal = UserPrincipal.builder().tier(CapabilityTier.GUEST).active(false).build();
        assertThat(principal.isAccountNonLocked()).isFalse();
    }

    @Test
    void isEnabled_reflectsActiveField_whenTrue() {
        var principal = UserPrincipal.builder().tier(CapabilityTier.GUEST).active(true).build();
        assertThat(principal.isEnabled()).isTrue();
    }

    @Test
    void isEnabled_reflectsActiveField_whenFalse() {
        var principal = UserPrincipal.builder().tier(CapabilityTier.GUEST).active(false).build();
        assertThat(principal.isEnabled()).isFalse();
    }

    @Test
    void builder_withoutTier_defaultsToGuest() {
        var principal = UserPrincipal.builder()
                .email("user@kpi.ua")
                .build();

        assertThat(principal.getTier()).isEqualTo(CapabilityTier.GUEST);
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly("TIER_0");
    }
}
