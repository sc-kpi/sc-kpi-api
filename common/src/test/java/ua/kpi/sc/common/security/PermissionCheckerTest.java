package ua.kpi.sc.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PermissionCheckerTest {

    private PermissionChecker checker;

    @BeforeEach
    void setUp() {
        checker = new PermissionChecker();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hasTier_noAuthentication_returnsFalse() {
        assertThat(checker.hasTier(0)).isFalse();
    }

    @Test
    void hasTier_nonUserPrincipal_returnsFalse() {
        var auth = new UsernamePasswordAuthenticationToken("stringPrincipal", "password");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(checker.hasTier(0)).isFalse();
    }

    @Test
    void hasTier_sufficientTier_returnsTrue() {
        setAuthentication(CapabilityTier.ADMIN);
        assertThat(checker.hasTier(CapabilityTier.BASIC.getLevel())).isTrue();
    }

    @Test
    void hasTier_insufficientTier_returnsFalse() {
        setAuthentication(CapabilityTier.GUEST);
        assertThat(checker.hasTier(CapabilityTier.ADMIN.getLevel())).isFalse();
    }

    @Test
    void hasTierEnum_sufficientTier_returnsTrue() {
        setAuthentication(CapabilityTier.ADMIN);
        assertThat(checker.hasTier(CapabilityTier.BASIC)).isTrue();
    }

    @Test
    void hasTierEnum_insufficientTier_returnsFalse() {
        setAuthentication(CapabilityTier.GUEST);
        assertThat(checker.hasTier(CapabilityTier.ADMIN)).isFalse();
    }

    @Test
    void isAdmin_withAdminTier_returnsTrue() {
        setAuthentication(CapabilityTier.ADMIN);
        assertThat(checker.isAdmin()).isTrue();
    }

    @Test
    void isSenior_withSeniorTier_returnsTrue() {
        setAuthentication(CapabilityTier.SENIOR);
        assertThat(checker.isSenior()).isTrue();
    }

    @Test
    void isAdvanced_withAdvancedTier_returnsTrue() {
        setAuthentication(CapabilityTier.ADVANCED);
        assertThat(checker.isAdvanced()).isTrue();
    }

    @Test
    void isInternal_withInternalTier_returnsTrue() {
        setAuthentication(CapabilityTier.INTERNAL);
        assertThat(checker.isInternal()).isTrue();
    }

    @Test
    void isInternal_withGuestTier_returnsFalse() {
        setAuthentication(CapabilityTier.GUEST);
        assertThat(checker.isInternal()).isFalse();
    }

    @Test
    void isBasic_withBasicTier_returnsTrue() {
        setAuthentication(CapabilityTier.BASIC);
        assertThat(checker.isBasic()).isTrue();
    }

    @Test
    void isBasic_withGuestTier_returnsFalse() {
        setAuthentication(CapabilityTier.GUEST);
        assertThat(checker.isBasic()).isFalse();
    }

    @Test
    void isBasic_withAdminTier_returnsTrue() {
        setAuthentication(CapabilityTier.ADMIN);
        assertThat(checker.isBasic()).isTrue();
    }

    private void setAuthentication(CapabilityTier tier) {
        var principal = UserPrincipal.builder()
                .email("test@kpi.ua")
                .tier(tier)
                .active(true)
                .build();
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
