package com.example.onuldo.domain.user.service;

import com.example.onuldo.domain.user.entity.Notification;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.NotificationType;
import com.example.onuldo.domain.user.repository.NotificationRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationCreateService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;

    // 알림함용 Notification을 중복 없이 생성하는 메서드
    public Optional<Notification> createIfAbsent(
            Long userId,
            NotificationType type,
            String title,
            String content,
            String refType,
            Long refId
    ) {
        try {
            return executeInNewTransaction(status -> createIfAbsentInTransaction(
                    userId,
                    type,
                    title,
                    content,
                    refType,
                    refId
            ));
        } catch (DataIntegrityViolationException e) {
            if (refType == null || refId == null) {
                throw e;
            }

            Optional<Notification> existing = executeInNewTransaction(status -> notificationRepository.findByUser_IdAndTypeAndRefTypeAndRefId(
                    userId,
                    type,
                    refType,
                    refId
            ));
            if (existing.isEmpty()) {
                throw e;
            }

            return existing;
        }
    }

    private Optional<Notification> createIfAbsentInTransaction(
            Long userId,
            NotificationType type,
            String title,
            String content,
            String refType,
            Long refId
    ) {
        if (refType != null && refId != null) {
            var existing = notificationRepository.findByUser_IdAndTypeAndRefTypeAndRefId(
                    userId,
                    type,
                    refType,
                    refId
            );
            if (existing.isPresent()) {
                return existing;
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        return Optional.of(notificationRepository.saveAndFlush(
                Notification.builder()
                        .user(user)
                        .type(type)
                        .title(title)
                        .content(content)
                        .refType(refType)
                        .refId(refId)
                .build()
        ));
    }

    private <T> T executeInNewTransaction(TransactionCallback<T> callback) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(callback);
    }
}
