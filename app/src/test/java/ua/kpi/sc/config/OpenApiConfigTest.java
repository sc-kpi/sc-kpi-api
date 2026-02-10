package ua.kpi.sc.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OpenApiConfigTest {

    private OpenApiConfig config;

    @BeforeEach
    void setUp() {
        config = new OpenApiConfig();
        ReflectionTestUtils.setField(config, "contactUrl", "https://sc.kpi.ua");
    }

    @Test
    void openAPI_hasCorrectInfo() {
        var openApi = config.openAPI();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("Student Council KPI API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openApi.getInfo().getContact().getName()).isEqualTo("DCT Team");
        assertThat(openApi.getInfo().getContact().getUrl()).isEqualTo("https://sc.kpi.ua");
    }

    @Test
    void openAPI_hasSecurityScheme() {
        var openApi = config.openAPI();

        assertThat(openApi.getSecurity()).hasSize(1);
        var scheme = openApi.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(scheme.getType()).isEqualTo(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    void authApi_returnsCorrectGroup() {
        var group = config.authApi();
        assertThat(group.getGroup()).isEqualTo("1-auth");
    }

    @Test
    void usersApi_returnsCorrectGroup() {
        var group = config.usersApi();
        assertThat(group.getGroup()).isEqualTo("2-users");
    }

    @Test
    void engagementsApi_returnsCorrectGroup() {
        var group = config.engagementsApi();
        assertThat(group.getGroup()).isEqualTo("3-engagements");
    }

    @Test
    void councilApi_returnsCorrectGroup() {
        var group = config.councilApi();
        assertThat(group.getGroup()).isEqualTo("4-council");
    }

    @Test
    void documentsApi_returnsCorrectGroup() {
        var group = config.documentsApi();
        assertThat(group.getGroup()).isEqualTo("5-documents");
    }

    @Test
    void notificationsApi_returnsCorrectGroup() {
        var group = config.notificationsApi();
        assertThat(group.getGroup()).isEqualTo("6-notifications");
    }

    @Test
    void adminApi_returnsCorrectGroup() {
        var group = config.adminApi();
        assertThat(group.getGroup()).isEqualTo("7-admin");
    }
}
