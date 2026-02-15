package ua.kpi.sc.common.featureflag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative annotation that gates access to a controller method or all methods
 * of a controller class behind a feature flag.
 *
 * <p>When the referenced feature flag is disabled, the annotated endpoint returns
 * an HTTP 404 Not Found by default (configurable via {@link #disabledStatus()}).
 *
 * @see FeatureFlagChecker
 * @since 0.3.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureFlag {

    /**
     * The feature flag key to evaluate.
     *
     * @return the flag key
     */
    String value();

    /**
     * HTTP status code to return when the flag is disabled.
     * Defaults to 404 (Not Found).
     *
     * @return the HTTP status code
     */
    int disabledStatus() default 404;
}
