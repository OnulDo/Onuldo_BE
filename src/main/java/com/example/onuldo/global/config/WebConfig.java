package com.example.onuldo.global.config;

import com.example.onuldo.global.security.JwtAuthenticationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/signup",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/error"
                );
    }
}
