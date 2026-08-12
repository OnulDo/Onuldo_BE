package com.example.onuldo.global.ratelimit;

import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.RateLimitException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

@Component
public class EmailRateLimiter {

    private static final int EMAIL_LIMIT_PER_MINUTE = 20;

    // 단일 인스턴스 기준 인메모리 카운터. 서버가 여러 대로 늘어나면 Redis(INCR + TTL) 기반으로 교체 필요.
    private final Cache<String, Bucket> emailBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    // scope로 동작 유형(signup/oauth-login 등)을 구분해, 공개 엔드포인트에서 특정 이메일의
    // 버킷을 미리 소진시켜 다른 동작(특히 로그인)을 막는 걸 방지한다.
    public void consume(String email, String scope) {
        String bucketKey = scope + ":" + email.toLowerCase(Locale.ROOT);
        Bucket bucket = emailBuckets.get(bucketKey, key -> newBucket(EMAIL_LIMIT_PER_MINUTE, Duration.ofMinutes(1)));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitException(ErrorStatus._RATE_LIMIT_EXCEEDED);
        }
    }

    private Bucket newBucket(int capacity, Duration period) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, period)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
