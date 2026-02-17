package ua.kpi.sc.ratelimit.filter;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import ua.kpi.sc.common.featureflag.FeatureFlagChecker;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.ratelimit.config.RateLimitProperties;
import ua.kpi.sc.ratelimit.entity.RateLimitRule;
import ua.kpi.sc.ratelimit.service.RateLimitEvaluationService;

@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final RateLimitProperties properties;
    private final RateLimitEvaluationService evaluationService;
    private final FeatureFlagChecker featureFlagChecker;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Hard kill switch
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Soft kill switch via feature flag
        UUID userId = null;
        Integer tierLevel = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            userId = principal.getId();
            tierLevel = principal.getTier().getLevel();
        }

        if (!featureFlagChecker.isEnabled("rate-limiting.enabled", userId, tierLevel)) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();
        String method = request.getMethod();
        String clientIp = extractClientIp(request);

        // Check whitelisted IPs
        if (properties.getWhitelistedIps().contains(clientIp)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check excluded patterns
        for (String pattern : properties.getExcludedPatterns()) {
            if (PATH_MATCHER.match(pattern, path)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        RateLimitRule rule = evaluationService.resolveRule(path, method, clientIp, userId, tierLevel);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        ConsumptionProbe probe = evaluationService.tryConsume(rule, clientIp, userId, tierLevel);

        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.getBurstCapacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            long resetSeconds = probe.getNanosToWaitForReset() / 1_000_000_000;
            response.setHeader("X-RateLimit-Reset", String.valueOf(resetSeconds));
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setHeader("X-RateLimit-Reset", String.valueOf(retryAfterSeconds));

            evaluationService.recordViolation(rule.getEndpointPattern());

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded. Try again in %d seconds.".formatted(retryAfterSeconds));
            problem.setTitle("Too Many Requests");
            problem.setType(URI.create("about:blank"));
            problem.setInstance(URI.create(request.getRequestURI()));

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), problem);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/actuator/health");
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
