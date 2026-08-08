package com.example.onuldo.global.common.alert;

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

    private final String webhookUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public DiscordAlertSender(@Value("${discord.webhook-url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void sendServerError(String path, Exception e) {
        // 웹훅이 설정되지 않은 환경(로컬 등)에서는 알림을 보내지 않는다.
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        String payload = "{\"content\":\"" + escape(buildContent(path, e)) + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        // 알림 전송 실패가 실제 API 응답을 막거나 깨뜨리면 안 되므로 비동기로 보내고 예외는 로깅만 한다.
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(ex -> {
                    log.warn("디스코드 알림 전송 실패", ex);
                    return null;
                });
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

    private String escape(String raw) {
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
