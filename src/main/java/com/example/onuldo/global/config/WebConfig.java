package com.example.onuldo.global.config;

import com.example.onuldo.global.ratelimit.RateLimitInterceptor;
import com.example.onuldo.global.security.AuthUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final AuthUserArgumentResolver authUserArgumentResolver;

    // JWT 인증은 SecurityConfig(JwtAuthenticationFilter + authorizeHttpRequests)로 일원화됨.
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(
                        "/api/auth/signup",
                        "/api/auth/login",
                        "/api/auth/email/exists",
                        "/api/auth/oauth/login",
                        "/api/auth/oauth/signup"
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authUserArgumentResolver);
    }
}
