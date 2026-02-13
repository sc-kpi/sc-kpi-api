package ua.kpi.sc.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Provides a {@link RestClient} bean for OAuth HTTP calls.
 *
 * @since 0.1.0
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient oAuthRestClient() {
        return RestClient.create();
    }
}
