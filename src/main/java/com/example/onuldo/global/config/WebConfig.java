package com.example.onuldo.global.config;

import com.example.onuldo.global.ratelimit.RateLimitInterceptor;
import com.example.onuldo.global.security.AuthUserArgumentResolver;
import com.example.onuldo.global.security.JwtAuthenticationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final AuthUserArgumentResolver authUserArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/signup",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/oauth/login",
                        "/api/auth/oauth/signup",
                        "/api/auth/email/exists",
                        "/api/terms/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/error"
                );

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
