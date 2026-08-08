package com.example.onuldo.global.common.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class DiscordAlertSender {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // 디스코드 메시지 본문은 2000자 제한이므로 여유를 두고 자른다.
    private static final int MAX_CONTENT_LENGTH = 1800;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 잘못된 웹훅 URL이 예외 처리 도중 다시 예외를 던지지 않도록 생성 시점에 미리 파싱해 둔다.
    private final URI webhookUri;

    public DiscordAlertSender(@Value("${discord.webhook-url:}") String webhookUrl) {
        this.webhookUri = parseWebhookUri(webhookUrl);
    }

    public void sendServerError(String path, Exception e) {
        // 웹훅이 설정되지 않았거나 URL이 잘못된 환경에서는 알림을 보내지 않는다.
        if (webhookUri == null) {
            return;
        }

        String payload = buildPayload(path, e);
        if (payload == null) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(webhookUri)
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        // 알림 전송 실패가 실제 API 응답을 막거나 깨뜨리면 안 되므로 비동기로 보낸다.
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    // 2xx가 아니면 디스코드가 알림을 거부한 것이므로 경고 로그를 남긴다.
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        log.warn("디스코드 알림 응답 실패: status={}", response.statusCode());
                    }
                })
                .exceptionally(ex -> {
                    log.warn("디스코드 알림 전송 실패", ex);
                    return null;
                });
    }

    private URI parseWebhookUri(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return null;
        }
        try {
            return URI.create(webhookUrl);
        } catch (IllegalArgumentException ex) {
            log.warn("디스코드 웹훅 URL이 올바르지 않아 알림을 비활성화합니다: {}", webhookUrl, ex);
            return null;
        }
    }

    private String buildPayload(String path, Exception e) {
        // 문자열 연결 대신 ObjectMapper로 직렬화해 제어 문자까지 안전하게 이스케이프한다.
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("content", buildContent(path, e));
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            log.warn("디스코드 알림 payload 생성 실패", ex);
            return null;
        }
    }

    private String buildContent(String path, Exception e) {
        String content = "🚨 500 서버 에러 발생\n"
                + "시간: " + LocalDateTime.now().format(TIME_FORMAT) + "\n"
                + "경로: " + (path == null ? "-" : path) + "\n"
                + "예외: " + e.getClass().getSimpleName() + "\n"
                + "메시지: " + e.getMessage();

        if (content.length() > MAX_CONTENT_LENGTH) {
            return content.substring(0, MAX_CONTENT_LENGTH);
        }
        return content;
    }
}
