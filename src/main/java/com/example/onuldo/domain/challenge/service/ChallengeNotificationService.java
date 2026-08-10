package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationType;
import com.example.onuldo.domain.challenge.enums.VerificationReviewStatus;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import com.example.onuldo.domain.user.enums.NotificationType;
import com.example.onuldo.domain.user.service.NotificationDispatchCommand;
import com.example.onuldo.domain.user.service.NotificationDispatchService;
import com.example.onuldo.global.common.time.TimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeNotificationService {

    private static final LocalTime DEFAULT_VERIFICATION_START = LocalTime.MIDNIGHT;
    private static final LocalTime DEFAULT_VERIFICATION_END = LocalTime.of(23, 0);
    private static final LocalTime DEFAULT_REMINDER_MORNING = LocalTime.of(10, 0);
    private static final LocalTime DEFAULT_REMINDER_EVENING = LocalTime.of(20, 0);
    private static final LocalTime DEFAULT_REMINDER_WARNING = LocalTime.of(22, 0);
    private static final LocalTime END_REMINDER_TIME = LocalTime.of(20, 0);
    private static final int NOTIFICATION_CONTENT_MAX_LENGTH = 255;
    private static final DateTimeFormatter TIME_KEY_FORMATTER = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter TIME_TEXT_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final NotificationDispatchService notificationDispatchService;
    private final ApplicationEventPublisher eventPublisher;
    private final TimeService timeService;

    // 챌린지 참여 확정 시 시작 알림과 종료 리마인더를 예약하는 메서드
    @Transactional
    public void scheduleChallengeLifecycleNotifications(Participation participation) {
        scheduleChallengeStartNotification(participation);
        scheduleChallengeEndReminder(participation, 5);
        scheduleChallengeEndReminder(participation, 3);
        scheduleChallengeEndReminder(participation, 1);
    }

    // 현재 시각 기준으로 인증 마감 리마인더 대상자를 찾아 푸시를 예약하는 메서드
    @Transactional
    public void sendDueVerificationDeadlineReminders() {
        LocalDateTime now = timeService.nowKst();
        LocalDate today = now.toLocalDate();
        LocalTime currentMinute = now.toLocalTime().withSecond(0).withNano(0);

        List<Participation> participations = participationRepository.findAllActiveOnDate(
                ParticipationStatus.ONGOING,
                today
        );

        for (Participation participation : participations) {
            if (verificationRepository.existsByParticipation_IdAndVerificationDate(participation.getId(), today)) {
                continue;
            }

            ReminderSlot reminderSlot = resolveReminderSlot(participation.getChallenge(), currentMinute);
            if (reminderSlot == null) {
                continue;
            }

            enqueuePush(
                    NotificationDispatchCommand.builder()
                            .userId(participation.getUser().getId())
                            .type(NotificationType.VERIFICATION_DEADLINE)
                            .title(reminderSlot.title())
                            .content(reminderSlot.content())
                            .scheduledAt(now)
                            .refType("DEADLINE:" + today + ":" + currentMinute.format(TIME_KEY_FORMATTER))
                            .refId(participation.getId())
                            .build()
            );
        }
    }

    // 파티원 인증 성공 알림을 인증 당사자를 제외한 파티원들에게 보내는 메서드
    @Transactional
    public void notifyPartyMemberVerified(Verification verification) {
        Participation participation = verification.getParticipation();
        if (verification.getReview() != VerificationReviewStatus.PASS
                || participation.getParticipationType() != ParticipationType.PARTY
                || participation.getParty() == null) {
            return;
        }

        List<Participation> partyParticipations = participationRepository.findAllByPartyIdAndStatusWithUserAndChallenge(
                participation.getParty().getId(),
                ParticipationStatus.ONGOING
        );

        String verifierNickname = participation.getUser().getNickname();
        for (Participation target : partyParticipations) {
            if (target.getUser().getId().equals(participation.getUser().getId())) {
                continue;
            }

            enqueuePush(
                    NotificationDispatchCommand.builder()
                            .userId(target.getUser().getId())
                            .type(NotificationType.PARTY_MEMBER_VERIFIED)
                            .title(verifierNickname + "님이 인증을 완료했어요 📸")
                            .content(participation.getChallenge().getName() + " 파티 피드에서 확인해보세요")
                            .scheduledAt(timeService.nowKst())
                            .refType("PARTY_VERIFIED:" + verification.getId())
                            .refId(participation.getParty().getId())
                            .build()
            );
        }
    }

    // 직접검토 승인 결과 알림을 보내고 파티 인증 완료 알림도 함께 처리하는 메서드
    @Transactional
    public void notifyManualReviewPassed(Verification verification) {
        Participation participation = verification.getParticipation();
        enqueuePush(
                NotificationDispatchCommand.builder()
                        .userId(participation.getUser().getId())
                        .type(NotificationType.VERIFICATION_APPROVED)
                        .title("인증이 승인됐어요 ✅")
                        .content(participation.getChallenge().getName() + " 인증이 검토를 통과했어요. 오늘도 성공!")
                        .scheduledAt(timeService.nowKst())
                        .refType("MANUAL_PASS:" + verification.getId())
                        .refId(participation.getId())
                        .build()
        );
        notifyPartyMemberVerified(verification);
    }

    // 직접검토 기각 결과 알림을 기각 사유와 함께 보내는 메서드
    @Transactional
    public void notifyManualReviewRejected(Verification verification, String rejectReason) {
        Participation participation = verification.getParticipation();
        String reason = rejectReason == null || rejectReason.isBlank()
                ? "기각 사유를 확인해 주세요."
                : rejectReason;

        enqueuePush(
                NotificationDispatchCommand.builder()
                        .userId(participation.getUser().getId())
                        .type(NotificationType.VERIFICATION_REJECTED)
                        .title("인증이 승인되지 않았어요")
                        .content(truncateContent(participation.getChallenge().getName() + " 오늘 인증이 실패 처리됐어요. 사유: " + reason))
                        .scheduledAt(timeService.nowKst())
                        .refType("MANUAL_REJECT:" + verification.getId())
                        .refId(participation.getId())
                        .build()
        );
    }

    // 솔로/파티 정산 완료 알림을 지급액과 함께 보내는 메서드
    @Transactional
    public void notifySettlementCompleted(Participation participation, int refundAmount) {
        boolean party = participation.getParticipationType() == ParticipationType.PARTY;
        NotificationType type = party
                ? NotificationType.PARTY_SETTLEMENT_COMPLETE
                : NotificationType.REFUND_COMPLETE;
        String title = party ? "파티 정산이 완료됐어요 💰" : "포인트가 지급됐어요 💰";
        String content = party
                ? String.format("%s 정산 결과 %,dP가 들어왔어요", participation.getChallenge().getName(), refundAmount)
                : String.format("%s 정산 완료! %,dP가 들어왔어요", participation.getChallenge().getName(), refundAmount);

        enqueuePush(
                NotificationDispatchCommand.builder()
                        .userId(participation.getUser().getId())
                        .type(type)
                        .title(title)
                        .content(content)
                        .scheduledAt(timeService.nowKst())
                        .refType("SETTLEMENT:" + participation.getId())
                        .refId(participation.getId())
                        .build()
        );
    }

    // 챌린지 시작일의 첫 인증 가능 시각에 시작 알림을 예약하는 메서드
    private void scheduleChallengeStartNotification(Participation participation) {
        Challenge challenge = participation.getChallenge();
        LocalDateTime scheduledAt = participation.getStartDate().atTime(resolveVerificationStart(challenge));
        LocalDateTime now = timeService.nowKst();
        if (scheduledAt.isBefore(now)) {
            scheduledAt = now;
        }

        enqueuePush(
                NotificationDispatchCommand.builder()
                        .userId(participation.getUser().getId())
                        .type(NotificationType.CHALLENGE_START)
                        .title(challenge.getName() + ", 오늘부터 시작!")
                        .content("1일차예요. 첫 인증으로 기분 좋게 출발해요")
                        .scheduledAt(scheduledAt)
                        .refType("CHALLENGE_START")
                        .refId(participation.getId())
                        .build()
        );
    }

    // 챌린지 종료 N일 전 20:00 리마인더를 예약하는 메서드
    private void scheduleChallengeEndReminder(Participation participation, int daysBefore) {
        LocalDateTime scheduledAt = participation.getEndDate()
                .minusDays(daysBefore)
                .atTime(END_REMINDER_TIME);
        if (scheduledAt.isBefore(timeService.nowKst())) {
            return;
        }

        enqueuePush(
                NotificationDispatchCommand.builder()
                        .userId(participation.getUser().getId())
                        .type(NotificationType.CHALLENGE_END_REMINDER)
                        .title(participation.getChallenge().getName() + " 종료까지 " + daysBefore + "일")
                        .content("완주가 눈앞이에요. 마지막까지 함께해요!")
                        .scheduledAt(scheduledAt)
                        .refType("END_REMINDER:D" + daysBefore)
                        .refId(participation.getId())
                        .build()
        );
    }

    // 현재 시각에 발송해야 할 인증 마감 리마인더 문구를 결정하는 메서드
    private ReminderSlot resolveReminderSlot(Challenge challenge, LocalTime currentMinute) {
        String challengeName = challenge.getName();
        if (hasDesignatedVerificationTime(challenge)) {
            LocalTime start = resolveVerificationStart(challenge);
            if (start.equals(currentMinute)) {
                return new ReminderSlot(
                        "지금 인증할 시간이에요 ⏰",
                        challengeName + " 인증이 시작됐어요. " + resolveVerificationEnd(challenge).format(TIME_TEXT_FORMATTER) + "까지 완료해주세요!"
                );
            }
            return null;
        }

        if (DEFAULT_REMINDER_MORNING.equals(currentMinute) || DEFAULT_REMINDER_EVENING.equals(currentMinute)) {
            return new ReminderSlot(
                    "오늘 인증, 잊지 않으셨죠?",
                    challengeName + " 인증은 23시까지! 지금 완료해보세요"
            );
        }

        if (DEFAULT_REMINDER_WARNING.equals(currentMinute)) {
            return new ReminderSlot(
                    "마감 1시간 전이에요 ⚠️",
                    "23시까지 인증하지 않으면 오늘 " + challengeName + "는 실패 처리돼요"
            );
        }

        return null;
    }

    // 챌린지가 별도 인증 가능 시간대를 가진 챌린지인지 확인하는 메서드
    private boolean hasDesignatedVerificationTime(Challenge challenge) {
        if (challenge.getTimeStart() == null && challenge.getTimeEnd() == null) {
            return false;
        }

        return !DEFAULT_VERIFICATION_START.equals(resolveVerificationStart(challenge))
                || !DEFAULT_VERIFICATION_END.equals(resolveVerificationEnd(challenge));
    }

    // 챌린지 인증 시작 시각을 반환하고 없으면 기본 시작 시각을 반환하는 메서드
    private LocalTime resolveVerificationStart(Challenge challenge) {
        return challenge.getTimeStart() == null ? DEFAULT_VERIFICATION_START : challenge.getTimeStart();
    }

    // 챌린지 인증 종료 시각을 반환하고 없으면 기본 종료 시각을 반환하는 메서드
    private LocalTime resolveVerificationEnd(Challenge challenge) {
        return challenge.getTimeEnd() == null ? DEFAULT_VERIFICATION_END : challenge.getTimeEnd();
    }

    // 알림함 content 길이 제한에 맞춰 문구를 자르는 메서드
    private String truncateContent(String content) {
        if (content.length() <= NOTIFICATION_CONTENT_MAX_LENGTH) {
            return content;
        }

        return content.substring(0, NOTIFICATION_CONTENT_MAX_LENGTH);
    }

    // NotificationDispatchService에 푸시 발송 예약을 위임하는 메서드
    private void enqueuePush(NotificationDispatchCommand command) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            eventPublisher.publishEvent(new ChallengeNotificationEnqueueEvent(command));
            return;
        }

        notificationDispatchService.enqueue(command);
    }

    private record ReminderSlot(String title, String content) {
    }
}
