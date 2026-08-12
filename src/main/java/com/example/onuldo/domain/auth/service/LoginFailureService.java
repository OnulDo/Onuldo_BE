package com.example.onuldo.domain.auth.service;

import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginFailureService {

    private static final int MAX_LOGIN_FAILURE_COUNT = 5;
    private static final int LOCK_SECONDS = 60;

    private final UserRepository userRepository;

    @Transactional
    public void recordFailure(Long userId, LocalDateTime now) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new RestApiException(ErrorStatus._USER_NOT_FOUND));

        if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now)) {
            user.setLoginFailCount(0);
            user.setLockedUntil(null);
        }

        int nextFailCount = user.getLoginFailCount() == null ? 1 : user.getLoginFailCount() + 1;
        user.setLoginFailCount(nextFailCount);
        user.setLockedUntil(nextFailCount >= MAX_LOGIN_FAILURE_COUNT ? now.plusSeconds(LOCK_SECONDS) : null);
    }
}
