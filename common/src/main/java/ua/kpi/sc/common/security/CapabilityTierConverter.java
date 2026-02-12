package ua.kpi.sc.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter that maps {@link CapabilityTier} to its integer level for database storage.
 *
 * @see CapabilityTier#getLevel()
 * @see CapabilityTier#fromLevel(int)
 * @since 0.1.0
 */
@Converter(autoApply = true)
public class CapabilityTierConverter implements AttributeConverter<CapabilityTier, Integer> {

    @Override
    public Integer convertToDatabaseColumn(CapabilityTier tier) {
        return tier == null ? null : tier.getLevel();
    }

    @Override
    public CapabilityTier convertToEntityAttribute(Integer level) {
        return level == null ? CapabilityTier.GUEST : CapabilityTier.fromLevel(level);
    }
}
