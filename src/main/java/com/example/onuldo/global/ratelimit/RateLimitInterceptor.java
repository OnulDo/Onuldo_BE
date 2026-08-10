package com.example.onuldo.global.ratelimit;

import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Locale;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int IP_LIMIT_PER_MINUTE = 5;
    private static final int EMAIL_LIMIT_PER_HOUR = 3;
    private static final String EMAIL_PARAM = "email";
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    // 단일 인스턴스 기준 인메모리 카운터. 서버가 여러 대로 늘어나면 Redis(INCR + TTL) 기반으로 교체 필요.
    private final Cache<String, Bucket> ipBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    private final Cache<String, Bucket> emailBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(2))
            .maximumSize(100_000)
            .build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = resolveClientIp(request);
        Bucket ipBucket = ipBuckets.get(clientIp, key -> newBucket(IP_LIMIT_PER_MINUTE, Duration.ofMinutes(1)));
        if (!ipBucket.tryConsume(1)) {
            throw new RestApiException(GlobalErrorStatus._RATE_LIMIT_EXCEEDED);
        }

        String email = request.getParameter(EMAIL_PARAM);
        if (email != null && !email.isBlank()) {
            String emailKey = email.toLowerCase(Locale.ROOT);
            Bucket emailBucket = emailBuckets.get(emailKey, key -> newBucket(EMAIL_LIMIT_PER_HOUR, Duration.ofHours(1)));
            if (!emailBucket.tryConsume(1)) {
                throw new RestApiException(GlobalErrorStatus._RATE_LIMIT_EXCEEDED);
            }
        }

        return true;
    }

    private Bucket newBucket(int capacity, Duration period) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, period));
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
