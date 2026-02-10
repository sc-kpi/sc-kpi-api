package ua.kpi.sc.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

class AsyncConfigTest {

    @Test
    void taskExecutor_returnsConfiguredExecutor() {
        var config = new AsyncConfig();
        ReflectionTestUtils.setField(config, "corePoolSize", 4);
        ReflectionTestUtils.setField(config, "maxPoolSize", 8);
        ReflectionTestUtils.setField(config, "queueCapacity", 100);

        var executor = (ThreadPoolTaskExecutor) config.taskExecutor();

        assertThat(executor.getCorePoolSize()).isEqualTo(4);
        assertThat(executor.getMaxPoolSize()).isEqualTo(8);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("sc-kpi-async-");
        executor.shutdown();
    }
}
