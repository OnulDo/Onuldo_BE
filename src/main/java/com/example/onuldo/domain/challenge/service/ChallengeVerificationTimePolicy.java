package com.example.onuldo.domain.challenge.service;

import java.time.LocalTime;

public final class ChallengeVerificationTimePolicy {

    private ChallengeVerificationTimePolicy() {
    }

    public static boolean isWithinVerificationTime(LocalTime timeStart, LocalTime timeEnd, LocalTime currentTime) {
        if (timeStart == null && timeEnd == null) {
            return true;
        }

        if (timeStart == null) {
            return !currentTime.isAfter(timeEnd);
        }

        if (timeEnd == null) {
            return !currentTime.isBefore(timeStart);
        }

        if (!timeStart.isAfter(timeEnd)) {
            return !currentTime.isBefore(timeStart) && !currentTime.isAfter(timeEnd);
        }

        return !currentTime.isBefore(timeStart) || !currentTime.isAfter(timeEnd);
    }

    public static boolean isBeforeVerificationTime(LocalTime timeStart, LocalTime timeEnd, LocalTime currentTime) {
        if (timeStart == null || isWithinVerificationTime(timeStart, timeEnd, currentTime)) {
            return false;
        }

        if (timeEnd != null && timeStart.isAfter(timeEnd)) {
            return currentTime.isAfter(timeEnd) && currentTime.isBefore(timeStart);
        }

        return currentTime.isBefore(timeStart);
    }
}
