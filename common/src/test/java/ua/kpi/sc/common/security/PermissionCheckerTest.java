package ua.kpi.sc.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

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

    // --- Partner-level tests ---

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PARTNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Test
    void hasPartnerLevel_noAuth_returnsFalse() {
        assertThat(checker.hasPartnerLevel(PARTNER_ID, PartnerLevel.BASIC)).isFalse();
    }

    @Test
    void hasPartnerLevel_admin_alwaysReturnsTrue() {
        setAuthenticationWithPartnerRoles(CapabilityTier.ADMIN, Map.of());
        assertThat(checker.hasPartnerLevel(PARTNER_ID, PartnerLevel.FULL)).isTrue();
    }

    @Test
    void hasPartnerLevel_hasSufficientLevel_returnsTrue() {
        setAuthenticationWithPartnerRoles(CapabilityTier.BASIC, Map.of(PARTNER_ID, PartnerLevel.FULL));
        assertThat(checker.hasPartnerLevel(PARTNER_ID, PartnerLevel.BASIC)).isTrue();
    }

    @Test
    void hasPartnerLevel_noPartnerRole_returnsFalse() {
        setAuthenticationWithPartnerRoles(CapabilityTier.BASIC, Map.of());
        assertThat(checker.hasPartnerLevel(PARTNER_ID, PartnerLevel.BASIC)).isFalse();
    }

    @Test
    void getEffectiveTierForPartner_noAuth_returnsZero() {
        assertThat(checker.getEffectiveTierForPartner(PARTNER_ID)).isEqualTo(0);
    }

    @Test
    void getEffectiveTierForPartner_admin_returnsFullTier() {
        setAuthenticationWithPartnerRoles(CapabilityTier.ADMIN, Map.of());
        assertThat(checker.getEffectiveTierForPartner(PARTNER_ID)).isEqualTo(5);
    }

    @Test
    void getEffectiveTierForPartner_withRole_returnsMin() {
        setAuthenticationWithPartnerRoles(CapabilityTier.ADVANCED, Map.of(PARTNER_ID, PartnerLevel.FULL));
        // ADVANCED=3, FULL effectiveTier=2, MIN=2
        assertThat(checker.getEffectiveTierForPartner(PARTNER_ID)).isEqualTo(2);
    }

    @Test
    void getEffectiveTierForPartner_noRole_returnsZero() {
        setAuthenticationWithPartnerRoles(CapabilityTier.BASIC, Map.of());
        assertThat(checker.getEffectiveTierForPartner(PARTNER_ID)).isEqualTo(0);
    }

    @Test
    void isSelfOrAdmin_noAuth_returnsFalse() {
        assertThat(checker.isSelfOrAdmin(USER_ID)).isFalse();
    }

    @Test
    void isSelfOrAdmin_sameUser_returnsTrue() {
        setAuthenticationWithId(USER_ID, CapabilityTier.BASIC);
        assertThat(checker.isSelfOrAdmin(USER_ID)).isTrue();
    }

    @Test
    void isSelfOrAdmin_differentUserNotAdmin_returnsFalse() {
        setAuthenticationWithId(UUID.fromString("00000000-0000-0000-0000-000000000002"), CapabilityTier.BASIC);
        assertThat(checker.isSelfOrAdmin(USER_ID)).isFalse();
    }

    @Test
    void isSelfOrAdmin_differentUserIsAdmin_returnsTrue() {
        setAuthenticationWithId(UUID.fromString("00000000-0000-0000-0000-000000000002"), CapabilityTier.ADMIN);
        assertThat(checker.isSelfOrAdmin(USER_ID)).isTrue();
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

    private void setAuthenticationWithPartnerRoles(CapabilityTier tier, Map<UUID, PartnerLevel> partnerRoles) {
        var principal = UserPrincipal.builder()
                .id(USER_ID)
                .email("test@kpi.ua")
                .tier(tier)
                .active(true)
                .partnerRoles(partnerRoles)
                .build();
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setAuthenticationWithId(UUID id, CapabilityTier tier) {
        var principal = UserPrincipal.builder()
                .id(id)
                .email("test@kpi.ua")
                .tier(tier)
                .active(true)
                .build();
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
