package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.challenge.enums.DailyChallengeStatus;
import com.example.onuldo.domain.challenge.enums.VerificationReviewStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class DailyChallengeStatusResolver {

    public DailyChallengeStatus resolve(
            Participation participation,
            Challenge challenge,
            Verification latestVerification,
            LocalDate today,
            LocalTime currentTime
    ) {
        if (participation == null || isOutsideParticipationDates(participation, today)) {
            return DailyChallengeStatus.UNAVAILABLE;
        }

        if (latestVerification == null) {
            return resolveUnverifiedDailyStatus(challenge, currentTime);
        }

        return toDailyChallengeStatus(latestVerification);
    }

    private boolean isOutsideParticipationDates(Participation participation, LocalDate today) {
        LocalDate startDate = participation.getStartDate();
        LocalDate endDate = participation.getEndDate();
        return (startDate != null && today.isBefore(startDate))
                || (endDate != null && today.isAfter(endDate));
    }

    private DailyChallengeStatus toDailyChallengeStatus(Verification verification) {
        VerificationReviewStatus review = verification.getReview();
        return switch (review) {
            case PASS -> DailyChallengeStatus.SUCCESS;
            case AUTO_FAIL -> DailyChallengeStatus.FAIL;
            case MANUAL_REVIEW, PENDING -> DailyChallengeStatus.REVIEW_PENDING;
        };
    }

    private DailyChallengeStatus resolveUnverifiedDailyStatus(Challenge challenge, LocalTime currentTime) {
        LocalTime timeStart = challenge.getTimeStart();
        LocalTime timeEnd = challenge.getTimeEnd();

        if (timeStart != null && currentTime.isBefore(timeStart)) {
            return DailyChallengeStatus.UNAVAILABLE;
        }

        if (timeEnd != null && currentTime.isAfter(timeEnd)) {
            return DailyChallengeStatus.FAIL;
        }

        return DailyChallengeStatus.WAITING;
    }
}
