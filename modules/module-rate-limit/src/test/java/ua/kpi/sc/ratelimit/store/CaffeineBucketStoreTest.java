package ua.kpi.sc.ratelimit.store;

import java.time.Duration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.kpi.sc.ratelimit.config.RateLimitProperties;

import static org.assertj.core.api.Assertions.assertThat;

class CaffeineBucketStoreTest {

    private CaffeineBucketStore store;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setMaxBucketCount(100);
        properties.setBucketTtlSeconds(60);
        store = new CaffeineBucketStore(properties);
    }

    @Test
    void resolveBucket_createsNewBucket() {
        Bucket bucket = store.resolveBucket("test-key", this::createBucket);

        assertThat(bucket).isNotNull();
        assertThat(store.estimatedSize()).isEqualTo(1);
    }

    @Test
    void resolveBucket_returnsSameBucketForSameKey() {
        Bucket first = store.resolveBucket("test-key", this::createBucket);
        Bucket second = store.resolveBucket("test-key", this::createBucket);

        assertThat(first).isSameAs(second);
    }

    @Test
    void resolveBucket_createsDifferentBucketsForDifferentKeys() {
        Bucket first = store.resolveBucket("key-1", this::createBucket);
        Bucket second = store.resolveBucket("key-2", this::createBucket);

        assertThat(first).isNotSameAs(second);
        assertThat(store.estimatedSize()).isEqualTo(2);
    }

    @Test
    void invalidateAll_clearsAllBuckets() {
        store.resolveBucket("key-1", this::createBucket);
        store.resolveBucket("key-2", this::createBucket);

        store.invalidateAll();

        assertThat(store.estimatedSize()).isEqualTo(0);
    }

    @Test
    void invalidateByPrefix_removesMatchingKeys() {
        store.resolveBucket("ip:rule1:192.168.1.1", this::createBucket);
        store.resolveBucket("ip:rule1:192.168.1.2", this::createBucket);
        store.resolveBucket("ip:rule2:192.168.1.1", this::createBucket);

        store.invalidateByPrefix("ip:rule1");

        assertThat(store.estimatedSize()).isEqualTo(1);
    }

    @Test
    void estimatedSize_returnsCorrectCount() {
        assertThat(store.estimatedSize()).isEqualTo(0);

        store.resolveBucket("key-1", this::createBucket);
        assertThat(store.estimatedSize()).isEqualTo(1);

        store.resolveBucket("key-2", this::createBucket);
        assertThat(store.estimatedSize()).isEqualTo(2);
    }

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofSeconds(60))))
                .build();
    }
}
