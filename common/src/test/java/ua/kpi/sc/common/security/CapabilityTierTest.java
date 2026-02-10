package ua.kpi.sc.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CapabilityTierTest {

    @ParameterizedTest
    @CsvSource({
            "0, GUEST",
            "1, BASIC",
            "2, INTERNAL",
            "3, ADVANCED",
            "4, SENIOR",
            "5, ADMIN"
    })
    void fromLevel_returnsCorrectTier(int level, CapabilityTier expected) {
        assertThat(CapabilityTier.fromLevel(level)).isEqualTo(expected);
    }

    @Test
    void fromLevel_unknownLevel_returnsGuest() {
        assertThat(CapabilityTier.fromLevel(99)).isEqualTo(CapabilityTier.GUEST);
    }

    @Test
    void isAtLeast_sufficientTier_returnsTrue() {
        assertThat(CapabilityTier.ADMIN.isAtLeast(CapabilityTier.BASIC)).isTrue();
    }

    @Test
    void isAtLeast_insufficientTier_returnsFalse() {
        assertThat(CapabilityTier.GUEST.isAtLeast(CapabilityTier.ADMIN)).isFalse();
    }

    @Test
    void isAtLeast_sameTier_returnsTrue() {
        assertThat(CapabilityTier.SENIOR.isAtLeast(CapabilityTier.SENIOR)).isTrue();
    }
}
