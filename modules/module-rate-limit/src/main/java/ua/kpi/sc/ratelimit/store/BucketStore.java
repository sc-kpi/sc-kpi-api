package ua.kpi.sc.ratelimit.store;

import java.util.function.Supplier;

import io.github.bucket4j.Bucket;

public interface BucketStore {

    Bucket resolveBucket(String key, Supplier<Bucket> bucketSupplier);

    void invalidateAll();

    void invalidateByPrefix(String keyPrefix);

    long estimatedSize();
}
