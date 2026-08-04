package com.example.onuldo.domain.user.service;

import com.example.onuldo.domain.auth.entity.DeviceLog;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushService {
    /**
     * 단일 기기에 Notification 메시지 전송
     */
    public void send(DeviceLog deviceLog, String title, String body) throws FirebaseMessagingException {
        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setToken(deviceLog.getFcmToken())
                .build();

        String response = FirebaseMessaging.getInstance().send(message);
        log.info("메시지 전송 성공: {}", response);
    }

    /**
     * Notification + Data 혼합 메시지 전송
     */
    public void send(DeviceLog deviceLog, String title, String body, Map<String, String> data) throws FirebaseMessagingException {

        Message.Builder messageBuilder = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setToken(deviceLog.getFcmToken());

        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        }

        String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
        log.info("메시지 전송 성공: {}", response);
    }
}
