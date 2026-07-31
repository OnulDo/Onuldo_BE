package com.example.onuldo.global.common.time;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TimeService {

    public static final ZoneOffset UTC = ZoneOffset.UTC;
    public static final ZoneOffset KST = ZoneOffset.ofHours(9);

    public LocalDateTime nowUtc() {
        return LocalDateTime.now(UTC);
    }

    public static LocalDateTime nowUtcStatic() {
        return LocalDateTime.now(UTC);
    }

    public LocalDateTime nowKst() {
        return LocalDateTime.now(KST);
    }

    public static LocalDateTime nowKstStatic() {
        return LocalDateTime.now(KST);
    }

    public LocalDate todayKst() {
        return nowKst().toLocalDate();
    }

    public static LocalDate todayKstStatic() {
        return nowKstStatic().toLocalDate();
    }

    public boolean isPastKstTime(LocalDateTime targetKstDateTime) {
        return nowKst().isAfter(targetKstDateTime);
    }
}
