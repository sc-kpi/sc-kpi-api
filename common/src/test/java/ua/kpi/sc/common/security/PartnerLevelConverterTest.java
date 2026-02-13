package ua.kpi.sc.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PartnerLevelConverterTest {

    private final PartnerLevelConverter converter = new PartnerLevelConverter();

    @Test
    void convertToDatabaseColumn_full() {
        assertThat(converter.convertToDatabaseColumn(PartnerLevel.FULL)).isEqualTo("full");
    }

    @Test
    void convertToDatabaseColumn_documents() {
        assertThat(converter.convertToDatabaseColumn(PartnerLevel.DOCUMENTS)).isEqualTo("documents");
    }

    @Test
    void convertToDatabaseColumn_basic() {
        assertThat(converter.convertToDatabaseColumn(PartnerLevel.BASIC)).isEqualTo("basic");
    }

    @Test
    void convertToDatabaseColumn_null() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_full() {
        assertThat(converter.convertToEntityAttribute("full")).isEqualTo(PartnerLevel.FULL);
    }

    @Test
    void convertToEntityAttribute_documents() {
        assertThat(converter.convertToEntityAttribute("documents")).isEqualTo(PartnerLevel.DOCUMENTS);
    }

    @Test
    void convertToEntityAttribute_basic() {
        assertThat(converter.convertToEntityAttribute("basic")).isEqualTo(PartnerLevel.BASIC);
    }

    @Test
    void convertToEntityAttribute_null() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_unknownFallsToBasic() {
        assertThat(converter.convertToEntityAttribute("unknown")).isEqualTo(PartnerLevel.BASIC);
    }
}
