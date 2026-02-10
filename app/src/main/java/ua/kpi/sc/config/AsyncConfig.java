package ua.kpi.sc.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configures the application-wide async thread pool used by {@code @Async} methods.
 *
 * <p>Thread pool parameters are externalized under the {@code app.async.*} property prefix:
 * <ul>
 *   <li>{@code app.async.core-pool-size} — minimum number of threads kept alive</li>
 *   <li>{@code app.async.max-pool-size} — upper bound on thread count</li>
 *   <li>{@code app.async.queue-capacity} — task queue size before new threads are spawned</li>
 * </ul>
 *
 * @since 0.1.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${app.async.core-pool-size}")
    private int corePoolSize;

    @Value("${app.async.max-pool-size}")
    private int maxPoolSize;

    @Value("${app.async.queue-capacity}")
    private int queueCapacity;

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("sc-kpi-async-");
        executor.initialize();
        return executor;
    }
}
