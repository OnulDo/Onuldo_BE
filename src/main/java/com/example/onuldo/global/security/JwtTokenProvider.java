package com.example.onuldo.global.security;

import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String USER_ID_CLAIM = "userId";

    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;
    private final long accessExpirationMillis;
    private final long refreshExpirationMillis;

    public JwtTokenProvider(
            @Value("${jwt.token.secretKey}")
            String accessSecret,
            @Value("${jwt.token.refreshKey}")
            String refreshSecret,
            @Value("${jwt.token.expiration.access:1800000}")
            long accessExpirationMillis,
            @Value("${jwt.token.expiration.refresh:1209600000}")
            long refreshExpirationMillis
    ) {
        this.accessSecretKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshSecretKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMillis = accessExpirationMillis;
        this.refreshExpirationMillis = refreshExpirationMillis;
    }

    public String createAccessToken(User user) {
        return createToken(user, TokenType.ACCESS, accessSecretKey, accessExpirationMillis);
    }

    public String createRefreshToken(User user) {
        return createToken(user, TokenType.REFRESH, refreshSecretKey, refreshExpirationMillis);
    }

    public Long getUserIdFromAccessToken(String token) {
        return getUserId(token, TokenType.ACCESS, accessSecretKey);
    }

    public Long getUserIdFromRefreshToken(String token) {
        return getUserId(token, TokenType.REFRESH, refreshSecretKey);
    }

    public long getAccessExpirationMillis() {
        return accessExpirationMillis;
    }

    public long getRefreshExpirationMillis() {
        return refreshExpirationMillis;
    }

    private String createToken(User user, TokenType tokenType, SecretKey secretKey, long expirationMillis) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(expirationMillis);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(USER_ID_CLAIM, user.getId())
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    private Long getUserId(String token, TokenType expectedTokenType, SecretKey secretKey) {
        Claims claims = parseClaims(token, secretKey);
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        if (!expectedTokenType.name().equals(tokenType)) {
            throw new RestApiException(ErrorStatus._UNAUTHORIZED);
        }

        return claims.get(USER_ID_CLAIM, Long.class);
    }

    private Claims parseClaims(String token, SecretKey secretKey) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new RestApiException(ErrorStatus._TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new RestApiException(ErrorStatus._INVALID_TOKEN);
        }
    }
}
