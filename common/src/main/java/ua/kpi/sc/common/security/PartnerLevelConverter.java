package ua.kpi.sc.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter that maps {@link PartnerLevel} to its string value for database storage.
 *
 * @see PartnerLevel#getValue()
 * @since 0.2.0
 */
@Converter(autoApply = true)
public class PartnerLevelConverter implements AttributeConverter<PartnerLevel, String> {

    @Override
    public String convertToDatabaseColumn(PartnerLevel level) {
        return level == null ? null : level.getValue();
    }

    @Override
    public PartnerLevel convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        for (PartnerLevel level : PartnerLevel.values()) {
            if (level.getValue().equals(value)) {
                return level;
            }
        }
        return PartnerLevel.BASIC;
    }
}
