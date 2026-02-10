package ua.kpi.sc.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ua.kpi.sc.common.security.handler.ProblemDetailAccessDeniedHandler;
import ua.kpi.sc.common.security.handler.ProblemDetailAuthenticationEntryPoint;

class SecurityFilterChainConfigTest {

    private SecurityFilterChainConfig config;
    private CorsProperties corsProperties;

    @BeforeEach
    void setUp() {
        corsProperties = new CorsProperties();
        var accessDeniedHandler = mock(ProblemDetailAccessDeniedHandler.class);
        var authenticationEntryPoint = mock(ProblemDetailAuthenticationEntryPoint.class);
        config = new SecurityFilterChainConfig(corsProperties, accessDeniedHandler, authenticationEntryPoint);
        ReflectionTestUtils.setField(config, "bcryptStrength", 10);
    }

    @SuppressWarnings("unchecked")
    @Test
    void filterChain_configuresSecurityCorrectly() throws Exception {
        var http = mock(HttpSecurity.class, Mockito.RETURNS_DEEP_STUBS);
        var expectedChain = mock(DefaultSecurityFilterChain.class);

        when(http.csrf(any(Customizer.class))).thenReturn(http);
        when(http.cors(any(Customizer.class))).thenReturn(http);
        when(http.sessionManagement(any(Customizer.class))).thenReturn(http);
        when(http.authorizeHttpRequests(any(Customizer.class))).thenReturn(http);
        when(http.exceptionHandling(any(Customizer.class))).thenReturn(http);
        when(http.build()).thenReturn(expectedChain);

        var chain = config.filterChain(http);

        assertThat(chain).isSameAs(expectedChain);
        verify(http).build();

        // Capture and invoke customizers to cover lambda bodies
        var csrfCaptor = ArgumentCaptor.forClass(Customizer.class);
        verify(http).csrf(csrfCaptor.capture());
        var csrfConfigurer = mock(CsrfConfigurer.class);
        csrfCaptor.getValue().customize(csrfConfigurer);
        verify(csrfConfigurer).disable();

        var corsCaptor = ArgumentCaptor.forClass(Customizer.class);
        verify(http).cors(corsCaptor.capture());
        var corsConfigurer = mock(CorsConfigurer.class);
        corsCaptor.getValue().customize(corsConfigurer);
        verify(corsConfigurer).configurationSource(any());

        var smCaptor = ArgumentCaptor.forClass(Customizer.class);
        verify(http).sessionManagement(smCaptor.capture());
        var smConfigurer = mock(SessionManagementConfigurer.class);
        smCaptor.getValue().customize(smConfigurer);
        verify(smConfigurer).sessionCreationPolicy(any());

        var authCaptor = ArgumentCaptor.forClass(Customizer.class);
        verify(http).authorizeHttpRequests(authCaptor.capture());
        var authConfigurer = mock(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class,
                Mockito.RETURNS_DEEP_STUBS);
        authCaptor.getValue().customize(authConfigurer);

        var ehCaptor = ArgumentCaptor.forClass(Customizer.class);
        verify(http).exceptionHandling(ehCaptor.capture());
        var ehConfigurer = mock(ExceptionHandlingConfigurer.class);
        when(ehConfigurer.accessDeniedHandler(any())).thenReturn(ehConfigurer);
        when(ehConfigurer.authenticationEntryPoint(any())).thenReturn(ehConfigurer);
        ehCaptor.getValue().customize(ehConfigurer);
        verify(ehConfigurer).accessDeniedHandler(any());
        verify(ehConfigurer).authenticationEntryPoint(any());
    }

    @Test
    void corsConfigurationSource_returnsConfiguredSource() {
        var source = config.corsConfigurationSource();

        assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);
        var urlSource = (UrlBasedCorsConfigurationSource) source;
        CorsConfiguration corsConfig = urlSource.getCorsConfiguration(
                new org.springframework.mock.web.MockHttpServletRequest("GET", "/test"));
        assertThat(corsConfig).isNotNull();
        assertThat(corsConfig.getAllowedOrigins()).isEqualTo(corsProperties.getAllowedOrigins());
        assertThat(corsConfig.getAllowedMethods()).isEqualTo(corsProperties.getAllowedMethods());
        assertThat(corsConfig.getAllowedHeaders()).isEqualTo(corsProperties.getAllowedHeaders());
        assertThat(corsConfig.getAllowCredentials()).isEqualTo(corsProperties.isAllowCredentials());
        assertThat(corsConfig.getMaxAge()).isEqualTo(corsProperties.getMaxAge());
    }

    @Test
    void passwordEncoder_returnsBCryptEncoder() {
        var encoder = config.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        String encoded = encoder.encode("test");
        assertThat(encoder.matches("test", encoded)).isTrue();
    }

    @Test
    void authenticationManager_delegatesToConfiguration() throws Exception {
        var authConfig = mock(AuthenticationConfiguration.class);
        var expectedManager = mock(AuthenticationManager.class);
        when(authConfig.getAuthenticationManager()).thenReturn(expectedManager);

        var manager = config.authenticationManager(authConfig);

        assertThat(manager).isSameAs(expectedManager);
    }
}
