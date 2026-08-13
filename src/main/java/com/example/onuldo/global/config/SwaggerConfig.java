package com.example.onuldo.global.config;

import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import com.example.onuldo.global.config.swagger.ApiErrorCodes;
import com.example.onuldo.global.config.swagger.SwaggerErrorResponse;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String APPLICATION_JSON = "application/json";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Onuldo API")
                        .description("Onuldo backend API documentation")
                        .version("v1"))
                .tags(List.of(
                        new Tag().name("Auth").description("이메일 회원가입, 로그인, JWT 재발급 API"),
                        new Tag().name("Challenge").description("챌린지 관련 API"),
                        new Tag().name("File").description("파일 관련 API"),
                        new Tag().name("Party").description("파티 생성, 조회, 참여, 시작, 진행 피드, 정산 결과, 홈 화면 관련 API"),
                        new Tag().name("Term").description("약관 관련 API"),
                        new Tag().name("User").description("마이페이지 API")
                ))
                .components(new Components()
                        .addSchemas("SwaggerErrorResponse", errorResponseSchema())
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public OpenApiCustomizer sortPathsByResourceCustomizer() {
        return openApi -> {
            Paths paths = openApi.getPaths();
            if (paths != null) {
                Paths sortedPaths = paths.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey(this::comparePaths))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (first, second) -> first,
                                Paths::new
                        ));

                openApi.setPaths(sortedPaths);
            }

            if (openApi.getTags() != null) {
                openApi.setTags(openApi.getTags().stream()
                        .sorted(Comparator
                                .comparingInt((Tag tag) -> tagOrder(tag.getName()))
                                .thenComparing(Tag::getName))
                        .toList());
            }
        };
    }

    @Bean
    public OperationCustomizer apiErrorCodesCustomizer() {
        return (operation, handlerMethod) -> {
            ApiErrorCodes apiErrorCodes = findApiErrorCodes(handlerMethod);
            if (apiErrorCodes == null || apiErrorCodes.value().length == 0) {
                return operation;
            }

            Map<Integer, java.util.List<ErrorStatus>> errorsByStatus = Arrays.stream(apiErrorCodes.value())
                    .collect(Collectors.groupingBy(
                            errorStatus -> errorStatus.getHttpStatus().value(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            errorsByStatus.forEach((status, errorStatuses) ->
                    operation.getResponses().addApiResponse(String.valueOf(status), apiResponse(errorStatuses)));

            return operation;
        };
    }

    private ApiResponse apiResponse(java.util.List<ErrorStatus> errorStatuses) {
        MediaType mediaType = new MediaType()
                .schema(new Schema<SwaggerErrorResponse>().$ref("#/components/schemas/SwaggerErrorResponse"));

        errorStatuses.forEach(errorStatus -> {
            Map<String, Object> example = new LinkedHashMap<>();
            example.put("timestamp", "2026-08-13T12:00:00");
            example.put("code", errorCode(errorStatus));
            example.put("message", errorStatus.getMessage());
            example.put("result", null);

            mediaType.addExamples(errorCode(errorStatus), new Example()
                        .summary(errorCode(errorStatus))
                        .description(errorStatus.getMessage())
                        .value(example));
        });

        String description = errorStatuses.stream()
                .map(errorStatus -> errorCode(errorStatus) + " - " + errorStatus.getMessage())
                .collect(Collectors.joining("<br/>"));

        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(APPLICATION_JSON, mediaType));
    }

    private Schema<SwaggerErrorResponse> errorResponseSchema() {
        return new Schema<SwaggerErrorResponse>()
                .type("object")
                .description("공통 에러 응답")
                .addProperty("timestamp", new Schema<>().type("string").format("date-time").example("2026-08-13T12:00:00"))
                .addProperty("code", new Schema<>().type("string").example("BAD_REQUEST"))
                .addProperty("message", new Schema<>().type("string").example("잘못된 요청입니다."))
                .addProperty("result", new Schema<>().nullable(true).description("검증 오류 상세 또는 포인트 부족 상세. 없으면 null"));
    }

    private ApiErrorCodes findApiErrorCodes(HandlerMethod handlerMethod) {
        ApiErrorCodes annotation = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(),
                ApiErrorCodes.class
        );
        if (annotation != null) {
            return annotation;
        }

        return Arrays.stream(handlerMethod.getBeanType().getInterfaces())
                .map(type -> findInterfaceMethod(type, handlerMethod.getMethod()))
                .filter(Objects::nonNull)
                .map(method -> AnnotatedElementUtils.findMergedAnnotation(method, ApiErrorCodes.class))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String errorCode(ErrorStatus errorStatus) {
        return errorStatus.getCode().getCode();
    }

    private Method findInterfaceMethod(Class<?> type, Method method) {
        try {
            return type.getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private int resourceOrder(String path) {
        if (path.startsWith("/api/auth")) {
            return 1;
        }
        if (path.startsWith("/api/challenges")) {
            return 2;
        }
        if (path.startsWith("/api/files")) {
            return 3;
        }
        if (path.startsWith("/api/parties")) {
            return 4;
        }
        if (path.startsWith("/api/terms")) {
            return 5;
        }
        if (path.startsWith("/api/users")) {
            return 6;
        }
        return 100;
    }

    private int comparePaths(String first, String second) {
        return Comparator
                .comparingInt(this::resourceOrder)
                .thenComparingInt(this::endpointOrder)
                .thenComparing(String::compareTo)
                .compare(first, second);
    }

    private int endpointOrder(String path) {
        return switch (path) {
            case "/api/auth/signup" -> 1;
            case "/api/auth/login" -> 2;
            case "/api/auth/oauth/login" -> 3;
            case "/api/auth/oauth/signup" -> 4;
            case "/api/auth/email/exists" -> 5;
            case "/api/auth/refresh" -> 6;
            default -> 100;
        };
    }

    private int tagOrder(String tagName) {
        return switch (tagName) {
            case "Auth" -> 1;
            case "Challenge" -> 2;
            case "File" -> 3;
            case "Party" -> 4;
            case "Term" -> 5;
            case "User" -> 6;
            default -> 100;
        };
    }

}
