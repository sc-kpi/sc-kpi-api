package ua.kpi.sc.ratelimit.store;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import ua.kpi.sc.ratelimit.config.RateLimitProperties;

public class CaffeineBucketStore implements BucketStore {

    private final Cache<String, Bucket> cache;

    public CaffeineBucketStore(RateLimitProperties properties) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getMaxBucketCount())
                .expireAfterAccess(properties.getBucketTtlSeconds(), TimeUnit.SECONDS)
                .build();
    }

    @Override
    public Bucket resolveBucket(String key, Supplier<Bucket> bucketSupplier) {
        return cache.get(key, k -> bucketSupplier.get());
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    public void invalidateByPrefix(String keyPrefix) {
        cache.asMap().keySet().removeIf(key -> key.startsWith(keyPrefix));
    }

    @Override
    public long estimatedSize() {
        return cache.estimatedSize();
    }
}
