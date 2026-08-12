package com.example.onuldo.global.ratelimit;

import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int IP_LIMIT_PER_MINUTE = 100;
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    // 단일 인스턴스 기준 인메모리 카운터. 서버가 여러 대로 늘어나면 Redis(INCR + TTL) 기반으로 교체 필요.
    private final Cache<String, Bucket> ipBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    // 이 목록에 있는 IP(리버스 프록시/로드밸런서 자신)에서 직접 접속해온 요청만 X-Forwarded-For를 신뢰한다.
    // 설정 안 하면(기본값 빈 목록) 항상 request.getRemoteAddr()만 사용 — 클라이언트가 헤더를 조작해
    // IP 버킷을 매번 새로 만들어 rate limit을 우회하는 걸 막기 위함.
    private final Set<String> trustedProxyIps;

    public RateLimitInterceptor(@Value("${rate-limit.trusted-proxies:}") List<String> trustedProxyIps) {
        this.trustedProxyIps = trustedProxyIps.stream()
                .map(String::trim)
                .filter(ip -> !ip.isBlank())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = resolveClientIp(request);
        Bucket ipBucket = ipBuckets.get(clientIp, key -> newBucket(IP_LIMIT_PER_MINUTE, Duration.ofMinutes(1)));
        if (!ipBucket.tryConsume(1)) {
            throw new RestApiException(ErrorStatus._RATE_LIMIT_EXCEEDED);
        }

        return true;
    }

    private Bucket newBucket(int capacity, Duration period) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, period)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxyIps.contains(remoteAddr)) {
            return remoteAddr;
        }

        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return remoteAddr;
    }
}
