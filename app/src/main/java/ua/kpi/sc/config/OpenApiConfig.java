package ua.kpi.sc.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures OpenAPI (Swagger) metadata and endpoint grouping for the SC-KPI API.
 *
 * <p>Defines a global JWT Bearer security scheme and organizes endpoints into 7 groups:
 * auth, users, engagements (clubs + projects), council (departments), documents,
 * notifications (settings + webhooks), and admin (audit logs).
 *
 * @since 0.1.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.contact-url}")
    private String contactUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Student Council KPI API")
                        .description("Student Council of KPI - Backend API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("DCT Team")
                                .url(contactUrl)))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("1-auth")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi usersApi() {
        return GroupedOpenApi.builder()
                .group("2-users")
                .pathsToMatch("/api/v1/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi engagementsApi() {
        return GroupedOpenApi.builder()
                .group("3-engagements")
                .pathsToMatch("/api/v1/clubs/**", "/api/v1/projects/**")
                .build();
    }

    @Bean
    public GroupedOpenApi councilApi() {
        return GroupedOpenApi.builder()
                .group("4-council")
                .pathsToMatch("/api/v1/departments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi documentsApi() {
        return GroupedOpenApi.builder()
                .group("5-documents")
                .pathsToMatch("/api/v1/documents/**")
                .build();
    }

    @Bean
    public GroupedOpenApi notificationsApi() {
        return GroupedOpenApi.builder()
                .group("6-notifications")
                .pathsToMatch("/api/v1/notifications/**", "/api/v1/webhooks/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("7-admin")
                .pathsToMatch("/api/v1/admin/**")
                .build();
    }
}
